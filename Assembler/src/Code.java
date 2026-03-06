import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class Code {

    // prepare files and paths

    // add labels to symbol table

    // assemble binary

    // write to output file

    public Code(String path, boolean writeDec) {
        final List<String> binary = new ArrayList<>();
        try{
            Path p = Paths.get(path);
            Path binOut;
            Path decOut;

            String fileNameWE = p.getFileName().toString();
            if(!fileNameWE.endsWith(".asm")) throw new RuntimeException("Invalid file type :"+fileNameWE+": must provide a file with a .asm extension");
            String fileName = fileNameWE.substring(0, fileNameWE.indexOf("."));

            binOut = p.getParent().resolve(fileName+"BIN.hack");
            decOut = p.getParent().resolve(fileName+"DEC.hack");

            Parser parser = new Parser(p);
            SymbolTable table = new SymbolTable();

            // Add labels to symbol table
            while(parser.hasMoreCommands()) {
                parser.advance();
                CType ct = parser.commandType();
                if(ct.equals(CType.L_COMMAND)) {
                    String symbol = parser.symbol();
                    if(table.contains(symbol)) throw new RuntimeException("duplicate symbol :" + symbol + ": on line " + parser.getLineNum());
                    table.addEntry(symbol, parser.getRomAddr());
                }
                else {
                    parser.incrRomAddr();
                }
            }

//            table.printTable();
//            System.out.println(table.getAddress("END_EQ"));
            parser.reset();

            // Assemble the binary and write to file
            while(parser.hasMoreCommands()) {
                parser.advance();
                CType ct = parser.commandType();
                String sym = parser.symbol();

                if(ct.equals(CType.A_COMMAND)){
                    if(parser.isConst()) binary.add(String.format("%16s", Integer.toBinaryString(Integer.parseInt(sym))).replace(' ', '0'));
                    else if(table.contains(sym)) binary.add(String.format("%16s", Integer.toBinaryString(table.getAddress(sym))).replace(' ', '0'));
                    else if(parser.isVar()) {
                        int varAddr = parser.getVarAddr();
                        binary.add(String.format("%16s", Integer.toBinaryString(parser.getAndIncrVarAddr())).replace(' ', '0'));
                        table.addEntry(sym, varAddr);
                    }
                    else throw new RuntimeException("Invalid label reference :" + sym + ": instruction " + parser.getLineNum());
                    parser.incrRomAddr();
                }
                else if(ct.equals(CType.C_COMMAND)) {
                    binary.add("111" + parser.comp()  + parser.dest() + parser.jump());
                    parser.incrRomAddr();
                }
            }

//            binary.forEach(System.out::println);
            Files.write(binOut, binary, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            if(writeDec){
                List<String> decimal = new ArrayList<>();
                binary.forEach(b -> {
                    int d = Integer.parseInt(b, 2);
                    decimal.add(String.valueOf(d));
                });
//            decimal.forEach(System.out::println);
                Files.write(decOut, decimal, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
            System.out.printf("\u001B[32mAssembled %s.hack to directory: %s \u001B[0m", fileName, p.getParent());
        }
        catch(RuntimeException | IOException e) {
//            e.printStackTrace();
            System.out.printf("\u001B[31m%s\u001B[0m", e);
        }
    }
}
