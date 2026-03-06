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
    private final Map<String, Integer> baseAddr = new HashMap<>(Map.of(
            "local", 300,
            "argument", 400,
            "this", 3000,
            "that", 3010,
            "pointer 0", 3,
            "pointer 1", 4,
            "temp", 5,
            "static", 16
    ));
    private final List<String> translatedAssembly = new ArrayList<>();

    private void writePushPop(CType ct, String seg, int idx) {
        // PUSHING
        // push constant 10 // @10 D=A @SP++ M=D
        // push that 5 // @baseAddr.that+5 D=A @SP++ M=D

        // POPPING:
        // NOTE: constants are never popped
        // pop local 0 // @--SP D=M @baseAddr.local+0 M=D

        if(!seg.equals("constant") && !baseAddr.containsKey(seg)) {
            System.out.printf("\u001B[31mInvalid segment: %s \u001B[0m", seg);
            throw new RuntimeException();
        }

        if(ct.equals(CType.C_PUSH)){
            translatedAssembly.addAll(List.of(seg.equals("constant") ? "@"+idx : "@"+(baseAddr.get(seg)+idx), seg.equals("constant") ? "D=A" : "D=M", "@"+(this.SP++), "M=D"));
        }
        else {
            translatedAssembly.addAll(List.of("@"+(--this.SP), "D=M", "@"+(baseAddr.get(seg)+idx), "M=D"));
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
                "@"+(--SP), // x
                "D=M",
                "@"+(--SP), // y
                "M=D-M",
                "D=M",
                "@true",
                booleanMap.get(op),
                "@false",
                "0;JMP",
                "(true)",
                "@"+(SP++),
                "M=-1",
                "@continue",
                "0;JMP",
                "(false)",
                "@"+(SP++),
                "M=0",
                "(continue)"
            ));
        }
        else if(op.equals("not")) {
            translatedAssembly.addAll(List.of(
                "@"+(SP-1),
                "M=!M"
            ));
        }
        else if(op.equals("neg")) {
            translatedAssembly.addAll(List.of(
                "@"+(SP-1),
                "D=M",
                "@"+(baseAddr.get("temp")),
                "M=D",
                "M=D+M",
                "D=M",
                "@"+(SP-1),
                "M=M-D"
            ));
        }
        else if(op.equals("add") || op.equals("sub")){
            translatedAssembly.addAll(List.of(
                "@"+(--SP),
                "D=M",
                "@"+(SP-1),
                op.equals("add") ? "M=D+M" : "M=M-D"

            ));
        }
        else if(op.equals("and") || op.equals("or")){
            translatedAssembly.addAll(List.of(
                "@"+(--SP),
                "D=M",
                "@"+(SP-1),
                op.equals("and") ? "M=D&M" : "M=D|M"
            ));
        }
    }

    public CodeWriter(String path) throws IOException {
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
                    if(ct.equals(CType.C_PUSH) || ct.equals(CType.C_POP)) writePushPop(ct, parser.argOne(), parser.argTwo());
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