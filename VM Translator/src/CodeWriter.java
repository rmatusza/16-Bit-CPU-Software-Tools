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
    private String fileName;
    private final Map<String, Integer> segments = new HashMap<>(Map.ofEntries(
            Map.entry("temp", 5),
            Map.entry("local", 1),
            Map.entry("argument", 2)
    ));
    private final Map<String, Integer> pointers = new HashMap<>(Map.ofEntries(
            Map.entry("SP", 0),
            Map.entry("LCL", 1),
            Map.entry("ARG", 2),
            Map.entry("THIS", 3),
            Map.entry("THAT", 4),
            Map.entry("R13", 13),
            Map.entry("R14", 14),
            Map.entry("R15", 15)
    ));
    private final List<String> translatedAssembly = new ArrayList<>();

    private void writePushPop(CType ct) {
        String address = parser.argOne();
        int offset = parser.hasArgTwo() ? parser.argTwo() : -1;

        if(ct.equals(CType.C_PUSH)){
            if(address.equalsIgnoreCase("constant")){
                int constant = parser.argTwo();
                translatedAssembly.addAll(List.of(
                        "@"+constant,
                        "D=A",
                        "@SP",
                        "A=M",
                        "M=D",
                        "@SP",
                        "M=M+1"
                ));
            }
            else if(address.equalsIgnoreCase("temp")){
                translatedAssembly.addAll(List.of(
                        "@"+(segments.get(address)+offset),
                        "D=M",
                        "@SP",
                        "A=M",
                        "M=D",
                        "@SP",
                        "M=M+1"
                ));
            }
            else if(address.equalsIgnoreCase("local") || address.equalsIgnoreCase("argument")){
                // push the value that the pointer is pointing to onto the stack
                translatedAssembly.addAll(List.of(
                        "@"+(address.equalsIgnoreCase("local") ? "LCL" : "ARG"),
                        "A=M",
                        "D=A",
                        "@"+offset,
                        "A=D+A",
                        "D=M",
                        "@SP",
                        "A=M",
                        "M=D",
                        "@SP",
                        "M=M+1"
                ));
            }
            else if(address.equalsIgnoreCase("static")){
                translatedAssembly.addAll(List.of(
                        "@"+fileName+"."+offset,
                        "D=M",
                        "@SP",
                        "A=M",
                        "M=D",
                        "@SP",
                        "M=M+1"
                ));
            }
            else if(address.equalsIgnoreCase("pointer")){
                translatedAssembly.addAll(List.of(
                        "@"+(offset == 0 ? "THIS" : "THAT"),
                        "D=M",
                        "@SP",
                        "A=M",
                        "M=D",
                        "@SP",
                        "M=M+1"
                ));
            }
            else if(address.equalsIgnoreCase("THIS") || address.equalsIgnoreCase("THAT")){
                translatedAssembly.addAll(List.of(
                        "@"+address.toUpperCase(),
                        "A=M",
                        "D=A",
                        "@"+offset,
                        "A=D+A",
                        "D=M",
                        "@SP",
                        "A=M",
                        "M=D",
                        "@SP",
                        "M=M+1"
                ));
            }
            else{
                translatedAssembly.addAll(List.of(
                        "@"+pointers.get(address),
                        "D=M",
                        "@SP",
                        "A=M",
                        "M=D",
                        "@SP",
                        "M=M+1"
                ));
            }
        }
        else {

            if(address.equalsIgnoreCase("static")){
                translatedAssembly.addAll(List.of(
                        "@SP",
                        "M=M-1",
                        "A=M",
                        "D=M",
                        "@"+fileName+"."+offset,
                        "M=D"
                ));
            }
            else if(address.equalsIgnoreCase("temp")){
                translatedAssembly.addAll(List.of(
                        "@SP",
                        "M=M-1",
                        "A=M",
                        "D=M",
                        "@"+(segments.get(address)+offset),
                        "M=D"
                ));
            }
            else if(address.equalsIgnoreCase("pointer")){
                translatedAssembly.addAll(List.of(
                        "@SP",
                        "M=M-1",
                        "A=M",
                        "D=M",
                        "@"+(offset == 0 ? "THIS" : "THAT"),
                        "M=D"
                ));
            }
            else if(address.equalsIgnoreCase("THIS") || address.equalsIgnoreCase("THAT")){
                translatedAssembly.addAll(List.of(
                        "@"+address.toUpperCase(),
                        "D=M",
                        "@"+offset,
                        "D=D+A",
                        "@"+(segments.get("temp")),
                        "M=D",

                        "@SP",
                        "M=M-1",
                        "A=M",
                        "D=M",
                        "@"+(segments.get("temp")),
                        "A=M",
                        "M=D"
                ));
            }
            else if(address.equalsIgnoreCase("local") || address.equalsIgnoreCase("argument")){
                translatedAssembly.addAll(List.of(
                        "@"+(address.equalsIgnoreCase("local") ? "LCL" : "ARG"),
                        "D=M",
                        "@"+offset,
                        "D=D+A",
                        "@"+(segments.get("temp")),
                        "M=D",
                        "@SP",
                        "M=M-1",
                        "A=M",
                        "D=M",
                        "@"+(segments.get("temp")),
                        "A=M",
                        "M=D"
                ));
            }
            else{
                translatedAssembly.addAll(List.of(
                        "@SP",
                        "M=M-1",
                        "A=M",
                        "D=M",
                        "@"+ pointers.get(address),
                        "M=D"
                ));
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
            translatedAssembly.addAll(List.of(
                    "@SP",
                    "M=M-1",
                    "A=M",
                    "D=M", // y
                    "@SP",
                    "M=M-1",
                    "A=M", // x
                    "M=M-D",
                    "D=M",
                    "@true_"+(id),
                    booleanMap.get(op),
                    "@false_"+id,
                    "0;JMP",
                    "(true_"+id+")",
                    "@1",
                    "D=A",
                    "@SP",
                    "A=M",
                    "M=-D",
                    "@SP",
                    "M=M+1",
                    "@continue_"+id,
                    "0;JMP",
                    "(false_"+id+")",
                    "@SP",
                    "A=M",
                    "M=0",
                    "@SP",
                    "M=M+1",
                    "(continue_"+id+")"
            ));
            id++;
        }
        else if(op.equals("not")) {
            translatedAssembly.addAll(List.of(
                    "@SP",
                    "M=M-1",
                    "A=M",
                    "D=M",
                    "@"+(segments.get("temp")),
                    "M=D",
                    "@"+(segments.get("temp")),
                    "D=M",
                    "@SP",
                    "A=M",
                    "M=!D",
                    "@SP",
                    "M=M+1"
            ));
        }
        else if(op.equals("neg")) {
            translatedAssembly.addAll(List.of(
                    "@SP",
                    "M=M-1",
                    "A=M",
                    "D=M",
                    "@"+(segments.get("temp")),
                    "M=D",
                    "@"+(segments.get("temp")),
                    "D=M",
                    "@SP",
                    "A=M",
                    "M=-D",
                    "@SP",
                    "M=M+1"
            ));
        }
        else if(op.equals("add") || op.equals("sub")){
            translatedAssembly.addAll(List.of(
                    "@SP",
                    "M=M-1",
                    "A=M",
                    "D=M", // y
                    "@SP",
                    "M=M-1",
                    "A=M", // x
                    op.equalsIgnoreCase("add") ? "M=M+D" : "M=M-D",
                    "@SP",
                    "M=M+1"
            ));
        }
        else if(op.equals("and") || op.equals("or")){
            translatedAssembly.addAll(List.of(
                    "@SP",
                    "M=M-1",
                    "A=M",
                    "D=M",
                    "@"+(segments.get("temp")),
                    "M=D",
                    "@SP",
                    "M=M-1",
                    "A=M",
                    "D=M",
                    "@"+(segments.get("temp") + 1),
                    "M=D",
                    "@"+(segments.get("temp")),
                    "D=M",
                    "@"+(segments.get("temp") + 1),
                    op.equalsIgnoreCase("and") ? "M=D&M" : "M=D|M",
                    "D=M",
                    "@SP",
                    "A=M",
                    "M=D",
                    "@SP",
                    "M=M+1"
            ));
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
                        String fileName = pat.getFileName().toString();
                        if(fileName.substring(fileName.lastIndexOf(".")+1).equals("vm")) files.add(pat);
                    });
                }
            }
            else{
                String fileNameWE = p.getFileName().toString();
                String fileName = fileNameWE.substring(0, fileNameWE.indexOf("."));

                out = p.getParent().resolve(fileName+".asm");

                if(fileNameWE.substring(fileNameWE.lastIndexOf(".")+1).equals("vm")) files.add(p);
            }

            for(var f : files) {
                this.parser = new Parser(f);
                fileName = f.getFileName().toString();
                while(parser.hasMoreCommands()) {
                    parser.advance();
                    CType ct = parser.commandType();
                    if(ct.equals(CType.C_PUSH) || ct.equals(CType.C_POP)) writePushPop(ct);
                    else if(ct.equals(CType.C_ARITHMETIC)) writeArithmetic(parser.argOne());
                }
            }
            Files.write(out, translatedAssembly, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        catch(RuntimeException | IOException e) {
            System.out.printf("\u001B[31m%s\u001B[0m", e);
        }
    }
}