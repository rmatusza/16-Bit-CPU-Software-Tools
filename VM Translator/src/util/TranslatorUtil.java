package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TranslatorUtil {
    private final StringBuilder sb = new StringBuilder();
    private int id = 0;
    private final Map<String, String> operations = new HashMap<>(Map.of(
            "add", "+",
            "sub", "-",
            "and", "&",
            "or", "|",
            "not", "!",
            "neg", "-"
    ));

    public List<String> getInstructions() {
        return new ArrayList<>(List.of(sb.toString().stripTrailing().split(" ")));
    }

    public void incrementSymbolId(){
        id++;
    }

    public void saveOffsetAddressToTemp(String address, String offset, String temp) {
        sb.append("@").append(address).append(" ")
                .append("D=M ")
                .append("@").append(offset).append(" ")
                .append("D=D+A ")
                .append("@").append(temp).append(" ")
                .append("M=D ");
    }

    public void writeDToM(String address) {
        sb.append("@").append(address).append(" ")
                .append("M=D ");
    }

    public void writeDToMViaPointer(String addressPointer) {
        sb.append("@").append(addressPointer).append(" ")
                .append("A=M ")
                .append("M=D ");
    }

    public void incrementSP() {
        sb.append("@SP ")
                .append("M=M+1 ");
    }

    public void pop(String address, String offset){
        if(offset.isEmpty()){
            pop();
            writeDToM(address);
        }
        else{
            saveOffsetAddressToTemp(address, offset, "R13");
            pop();
            writeDToMViaPointer("R13");
        }
    }

    public void pop() {
        sb.append("@SP ")
                .append("M=M-1 ")
                .append("A=M ")
                .append("D=M ");
    }

    public void popForOp() {
        sb.append("@SP ")
                .append("M=M-1 ")
                .append("A=M ");
    }

    public void push(String address, String offset){
        if(Character.isDigit(address.charAt(0))) {
            sb.append("@").append(address).append(" ")
                    .append("D=A ")
                    .append("@SP ")
                    .append("A=M ")
                    .append("M=D ");
            incrementSP();
        }
        else if(offset.isEmpty()){
            sb.append("@").append(address).append(" ")
                    .append("D=M ")
                    .append("@SP ")
                    .append("A=M ")
                    .append("M=D ");
            incrementSP();
        }
        else {
            sb.append("@").append(address).append(" ")
                    .append("D=M ")
                    .append("@").append(offset).append(" ")
                    .append("A=D+A ")
                    .append("D=M ")
                    .append("@SP ")
                    .append("A=M ")
                    .append("M=D ");
            incrementSP();
        }
    }

    public void binaryOp(String op) {
        String symbol = operations.get(op);

        pop();
        popForOp();
        sb.append("M=M").append(symbol).append("D ");
        incrementSP();
    }

    public void unaryOp(String op) {
        String symbol = operations.get(op);

        pop();
        sb.append("M=").append(symbol).append("D ");
        incrementSP();
    }

    public void writeIsTrueHandler() {
        sb.append("(true_").append(id).append(") ")
                .append("@1 ")
                .append("D=A ")
                .append("@SP ")
                .append("A=M ")
                .append("M=-D ");
        incrementSP();
    }

    public void writeIsFalseHandler() {
        sb.append("(false_").append(id).append(") ")
                .append("@SP ")
                .append("A=M ")
                .append("M=0 ");
        incrementSP();
    }

    public void writeIsContinueHandler(){
        sb.append("(continue_").append(id).append(") ");
    }

    public void writeIsTrueCondition(String operation){
        sb.append("@true_").append(id).append(" ")
                .append(operation).append(" ");
    }

    public void writeIsFalseCondition(){
        sb.append("@false_").append(id).append(" ")
                .append("0;JMP").append(" ");
    }

    public void writeIsContinueCondition(){
        sb.append("@continue_").append(id).append(" ")
                .append("0;JMP").append(" ");
    }
}