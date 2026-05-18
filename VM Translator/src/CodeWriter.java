import util.CType;
import util.TranslatorUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class CodeWriter {
    private Parser parser;
    private String filename;
    private String currFunction;
    private String modifier;
    private Path out;
    private boolean enforceSys;
    private final List<Path> files = new ArrayList<>();
    private final TranslatorUtil transUtil= new TranslatorUtil();
    private final Set<String> hasOffsetAddress = new HashSet<>(Set.of("local", "argument", "this", "that"));
    private final Map<String, String> booleanMap = Map.of(
            "eq", "D;JEQ",
            "lt", "D;JLT",
            "gt", "D;JGT"
    );
    private final Map<String, String> tempLocations = new HashMap<>(Map.of(
            "0", "R5",
            "1", "R6",
            "2", "R7",
            "3", "R8",
            "4", "R9",
            "5", "R10",
            "6", "R11",
            "7", "R12"
    ));
    private final Map<String, Supplier<String>> addressMap = new HashMap<>(Map.ofEntries(
            Map.entry("temp", () -> tempLocations.get(modifier)),
            Map.entry("local", () -> "LCL"),
            Map.entry("argument", () -> "ARG"),
            Map.entry("static", () -> filename+"."+modifier),
            Map.entry("pointer", () -> (modifier.equalsIgnoreCase("0") ? "THIS" : "THAT")),
            Map.entry("this", () -> "THIS"),
            Map.entry("that", () -> "THAT")
    ));

    private void writePushPop(CType ct) {
        modifier = parser.argTwo();
        String address = parser.argOne();
        String offset = hasOffsetAddress.contains(address) ? modifier : "";

        if(ct.equals(CType.C_PUSH)){
            transUtil.push(addressMap.getOrDefault(address, () -> modifier).get(), offset);
        }
        else {
            transUtil.pop(addressMap.get(address).get(), offset);
        }
    }

    private void writeArithmetic(String op) {
        if(op.equals("eq") || op.equals("gt") || op.equals("lt")){
            transUtil.binaryOp("sub");
            transUtil.pop();
            transUtil.writeIsTrueCondition(booleanMap.get(op));
            transUtil.writeIsFalseCondition();
            transUtil.writeIsTrueHandler();
            transUtil.writeIsContinueCondition();
            transUtil.writeIsFalseHandler();
            transUtil.writeIsContinueHandler();
            transUtil.incrementSymbolId();
        }
        else if(op.equals("not") || op.equals("neg")) {
            transUtil.unaryOp(op);
        }
        else if(op.equals("add") || op.equals("sub") || op.equals("and") || op.equals("or")){
           transUtil.binaryOp(op);
        }
    }

    private void writeLabel(String label) {
        transUtil.writeLabel(currFunction, label);
    }

    private void writeGoto(String label) {
        transUtil.writeGoto(currFunction, label);
    }

    private void writeIf(String label) {
       transUtil.writeIf(currFunction, label);
    }

    private void writeCall(String functionName, int numArgs) {
        StringBuilder returnAddr = transUtil.generateReturnAddressName(functionName);

        transUtil.pushReturnAddress(returnAddr.toString());
        transUtil.pushCallerPointers();
        transUtil.repositionCalleeArg(numArgs);
        transUtil.repositionCalleeLcl();
        transUtil.writeJumpToFunction(functionName);
        transUtil.writeLabel(returnAddr.toString());
    }

    private void writeReturn() {
        StringBuilder returnAddr = transUtil.generateReturnAddressName(currFunction);

        transUtil.setFrameVar();
        transUtil.setReturnAddrVar();
        transUtil.setReturnValue();
        transUtil.restoreCallerSP();
        transUtil.restoreCallerPointers();
        transUtil.writeGoto(returnAddr.toString());
    }

    private void writeFunction(String functionName, int numLocals) {
        this.currFunction = functionName;
        transUtil.writeLabel(functionName);
        transUtil.initializeLocals(numLocals);
    }

    private boolean hasSysFile() {
        return getFilename(files.get(0)).equalsIgnoreCase("sys");
    }

    private String getFilename(Path p) {
        String filename = p.getFileName().toString();
        return filename.substring(0, filename.lastIndexOf("."));
    }

    private String getFileExtension(Path p) {
        String filename = p.getFileName().toString();
        return filename.substring(filename.lastIndexOf(".")+1);
    }

    private void bootstrap(){
        transUtil.initializeStackPointer();
        transUtil.writeJumpToFunction("Sys.init");
    }

    private void initialize(String path) throws IOException {
        Path p = Paths.get(path);
        if(Files.isDirectory(p)) {
            Path file = Path.of(p.getFileName() + ".asm");
            out = p.resolve(file);
            try (Stream<Path> paths = Files.list(p)) {
                paths.forEach(pat -> {
                    if(getFileExtension(pat).equals("vm")){
                        if (getFilename(pat).equalsIgnoreCase("sys")){
                            files.add(0, pat);
                        }
                        else {
                            files.add(pat);
                        }
                    }
                });
            }
            if (enforceSys && !hasSysFile()){
                throw new RuntimeException("Sys.vm file was not provided\nEither include a Sys.vm file, or disable enforcement by adding \"false\" as an argument to the jar execution command");
            }
        }
        else{
            String filenameWE = p.getFileName().toString();
            String filename = filenameWE.substring(0, filenameWE.indexOf("."));

            out = p.getParent().resolve(filename+".asm");

            if(filenameWE.substring(filenameWE.lastIndexOf(".")+1).equals("vm")) files.add(p);
        }
    }

    private void translate(){
        for(var f : files) {
            this.filename = getFilename(f);
            this.parser = new Parser(f, filename);
            while(parser.hasMoreCommands()) {
                parser.advance();
                CType ct = parser.commandType();
                if(ct.equals(CType.C_PUSH) || ct.equals(CType.C_POP)) writePushPop(ct);
                else if(ct.equals(CType.C_ARITHMETIC)) writeArithmetic(parser.argOne());
                else if(ct.equals(CType.C_LABEL)) writeLabel(parser.argOne());
                else if(ct.equals(CType.C_GOTO)) writeGoto(parser.argOne());
                else if(ct.equals(CType.C_IF)) writeIf(parser.argOne());
                else if(ct.equals(CType.C_FUNCTION)) writeFunction(parser.argOne(), Integer.parseInt(parser.argTwo()));
                else if(ct.equals(CType.C_RETURN)) writeReturn();
                else if(ct.equals(CType.C_CALL)) writeCall(parser.argOne(), Integer.parseInt(parser.argTwo()));
                else throw new RuntimeException("Command type \""+ct+"\" not recognized");

            }
        }
    }

    private void writeOutput() throws IOException {
        Files.write(out, transUtil.getInstructions(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public CodeWriter(String path, boolean enforceSys) {
        this.enforceSys = enforceSys;
        try{
            initialize(path);
            bootstrap();
            translate();
            writeOutput();
        }
        catch(RuntimeException | IOException e) {
            System.out.printf("\u001B[31m%s\u001B[0m", e);
        }
    }
}