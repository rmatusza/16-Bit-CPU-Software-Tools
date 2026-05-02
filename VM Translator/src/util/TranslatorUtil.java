package util;

import java.util.HashMap;
import java.util.Map;

public class TranslatorUtil {
    private final Map<String, String> operations = new HashMap<>(Map.of(
            "add", "+",
            "sub", "-",
            "and", "&",
            "or", "|",
            "not", "!",
            "neg", "-"
    ));

    public String saveOffsetAddrToTemp(String address, int offset, String temp){
        return
                "@"+address+" "+
                "D=M "+
                "@"+offset+" "+
                "D=D+A "+
                "@"+temp+" "+
                "M=D ";
    }

    public String writeDToM(String address){
        return
                "@"+address+" "+
                "M=D ";
    }

    public String writeDToMViaPointer(String address){
        return
                "@"+address+" "+
                "A=M "+
                "M=D ";
    }

    public String push(int address, int offset, boolean hasOffset){
        if(!hasOffset){
            return
                    "@"+address+" "+
                    "D=A "+
                    "@SP "+
                    "A=M "+
                    "M=D "+
                    incrementSP();
        }
        else{
            return
                    "@"+address+" "+
                    "D=M "+
                    "@"+offset+" "+
                    "A=D+A "+
                    "D=M "+
                    "@SP "+
                    "A=M "+
                    "M=D "+
                    incrementSP();
        }
    }

    public String push(String address, int offset, boolean hasOffset){
        if(!hasOffset){
            return
                    "@"+address+" "+
                    "D=M "+
                    "@SP "+
                    "A=M "+
                    "M=D "+
                    incrementSP();
        }
        else{
            return
                    "@"+address+" "+
                    "D=M "+
                    "@"+offset+" "+
                    "A=D+A "+
                    "D=M "+
                    "@SP "+
                    "A=M "+
                    "M=D "+
                    incrementSP();
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
        return pop() + popForOp() + "M=M"+symbol+"D " + incrementSP();
    }

   public String unaryOp(String op){
        String symbol = operations.get(op);
        return pop() + "M="+symbol+"D " + incrementSP();
   }

   public String writeTrueFlag(int id){
        return
                "(true_"+id+") "+
                "@1 "+
                "D=A "+
                "@SP "+
                "A=M "+
                "M=-D "+
                incrementSP();
   }

   public String writeFalseFlag(int id){
        return
                "(false_"+id+") "+
                "@SP "+
                "A=M "+
                "M=0 "+
                incrementSP();
   }
}