import util.CType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static util.ParserUtil.*;

public class Parser {
    private List<String> lines;
    private int index = 0;
    private int lineNum = 1;
    private String filename;
    private List<String> currCom = new ArrayList<>();
    private final Map<Integer, Integer> lineTracker = new HashMap<>();

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
        if(!cMap.containsKey(com)) throw new RuntimeException("invalid command '" + com + "' in " +filename+ ".vm on line "+lineTracker.get(index-1));
        return cMap.get(com);
    }

    public String argOne() {
        return currCom.size() == 1 ? currCom.get(0) : currCom.get(1);
    }

    public String argTwo() {
        return currCom.get(2);
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
        for (String l : lines) {
            l = l.trim();
            if (!l.isEmpty() && !l.startsWith("//")) {
                formattedLines.add(removeComments(l));
                lineTracker.put(index++, lineNum);
            }
            lineNum++;
        }
        lines = formattedLines;
        index = 0;
    }

    private void validateCommand(String com) {
        if(!allComs.contains(com)){
            throw new RuntimeException("Invalid command '"+com+"' in " +filename+ ".vm on line "+lineTracker.get(index));
        }
    }

    private void validateArgCount(String[] coms){
        if(coms.length != comsMap.get(coms[0])) {
            throw new RuntimeException("Invalid number of command arguments '"+comsMap.get(coms[0])+"' for command type '"+coms[0]+"' in " +filename+ ".vm on line "+lineTracker.get(index));
        }
    }

    private void validateMemoryLocation(String loc){
        if(!memLocs.contains(loc)){
            throw new RuntimeException("Invalid memory location '"+loc+"' in " +filename+ ".vm on line "+lineTracker.get(index));
        }
    }

    private void validateName(String name) {
        String regex = "^[A-Za-z_.:][A-Za-z0-9_.:]*$";

        if(!name.matches(regex)){
            throw new RuntimeException("Invalid label or function mame '"+name+"' in " +filename+ ".vm on line "+lineTracker.get(index)+"\n\u001B[33mlabels and functions can be composed of any sequence of letters, digits, underscore(_), dot (.), and colon (:) that does NOT begin with a digit\u001B[0m");
        }
    }

    private void validateSyntax() {
        for(int i=0; i<lines.size(); i++) {
            index = i;
            String l = lines.get(i);
            String[] lArr = l.split(" ");

            validateCommand(lArr[0]);
            validateArgCount(lArr);
            if(memCom.contains(lArr[0])){
                validateMemoryLocation(lArr[1]);
            }
            if(lArr[0].equals("function") || lArr[0].equals("label")){
                validateName(lArr[1]);
            }
        }
        index = 0;
    }

    public Parser(Path src, String filename) {
        this.filename = filename;
        try {
            lines = Files.readAllLines(src);
            format();
            validateSyntax();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}