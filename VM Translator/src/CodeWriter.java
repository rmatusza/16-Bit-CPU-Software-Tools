import util.CType;
import service.TranslatorService;
import util.CodeWriterUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Stream;

public class CodeWriter {
    private Parser parser;
    private String filename;
    private String currFunction;
    private Path out;
    private final boolean enforceSys;
    private final List<Path> files = new ArrayList<>();
    private final TranslatorService service = new TranslatorService();
    private final CodeWriterUtil util = new CodeWriterUtil();

    private void writePushPop(CType ct) {
        String modifier = parser.argTwo();
        String address = parser.argOne();
        String offset = util.hasOffsetAddress.contains(address) ? modifier : "";
        String constant = address.equalsIgnoreCase("constant") ? modifier : "";
        if(ct.equals(CType.C_PUSH)){
            service.push(util.getAddressOrDefault(address, modifier, filename).get(), offset, constant);
        }
        else {
            service.popToAddress(util.getAddress(address, modifier, filename).get(), offset);
        }
    }

    private void writeArithmetic(String op) {
        if(op.equals("eq") || op.equals("gt") || op.equals("lt")){
            service.binaryOp("sub");
            service.popToD();
            service.writeIsTrueCondition(util.conditionalBranchMap.get(op));
            service.writeIsFalseCondition();
            service.writeIsTrueHandler();
            service.writeIsContinueCondition();
            service.writeIsFalseHandler();
            service.writeIsContinueHandler();
            service.incrementSymbolId();
        }
        else if(op.equals("not") || op.equals("neg")) {
            service.unaryOp(op);
        }
        else if(op.equals("add") || op.equals("sub") || op.equals("and") || op.equals("or")){
           service.binaryOp(op);
        }
    }

    private void writeLabel(String label) {
        service.writeLabel(currFunction, label);
    }

    private void writeGoto(String label) {
        service.writeGoto(currFunction, label);
    }

    private void writeIf(String label) {
       service.writeIf(currFunction, label, util.conditionalBranchMap.get("ne"));
    }

    private void writeCall(String functionName, int numArgs) {
        service.pushReturnAddress(functionName);
        service.pushCallerPointers();
        service.repositionCalleeArg(numArgs);
        service.repositionCalleeLcl();
        service.writeGoto(functionName);
        service.writeReturnAddressLabel(functionName);
        service.incrementSymbolId();
    }

    private void writeReturn() {
        service.setFrameVar();
        service.setReturnAddrVar();
        service.setReturnValue();
        service.restoreCallerSP();
        service.restoreCallerPointers();
        service.writeGotoVar("RET");
    }

    private void writeFunction(String functionName, int numLocals) {
        this.currFunction = functionName;
        service.writeLabel(functionName);
        service.initializeLocals(numLocals);
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
        service.initialize();
        writeCall("Sys.init", 0);
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
        Files.write(out, service.getAssembly(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
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