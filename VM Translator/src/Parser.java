import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// TODO: update the format method to be similar to the one in the assembler with syntax checks and an "instruction-to-line number" map for better error messages
public class Parser {
    private List<String> lines;
    private int index = 0;
    private int lineNum = 1;
    private List<String> currCom = new ArrayList<>();
    private final List<String> arithCom = List.of("add", "sub", "neg", "eq", "gt", "lt", "and", "or", "not");
    private final Map<String, CType> cMap = Map.of(
            "push", CType.C_PUSH,
            "pop", CType.C_POP,
            "math", CType.C_ARITHMETIC
    );

    public boolean hasMoreCommands() {
        return index < lines.size();
    }

    public void advance() {
        String command = lines.get(index);
        index++;
        lineNum++;
        this.currCom = new ArrayList<>(List.of(command.split(" ")));
    }

    public CType commandType() {
        String com = currCom.get(0);
        if(arithCom.contains(com)) return cMap.get("math");
        if(!cMap.containsKey(com)) throw new RuntimeException("invalid command :" + com + ": on line " + lineNum);
        return cMap.get(com);
    }

    public String argOne() {
        return currCom.size() == 1 ? currCom.get(0) : currCom.get(1);
    }

    public int argTwo() {
        String arg2Str = currCom.get(2);
        int arg2;
        try{
            arg2 = Integer.parseInt(arg2Str);
        }
        catch (NumberFormatException e) {
            throw new RuntimeException("invalid first argument :" + arg2Str + ": on line " + lineNum);
        }
        return arg2;
    }

    public boolean hasArgTwo(){
        if(currCom.size() < 3) return false;
        return true;
    }

    private String removeComments(String com) {
        int commentIdx = com.indexOf("//");
        if (commentIdx != -1) {
            com = com.substring(0, commentIdx);
        }
        return com.trim();
    }

    private void format() {
        List<String> formattedLines = new ArrayList<>();
        for(var l : lines) {
            l = l.trim();
            if(!l.isEmpty() && !l.startsWith("//")) {
                formattedLines.add(removeComments(l));
            }
        }
        lines = formattedLines;
    }

    public Parser(Path src) {
        try {
            lines = Files.readAllLines(src);
            format();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}