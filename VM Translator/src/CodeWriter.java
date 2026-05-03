import util.TranslatorUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class CodeWriter {
    private Parser parser;
    private String filename;
    private final TranslatorUtil transUtil= new TranslatorUtil();
    private final Map<Integer, String> tempLocations = new HashMap<>(Map.of(
            0, "R5",
            1, "R6",
            2, "R7",
            3, "R8",
            4, "R9",
            5, "R10",
            6, "R11",
            7, "R12"
    ));

    private void writePushPop(CType ct) {
        String address = parser.argOne();
        int offset = parser.hasArgTwo() ? parser.argTwo() : -1;

        if(ct.equals(CType.C_PUSH)){
            if(address.equalsIgnoreCase("constant")){
                int constant = parser.argTwo();
                transUtil.push(constant, 0, false);
            }
            else if(address.equalsIgnoreCase("temp")){
                transUtil.push(tempLocations.get(offset), 0, false);
            }
            else if(address.equalsIgnoreCase("local") || address.equalsIgnoreCase("argument")){
                transUtil.push((address.equalsIgnoreCase("local") ? "LCL" : "ARG"), offset, true);
            }
            else if(address.equalsIgnoreCase("static")){
                transUtil.push(filename+"."+offset, 0, false);
            }
            else if(address.equalsIgnoreCase("pointer")){
                transUtil.push((offset == 0 ? "THIS" : "THAT"), 0, false);
            }
            else if(address.equalsIgnoreCase("THIS") || address.equalsIgnoreCase("THAT")){
                transUtil.push(address.toUpperCase(), offset, true);
            }
        }
        else {
            if(address.equalsIgnoreCase("static")){
                transUtil.pop();
                transUtil.writeDToM(filename+"."+offset);
            }
            else if(address.equalsIgnoreCase("temp")){
                transUtil.pop();
                transUtil.writeDToM((tempLocations.get(offset)));
            }
            else if(address.equalsIgnoreCase("pointer")){
                transUtil.pop();
                transUtil.writeDToM((offset == 0 ? "THIS" : "THAT"));
            }
            else if(address.equalsIgnoreCase("THIS") || address.equalsIgnoreCase("THAT")){
                transUtil.saveOffsetAddrToTemp(address.toUpperCase(), offset, "R13");
                transUtil.pop();
                transUtil.writeDToMViaPointer("R13");
            }
            else if(address.equalsIgnoreCase("local") || address.equalsIgnoreCase("argument")){
                transUtil.saveOffsetAddrToTemp((address.equalsIgnoreCase("local") ? "LCL" : "ARG"), offset, "R13");
                transUtil.pop();
                transUtil.writeDToMViaPointer("R13");
            }
        }
    }

    private void writeArithmetic(String op) {
        Map<String, String> booleanMap = Map.of(
                "eq", "D;JEQ",
                "lt", "D;JLT",
                "gt", "D;JGT"
        );

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

    public CodeWriter(String path) {
        try{
            List<Path> files = new ArrayList<>();
            Path p = Paths.get(path);
            Path out;

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

            for(var f : files) {
                this.parser = new Parser(f);
                filename = f.getFileName().toString();
                while(parser.hasMoreCommands()) {
                    parser.advance();
                    CType ct = parser.commandType();
                    if(ct.equals(CType.C_PUSH) || ct.equals(CType.C_POP)) writePushPop(ct);
                    else if(ct.equals(CType.C_ARITHMETIC)) writeArithmetic(parser.argOne());
                }
            }

            Files.write(out, transUtil.getInstructions(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        catch(RuntimeException | IOException e) {
            System.out.printf("\u001B[31m%s\u001B[0m", e);
        }
    }
}