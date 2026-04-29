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
    private int SP = 256; /* stack pointer */
    private int id = 0;
    private final Map<String, Integer> segments = new HashMap<>(Map.ofEntries(
            Map.entry("temp", 5),
            Map.entry("static", 16) // range is 16 - 255 >> usage: static 10 => segments.get("static") + 10
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

        if(!address.equals("constant") && (!pointers.containsKey(address) || (!segments.containsKey(address)))) {
            System.out.printf("\u001B[31mInvalid memory segment name: %s \u001B[0m", address);
            throw new RuntimeException();
        }

        if(ct.equals(CType.C_PUSH)){
            if(address.equalsIgnoreCase("constant")){
                int constant = parser.argTwo();
                translatedAssembly.addAll(List.of(
                        // store constant in D
                        "@"+constant,
                        "D=A",
                        // push value in D onto stack (M[SP])
                        "@SP",
                        "A=M",
                        "M=D",
                        // increment stack pointer
                        "@SP",
                        "M=M+1"
                ));
            }
            else if(address.equalsIgnoreCase("static") || address.equalsIgnoreCase("temp")){
                // pushing value of static x onto stack
                translatedAssembly.addAll(List.of(
                        // get value from memory >> store in D
                        "@"+(segments.get(address)+offset),
                        "D=M",
                        // push value in D onto stack (M[SP])
                        "@SP",
                        "A=M",
                        "M=D",
                        // increment stack pointer
                        "@SP",
                        "M=M+1"
                ));
            }
            else{
                translatedAssembly.addAll(List.of(
                        // get value from memory >> store in D
                        "@"+pointers.get(address),
                        "D=M",
                        // push value in D onto stack (M[SP])
                        "@SP",
                        "A=M",
                        "M=D",
                        // increment stack pointer
                        "@SP",
                        "M=M+1"
                ));
            }
        }
        else {

            translatedAssembly.addAll(List.of(
                    // decrement stack pointer and grab top value from stack
                    "@SP",
                    "M=M-1",
                    "D=M",
                    // store value in memory location
                    "@"+ (address.equalsIgnoreCase("static") || address.equalsIgnoreCase("temp") ? (segments.get(address)+offset) : pointers.get(address)),
                    "M=D",
                    // set old stack top to 0 to finalize pop
                    "@SP",
                    "A=M",
                    "M=0"
            ));
        }
    }

    private void writeArithmetic(String op) {
        // neg & not are unary so only pop once
        // eq, gt, and lt require jump commands
        Map<String, String> booleanMap = Map.of(
                "eq", "D;JEQ",
                "lt", "D;JLT",
                "gt", "D;JGT"
        );

        if(op.equals("eq") || op.equals("gt") || op.equals("lt")){
            // true if x is greater than y
            // x is @256 and y is at @257

            translatedAssembly.addAll(List.of(
                    // pop and store temp 0
                    "@SP",
                    "M=M-1",
                    "D=M",
                    "@"+(segments.get("temp")),
                    "M=D",
                    "@SP",
                    "A=M",
                    "M=0",

                    // pop and store temp 1
                    "@SP",
                    "M=M-1",
                    "D=M",
                    "@"+(segments.get("temp") + 1),
                    "M=D",
                    "@SP",
                    "A=M",
                    "M=0",

                    // do temp 0 - temp 1
                    "@"+(segments.get("temp")),
                    "D=M",
                    "@"+(segments.get("temp") + 1),
                    "M=D-M",

                    // jump to true label if value in D is true i.e D == -1
                    "D=M",
                    "@true_"+(id),
                    booleanMap.get(op),

                    // jump to false label if false
                    "@false_"+id,
                    "0;JMP",

                    // true case >> push -1 onto stack
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

                    // false case >> push 0 onto stack
                    "(false_"+id+")",
                    "@SP",
                    "A=M",
                    "M=0",
                    "@SP",
                    "M=M+1",

                    // end of computation
                    "(continue_"+id+")"
            ));
        }
        else if(op.equals("not")) {
            translatedAssembly.addAll(List.of(
                    // pop and store temp 0
                    "@SP",
                    "M=M-1",
                    "D=M",
                    "@"+(segments.get("temp")),
                    "M=D",
                    "@SP",
                    "A=M",
                    "M=0",

                    // perform !temp 0
                    "@"+(segments.get("temp")),
                    "D=M",
                    "@SP",
                    "A=M",
                    "M=!D",

                    // increment stack pointer
                    "@SP",
                    "M=M+1"
            ));
        }
        else if(op.equals("neg")) {
            translatedAssembly.addAll(List.of(
                    // pop and store temp 0
                    "@SP",
                    "M=M-1",
                    "D=M",
                    "@"+(segments.get("temp")),
                    "M=D",
                    "@SP",
                    "A=M",
                    "M=0",

                    // perform -temp 0 and push to stack
                    "@"+(segments.get("temp")),
                    "D=M",
                    "@SP",
                    "A=M",
                    "M=-D",

                    // increment stack pointer
                    "@SP",
                    "M=M+1"
            ));
        }
        else if(op.equals("add") || op.equals("sub")){
            translatedAssembly.addAll(List.of(
                    // pop and store temp 0
                    "@SP",
                    "M=M-1",
                    "D=M",
                    "@"+(segments.get("temp")),
                    "M=D",
                    "@SP",
                    "A=M",
                    "M=0",

                    // pop and store temp 1
                    "@SP",
                    "M=M-1",
                    "D=M",
                    "@"+(segments.get("temp") + 1),
                    "M=D",
                    "@SP",
                    "A=M",
                    "M=0",

                    // do temp 0 +/- temp 1
                    "@"+(segments.get("temp")),
                    "D=M",
                    "@"+(segments.get("temp") + 1),
                    op.equalsIgnoreCase("add") ? "M=D+M" : "M=D-M",

                    // push result to stack
                    "D=M",
                    "@SP",
                    "A=M",
                    "M=D",

                    // increment stack pointer
                    "@SP",
                    "M=M+1"
            ));
        }
        else if(op.equals("and") || op.equals("or")){
            translatedAssembly.addAll(List.of(
                    // pop and store temp 0
                    "@SP",
                    "M=M-1",
                    "D=M",
                    "@"+(segments.get("temp")),
                    "M=D",
                    "@SP",
                    "A=M",
                    "M=0",

                    // pop and store temp 1
                    "@SP",
                    "M=M-1",
                    "D=M",
                    "@"+(segments.get("temp") + 1),
                    "M=D",
                    "@SP",
                    "A=M",
                    "M=0",

                    // do temp 0 +/- temp 1
                    "@"+(segments.get("temp")),
                    "D=M",
                    "@"+(segments.get("temp") + 1),
                    op.equalsIgnoreCase("and") ? "M=D&M" : "M=D|M",

                    // push result to stack
                    "D=M",
                    "@SP",
                    "A=M",
                    "M=D",

                    // increment stack pointer
                    "@SP",
                    "M=M+1"
            ));
        }
    }

    public CodeWriter(String path) throws IOException {
        // TODO: 1) replace this constructor with the one from the vm-final branch (includes correct file naming code) >> 2) add writeInit method from vm-final branch
        // TODO: update the code here to be similar to the constructor of the Code class in the Assembler, where all the logic is just a series of method calls and no loops
        try{
            List<Path> files = new ArrayList<>();
            Path p = Paths.get(path);
            Path out;

            if(Files.isDirectory(p)) {
                out = p.resolve(p+".asm");
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
                while(parser.hasMoreCommands()) {
                    parser.advance();
                    CType ct = parser.commandType();
                    if(ct.equals(CType.C_PUSH) || ct.equals(CType.C_POP)) writePushPop(ct);
                    else if(ct.equals(CType.C_ARITHMETIC)) writeArithmetic(parser.argOne());
                }
            }
//            translatedAssembly.forEach(System.out::println);
            Files.write(out, translatedAssembly, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        catch(RuntimeException | IOException e) {
            System.out.printf("\u001B[31m%s\u001B[0m", e);
        }
    }
}