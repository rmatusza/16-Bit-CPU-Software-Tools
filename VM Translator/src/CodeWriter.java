import util.CType;
import util.TranslatorUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class CodeWriter {
    private Parser parser;
    private String filename;
    private String modifier;
    private Path out;
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

    private List<Path> initializeOutputPaths(String path) throws IOException {
        List<Path> files = new ArrayList<>();
        Path p = Paths.get(path);

        if(Files.isDirectory(p)) {
            Path file = Path.of(p.getFileName() + ".asm");
            out = p.resolve(file);
            try (Stream<Path> paths = Files.list(p)) {
                paths.forEach(pat -> {
                    String filename = pat.getFileName().toString();
                    if(filename.substring(filename.lastIndexOf(".")+1).equals("vm")) files.add(pat);
                });
            }
        }
        else{
            String filenameWE = p.getFileName().toString();
            String filename = filenameWE.substring(0, filenameWE.indexOf("."));

            out = p.getParent().resolve(filename+".asm");

            if(filenameWE.substring(filenameWE.lastIndexOf(".")+1).equals("vm")) files.add(p);
        }
        return files;
    }

    private void writeAssembly(List<Path> files){
        for(var f : files) {
            String filenameWE = f.getFileName().toString();
            filename = filenameWE.substring(0, filenameWE.lastIndexOf("."));
            this.parser = new Parser(f, filename);
            while(parser.hasMoreCommands()) {
                parser.advance();
                CType ct = parser.commandType();
                if(ct.equals(CType.C_PUSH) || ct.equals(CType.C_POP)) writePushPop(ct);
                else if(ct.equals(CType.C_ARITHMETIC)) writeArithmetic(parser.argOne());
            }
        }
    }

    private void writeOutput() throws IOException {
        Files.write(out, transUtil.getInstructions(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public CodeWriter(String path) {
        try{
            List<Path> files = initializeOutputPaths(path);
            writeAssembly(files);
            writeOutput();
        }
        catch(RuntimeException | IOException e) {
            System.out.printf("\u001B[31m%s\u001B[0m", e);
        }
    }
}