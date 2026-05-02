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

/*
* Temp0 - Temp7 = 5-12 -> 300-307 LCL = 1 -> 308, ARG = 2 -> 400, THIS = 3 -> 3000, THAT = 4 -> 3010
* RAM 13 - 15 is free to use for anything
* static variables = 16 - 255
* stack = 256 - 2047
* heap = 2048 - 16383
* memory mapped io = 16384 - 24575
* pointer 0 is THIS (RAM address 3)
* pointer 1 is THAT (RAM address 4)
* NOTE: the pointers are used to change the  memory locations of this and that
*       Ex: pop this 5 -> means that we are storing the top value from the stack into RAM[this+5]
*       Ex: pop pointer 0 -> means that we are storing the top value from the stack into THIS i.e. RAM[3]
* */

public class CodeWriter {
    private Parser parser;
    private int id = 0;
    private String filename;
    private StringBuilder assembly = new StringBuilder();
    private TranslatorUtil transUtil= new TranslatorUtil();
    private final Map<String, Integer> segments = new HashMap<>(Map.ofEntries(
            Map.entry("local", 1),
            Map.entry("argument", 2)
    ));
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

    private final List<String> translatedAssembly = new ArrayList<>();

    private void writePushPop(CType ct) {
        String address = parser.argOne();
        int offset = parser.hasArgTwo() ? parser.argTwo() : -1;

        if(ct.equals(CType.C_PUSH)){
            if(address.equalsIgnoreCase("constant")){
                int constant = parser.argTwo();
                assembly.append(transUtil.push(constant, 0, false)).append(transUtil.incrementSP());
            }
            else if(address.equalsIgnoreCase("temp")){
                assembly.append(transUtil.push(tempLocations.get(offset), 0, false)).append(transUtil.incrementSP());
            }
            else if(address.equalsIgnoreCase("local") || address.equalsIgnoreCase("argument")){
                assembly.append(transUtil.push((address.equalsIgnoreCase("local") ? "LCL" : "ARG"), offset, true)).append(transUtil.incrementSP());
            }
            else if(address.equalsIgnoreCase("static")){
                assembly.append(transUtil.push(filename+"."+offset, 0, false)).append(transUtil.incrementSP());
            }
            else if(address.equalsIgnoreCase("pointer")){
                assembly.append(transUtil.push((offset == 0 ? "THIS" : "THAT"), 0, false)).append(transUtil.incrementSP());
            }
            else if(address.equalsIgnoreCase("THIS") || address.equalsIgnoreCase("THAT")){
                assembly.append(transUtil.push(address.toUpperCase(), offset, true)).append(transUtil.incrementSP());
            }
        }
        else {
            if(address.equalsIgnoreCase("static")){
                assembly.append(transUtil.pop()).append(transUtil.writeDToM(filename+"."+offset));
            }
            else if(address.equalsIgnoreCase("temp")){
                assembly.append(transUtil.pop()).append(transUtil.writeDToM((tempLocations.get(offset))));
            }
            else if(address.equalsIgnoreCase("pointer")){
                assembly.append(transUtil.pop()).append(transUtil.writeDToM((offset == 0 ? "THIS" : "THAT")));
            }
            else if(address.equalsIgnoreCase("THIS") || address.equalsIgnoreCase("THAT")){
                assembly.append(transUtil.saveOffsetAddrToTemp(address.toUpperCase(), offset, "R13"))
                        .append(transUtil.pop())
                        .append(transUtil.writeDToMViaPointer("R13"));
            }
            else if(address.equalsIgnoreCase("local") || address.equalsIgnoreCase("argument")){
                assembly.append(transUtil.saveOffsetAddrToTemp((address.equalsIgnoreCase("local") ? "LCL" : "ARG"), offset, "R13"))
                        .append(transUtil.pop())
                        .append(transUtil.writeDToMViaPointer("R13"));
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
            assembly.append(transUtil.binaryOp("sub"))
                    .append(transUtil.pop())
                    .append("@true_").append(id).append(" ")
                    .append(booleanMap.get(op)).append(" ")
                    .append("@false_").append(id).append(" ")
                    .append("0;JMP").append(" ")
                    .append(transUtil.writeTrueFlag(id))
                    .append("@continue_").append(id).append(" ")
                    .append("0;JMP").append(" ")
                    .append(transUtil.writeFalseFlag(id))
                    .append("(continue_").append(id).append(") ");
            id++;
        }
        else if(op.equals("not") || op.equals("neg")) {
            assembly.append(transUtil.unaryOp(op));
        }
        else if(op.equals("add") || op.equals("sub") || op.equals("and") || op.equals("or")){
            assembly.append(transUtil.binaryOp(op));
        }
    }

    public CodeWriter(String path) throws IOException {
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

            translatedAssembly.addAll(List.of(assembly.toString().stripTrailing().split(" ")));
            Files.write(out, translatedAssembly, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        catch(RuntimeException | IOException e) {
            System.out.printf("\u001B[31m%s\u001B[0m", e);
        }
    }
}