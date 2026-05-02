package util;

import java.util.HashMap;
import java.util.Map;

public class TranslationUtil {

    private final Map<String, Integer> segments = new HashMap<>(Map.ofEntries(
            Map.entry("temp", 5),
            Map.entry("local", 1),
            Map.entry("argument", 2)
    ));
    private final Map<String, String> operations = new HashMap<>(Map.of(
            "add", "+",
            "sub", "-",
            "and", "&",
            "or", "|",
            "not", "!",
            "neg", "-"
    ));

    private String getAddressCommand(String segment){
        if (segment.equalsIgnoreCase("local")) return "LCL";
        else if(segment.equalsIgnoreCase("argument")) return "ARG";
        else return segment.toUpperCase();
    }

    public String saveOffsetAddrToTemp(String address, int offset, String segment){
        return
                "@"+getAddressCommand(segment)+" "+
                "D=M "+
                "@"+offset+" "+
                "D=D+A "+
                "@"+(segments.get("temp"))+" "+
                "M=D ";
    }

    public String writeMToD(String address){
        return
                "@"+address+" "+
                "D=M ";
    }

    public String writeAToD(int val){
        return
                "@"+val+" "+
                "D=A ";
    }

    public String writeDToM(String address){
        return
                "@"+address+" "+
                "M=D ";
    }

    public String push(String address, int offset, boolean hasOffset){
        if(!hasOffset){
            return
                    "@"+address+" "+
                    "D=M "+
                    "@SP "+
                    "A=M "+
                    "M=D ";
        }
        else{
            return
                    "@"+address+" "+
                    "A=M "+
                    "D=A "+
                    "@"+offset+" "+
                    "A=D+A "+
                    "D=M "+
                    "@SP "+
                    "A=M "+
                    "M=D ";
        }
    }

    public String incrementSP() {
        return
                "@SP "+
                "M=M+1 ";
    }

    public String pop(){
        return
                "@SP " +
                "M=M-1 " +
                "A=M " +
                "D=M ";
    }

    public String popForOp(){
        return
                "@SP "+
                "M=M-1 "+
                "A=M ";
    }

    public String binaryOp(String op){
        String symbol = operations.get(op);
        return "M=M"+symbol+"D ";
    }

   public String unaryOp(String op){
        String symbol = operations.get(op);
        return "M="+symbol+"D ";
   }

   public String writeTrueFlag(int id){
        return
                "(true_"+id+") "+
                "@1 "+
                "D=A "+
                "@SP "+
                "A=M "+
                "M=-D ";
   }

   public String writeFalseFlag(int id){
        return
                "(false_"+id+") "+
                "@SP "+
                "A=M "+
                "M=0 ";
   }
}