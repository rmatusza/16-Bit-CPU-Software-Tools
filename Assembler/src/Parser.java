import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Parser {
    private List<String> lines;
    private int index = 0; // for incrementing over the list of instructions
    private int lineNum = 1; // for error messages so user can see the line that caused a compilation error (increments on every iteration during parsing and formatting)
    private int romAddr = 0; // for keeping track of the ROM address of a symbol - (only increments for A and C instructions)
    private int varAddr = 16; // for keeping track of the RAM address of a variable
    private String currCom;
    private List<String> parts;
    private boolean isJmp;
    Map<Integer, Integer> instToLn = new HashMap<>();

    private final Map<String, String> destMappings = Map.of(
        "null", "000",
        "M", "001",
        "D", "010",
        "MD", "011",
        "A", "100",
        "AM", "101",
        "AD", "110",
        "AMD", "111"
    );

    private final Map<String, String> jumpMappings = Map.of(
        "null", "000",
        "JGT", "001",
        "JEQ", "010",
        "JGE", "011",
        "JLT", "100",
        "JNE", "101",
        "JLE", "110",
        "JMP", "111"
    );

    private final Map<String, String> compMappings = Map.ofEntries(
        Map.entry("0", "0101010"),
        Map.entry("1", "0111111"),
        Map.entry("-1", "0111010"),
        Map.entry("D", "0001100"),
        Map.entry("A", "0110000"),
        Map.entry("!D", "0001101"),
        Map.entry("!A", "0110001"),
        Map.entry("-D", "0001111"),
        Map.entry("-A", "0110011"),
        Map.entry("D+1", "0011111"),
        Map.entry("A+1", "0110111"),
        Map.entry("D-1", "0001110"),
        Map.entry("A-1", "0110010"),
        Map.entry("D+A", "0000010"),
        Map.entry("D-A", "0010011"),
        Map.entry("A-D", "0000111"),
        Map.entry("D&A", "0000000"),
        Map.entry("D|A", "0010101"),
        Map.entry("M", "1110000"),
        Map.entry("!M", "1110001"),
        Map.entry("-M", "1110011"),
        Map.entry("M+1", "1110111"),
        Map.entry("M-1", "1110010"),
        Map.entry("D+M", "1000010"),
        Map.entry("M+D", "1000010"),
        Map.entry("D-M", "1010011"),
        Map.entry("M-D", "1000111"),
        Map.entry("D&M", "1000000"),
        Map.entry("D|M", "1010101")
    );

    public boolean hasMoreCommands()
    {
        return index < lines.size();
    }

    public void advance()
    {
        this.currCom = lines.get(index);
        index++;
        lineNum++;
    }

    public CType commandType()
    {
        char cc = currCom.charAt(0);
        if(cc == '(') return CType.L_COMMAND;
        else if(cc == '@') return CType.A_COMMAND;
        else return CType.C_COMMAND;
    }

    public String symbol()
    {
        CType ct = commandType();
        if (ct == CType.L_COMMAND) {
            String com = currCom.substring(1, currCom.indexOf(')')).trim();
//            if(!com.equals(com.toUpperCase())) throw new RuntimeException("Invalid label :" + com + ": labels must be in all caps. Instruction " + lineNum);
            return currCom.substring(1, currCom.indexOf(')'));
        }
        else {
            return currCom.substring(1);
        }
    }

    public String comp()
    {
        if(currCom.contains("=")) {
            parts = List.of(currCom.split("="));
            isJmp=false;
        }
        else {
            parts = List.of(currCom.split(";"));
            isJmp=true;
        }

        String mnemonic = isJmp ? parts.get(0) : parts.get(1);

        if(!compMappings.containsKey(mnemonic)) throw new RuntimeException("invalid computation mnemonic :" + mnemonic + ": instruction " + lineNum);
        return compMappings.get(mnemonic);
    }

    public String dest()
    {
        if(isJmp) return "000";

        String mnemonic = parts.get(0);
        if(!destMappings.containsKey(mnemonic)) throw new RuntimeException("invalid destination mnemonic :" + mnemonic + ": instruction " + lineNum);
        return destMappings.get(mnemonic);
    }

    public String jump()
    {
        if(!isJmp) return "000";

        String mnemonic = parts.get(1);
        if(!jumpMappings.containsKey(mnemonic)) throw new RuntimeException("invalid jump mnemonic :" + mnemonic + ": instruction " + lineNum);
        return jumpMappings.get(mnemonic);
    }

    public int getLineNum(){return instToLn.get(romAddr);}

    public int getRomAddr(){return this.romAddr;}

    public int getAndIncrVarAddr(){return this.varAddr++;}

    public void reset(){this.index=0; this.romAddr=0; this.lineNum=0;}

    private String peek() {
        if(hasMoreCommands()) return lines.get(index);
        return "";
    }

    public boolean isVar(){
        return peek().contains("=");
        // false when peek().contains(";") i.e. is a label reference
    }

    public boolean isConst(){
       return Character.isDigit(currCom.charAt(1)); // checking data type of character after the @ symbol
    }

    public void incrRomAddr(){this.romAddr++;}

    private String removeComments(String com) {
        int commentIdx = com.indexOf("//");
        if (commentIdx != -1) {
            com = com.substring(0, commentIdx);
        }
        else if(com.contains("/")) throw new RuntimeException("invalid character :/: on line " + lineNum);
        return com.trim();
    }

    private boolean isValidSyntax(String l) {
        boolean aInst = l.charAt(0) == '@';
        boolean isValidAInst = false;
        boolean isLabel = l.charAt(0) == '(';
        boolean isValidLabel = false;
        boolean isValidCInst = false;
        if(isLabel){
            if(l.contains(")")){
                isValidLabel = l.substring(1, l.indexOf(")")).matches("^([A-Za-z_.$:][A-Za-z0-9_.$:]*)$");
            }
        }else if(aInst) {
            isValidAInst = l.substring(1).matches("^(?:\\d+|[A-Za-z_.$:][A-Za-z0-9_.$:]*)$");
        }
        else {
            if(l.contains("=")){
                List<String> parts = List.of(l.split("="));
                isValidCInst = (l.indexOf("=") == parts.get(0).split("").length) && l.indexOf("=") == l.lastIndexOf("=");
            }
            else if(l.contains(";")) {
                List<String> parts = List.of(l.split(";"));
                isValidCInst = (l.indexOf(";") == parts.get(0).split("").length) && l.indexOf(";") == l.lastIndexOf(";");
            }
        }

        return (isValidAInst || isValidCInst || isValidLabel);
    }

    private void format() {
        List<String> formattedLines = new ArrayList<>();
        for(var l : lines) {
            l = l.trim();
            if(!l.isEmpty() && !l.startsWith("//")) {
                String formatted = removeComments(l);
                if(!isValidSyntax(formatted)) throw new RuntimeException("Invalid syntax :" + l + ": on line " + (lineNum));
                if(!l.startsWith("(")) {
                    instToLn.put(romAddr, lineNum); // map the rom address to the line number
                    romAddr++; // only increment rom address when A or C instruction
                }
                formattedLines.add(formatted);
            }
            lineNum++; // increment line number no matter what
        }
        lines = formattedLines;
    }

    public int getVarAddr() {
        return this.varAddr;
    }

    public Parser(Path src)
    {
        try {
            lines = Files.readAllLines(src);
            format(); // verify syntax and remove whitespace/comments
            reset();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}