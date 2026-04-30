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

//        if(!address.equals("constant") && (!address.equalsIgnoreCase("static") && !pointers.containsKey(address) && (!segments.containsKey(address)))) {
//            System.out.printf("\u001B[31mInvalid memory segment name: %s \u001B[0m", address);
//            throw new RuntimeException();
//        }

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
            else if(address.equalsIgnoreCase("temp")){
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
            else if(address.equalsIgnoreCase("local") || address.equalsIgnoreCase("argument")){
                // push the value that the pointer is pointing to onto the stack
                translatedAssembly.addAll(List.of(
                        "@"+(address.equalsIgnoreCase("local") ? "LCL" : "ARG"),
                        "A=M", // contains address of the value the pointer is pointing to
                        "D=A", // D contains the value of the pointer i.e. M[THIS/THAT]
                        "@"+offset,
                        "A=D+A",
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
            else if(address.equalsIgnoreCase("static")){
                translatedAssembly.addAll(List.of(
                        // get static value
                        "@"+fileName+"."+offset,
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
            else if(address.equalsIgnoreCase("pointer")){
                // update the base address of THIS/THAT pointers
                translatedAssembly.addAll(List.of(
                        "@"+(offset == 0 ? "THIS" : "THAT"),
                        "D=M", // D contains the value of the pointer
                        // push value in D onto stack (M[SP])
                        "@SP",
                        "A=M",
                        "M=D",
                        // increment stack pointer
                        "@SP",
                        "M=M+1"
                ));
            }
            else if(address.equalsIgnoreCase("THIS") || address.equalsIgnoreCase("THAT")){
                // push the value that the pointer is pointing to onto the stack
                translatedAssembly.addAll(List.of(
                        "@"+address.toUpperCase(),
                        "A=M", // contains address of the value the pointer is pointing to
                        "D=A", // D contains the value of the pointer i.e. M[THIS/THAT]
                        "@"+offset,
                        "A=D+A",
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

            if(address.equalsIgnoreCase("static")){
                translatedAssembly.addAll(List.of(
                        // decrement stack pointer and grab top value from stack
                        "@SP",
                        "M=M-1",
                        "A=M",
                        "D=M",
                        // store value in memory location
                        "@"+fileName+"."+offset,
                        "M=D"
                ));
            }
            else if(address.equalsIgnoreCase("temp")){
                translatedAssembly.addAll(List.of(
                        // decrement stack pointer and grab top value from stack
                        "@SP",
                        "M=M-1",
                        "A=M",
                        "D=M",
                        // store value in memory location
                        "@"+(segments.get(address)+offset),
                        "M=D"
                ));
            }
            else if(address.equalsIgnoreCase("pointer")){
                // change the value stored in THIS/THAT >> update the pointer address with the top value on the stack
                translatedAssembly.addAll(List.of(
                        // decrement stack pointer and grab top value from stack
                        "@SP",
                        "M=M-1",
                        "A=M",
                        "D=M",
                        // store value in memory location
                        "@"+(offset == 0 ? "THIS" : "THAT"),
                        "M=D"
                ));
            }
            else if(address.equalsIgnoreCase("THIS") || address.equalsIgnoreCase("THAT")){
                // change the value that THIS/THAT are pointing to i.e. M[THIS/THAT]
                translatedAssembly.addAll(List.of(
                        "@"+address.toUpperCase(),
                        "D=M",
                        "@"+offset,
                        "D=D+A",
                        "@"+(segments.get("temp")),
                        "M=D", // store address in temp var

                        "@SP",
                        "M=M-1",
                        "A=M", // A equals stack pointer
                        "D=M", // D equals top of stack value
                        "@"+(segments.get("temp")),
                        "A=M", // A = M[5] = address to store value
                        "M=D" // M[address] = D
                ));
            }
            else if(address.equalsIgnoreCase("local") || address.equalsIgnoreCase("argument")){
                translatedAssembly.addAll(List.of(
                        "@"+(address.equalsIgnoreCase("local") ? "LCL" : "ARG"),
                        "D=M",
                        "@"+offset,
                        "D=D+A", // address + offset i.e. pop argument 5
                        "@"+(segments.get("temp")),
                        "M=D", // store address in temp var

                        "@SP",
                        "M=M-1",
                        "A=M", // A equals stack pointer
                        "D=M", // D equals top of stack value
                        "@"+(segments.get("temp")),
                        "A=M", // A = M[5] = address to store value
                        "M=D" // M[address] = D
                ));
            }
            else{
                translatedAssembly.addAll(List.of(
                        // decrement stack pointer and grab top value from stack
                        "@SP",
                        "M=M-1",
                        "A=M",
                        "D=M",
                        // store value in memory location
                        "@"+ pointers.get(address),
                        "M=D"
                ));
            }
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
                    "@SP",
                    "M=M-1",
                    "A=M",
                    "D=M", // y

                    "@SP",
                    "M=M-1",
                    "A=M", // x

                    // do x - y
                    "M=M-D", // result also on top of stack >> no push needed

                    // jump to true label if value in D is true i.e D == -1
                    "D=M", // result of x - y

                    // evaluate result
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
            id++;
        }
        else if(op.equals("not")) {
            translatedAssembly.addAll(List.of(
                    // pop and store temp 0
                    "@SP",
                    "M=M-1",
                    "A=M",
                    "D=M",
                    "@"+(segments.get("temp")),
                    "M=D",

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
                    "A=M",
                    "D=M",
                    "@"+(segments.get("temp")),
                    "M=D",

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
                    "A=M",
                    "D=M", // y

                    // pop and store temp 1
                    "@SP",
                    "M=M-1",
                    "A=M", // x

                    // do x +/- y
                    op.equalsIgnoreCase("add") ? "M=M+D" : "M=M-D", // result also on top of stack >> no push needed

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
                    "A=M",
                    "D=M",
                    "@"+(segments.get("temp")),
                    "M=D",

                    // pop and store temp 1
                    "@SP",
                    "M=M-1",
                    "A=M",
                    "D=M",
                    "@"+(segments.get("temp") + 1),
                    "M=D",

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
//            translatedAssembly.forEach(System.out::println);
            Files.write(out, translatedAssembly, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        catch(RuntimeException | IOException e) {
            System.out.printf("\u001B[31m%s\u001B[0m", e);
        }
    }
}