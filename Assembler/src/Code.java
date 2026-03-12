import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class Code {
    private final List<String> binary = new ArrayList<>();
    private Parser parser;
    private SymbolTable table;
    private String fileName;
    private Path binOut;
    private Path decOut;

    // sets the output paths and the filename (w/o extension)
    private void initializeOutputPaths(Path p) {
        String fileNameWE = p.getFileName().toString();

        if(!fileNameWE.endsWith(".asm")) throw new RuntimeException("Invalid file type :"+fileNameWE+": must provide a file with a .asm extension");

        this.fileName = fileNameWE.substring(0, fileNameWE.indexOf("."));

        this.binOut = p.getParent().resolve(fileName+"BIN.hack");
        this.decOut = p.getParent().resolve(fileName+"DEC.hack");
    }

    // labels are things like (LOOP_START) that indicate where the program should jump to if a certain condition is met
    private void addLabelsToSymbolTable() {
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
    }

    private void assembleBinary() {
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
    }

    private void writeDecimal() throws IOException {
        List<String> decimal = new ArrayList<>();
        binary.forEach(b -> {
            int d = Integer.parseInt(b, 2);
            decimal.add(String.valueOf(d));
        });
        Files.write(decOut, decimal, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private void writeOutput (boolean writeDec) throws IOException {
        Files.write(binOut, binary, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        if(writeDec){
            writeDecimal();
        }
    }

    public Code(String path, boolean writeDec) {
        try{
            Path asmFilePath = Paths.get(path);
            initializeOutputPaths(asmFilePath);

            this.parser = new Parser(asmFilePath);
            this.table = new SymbolTable();

            addLabelsToSymbolTable();
            parser.reset();

            assembleBinary();

            writeOutput(writeDec);

            System.out.printf("\u001B[32mAssembled file(s) in directory: %s \u001B[0m", fileName, asmFilePath.getParent());
        }
        catch(RuntimeException | IOException e) {
//            e.printStackTrace();
            System.out.printf("\u001B[31m%s\u001B[0m", e);
        }
    }
}