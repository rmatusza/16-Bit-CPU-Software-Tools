package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TranslatorUtil {
    private final StringBuilder sb = new StringBuilder();
    private int id = 0;
    private final Map<String, String> functionNames = new HashMap<>();
    private final Map<String, String> operations = new HashMap<>(Map.of(
            "add", "+",
            "sub", "-",
            "and", "&",
            "or", "|",
            "not", "!",
            "neg", "-"
    ));

    public void initializeStackPointer() {
       sb.append("@256 ")
               .append("D=A ")
               .append("@SP ")
               .append("M=D ");
    }

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

    public void pushLabel(String label){
        sb.append("@").append(label).append(" ")
                .append("D=A ")
                .append("@SP ")
                .append("A=M ")
                .append("M=D ");
        incrementSP();
    }

    public void pushPointer(String pointer){
        sb.append("@").append(pointer).append(" ")
                .append("D=M ") // D = address that the pointer is pointing to
                .append("@SP ")
                .append("A=M ")
                .append("M=D ");
        incrementSP();
    }

    public void binaryOp(String operation) {
        String symbol = operations.get(operation);
        // (sp-2) operation (sp-1) >> if stack is 5, 2 with 2 being the top then sub command would be 5 - 2
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

    // PROGRAM CONTROL

    private void writeLabelRef(String name, String label){
        sb.append("@").append(name).append(".").append(label).append(" ");
    }

    public void writeLabel(String filename, String label){
        sb.append("(").append(filename).append(".").append(label).append(") ");
    }

    private void writeLabelRef(String name){
        sb.append("@").append(name).append(" ");
    }

    public void writeLabel(String name){
        sb.append("(").append(name).append(") ");
    }

    public StringBuilder generateReturnAddressName(String name) {
        StringBuilder b = new StringBuilder();
        b.append(name).append("_").append("RETURN").append("_").append(id++);
        return b;
    }

    public void pushReturnAddress(String returnAddr) {
        pushLabel(returnAddr);
    }

    public void writeGoto(String name, String label){
        writeLabelRef(name, label);
        sb.append("0;JMP ");
    }

    public void writeGoto(String name){
        writeLabelRef(name);
        sb.append("0;JMP ");
    }

    public void writeGotoVarAddress(String v) {
        sb.append("@").append(v).append(" ")
                .append("A=M ")
                .append("0;JMP ");
    }

    public void writeIf(String filename, String label){
        pop();
        writeLabelRef(filename, label);
        sb.append("D;JNE ");
    }

    public void pushCallerPointers() {
        pushPointer("LCL");
        pushPointer("ARG");
        pushPointer("THIS");
        pushPointer("THAT");
    }

    public void repositionCalleeArg(int numArgs) {
        int i = numArgs + 5;
        sb.append("@SP ")
                .append("D=M ")
                .append("@").append(i).append(" ")
                .append("D=D-A ")
                .append("@ARG ")
                .append("M=D ");
    }

    public void repositionCalleeLcl(){
        sb.append("@SP ")
                .append("D=M ")
                .append("@LCL ")
                .append("M=D ");
    }

    public void setFrameVar() {
        sb.append("@LCL ")
                .append("D=M ")
                .append("@FRAME ")
                .append("M=D ");
    }

    private void modifyPointerViaFrame(int i, String pointer) {
        sb.append("@").append(i).append(" ")
                .append("D=A ")
                .append("@FRAME ")
                .append("A=M-D ") // A = address that contains the pointer address
                .append("D=M ") // D = pointer address
                .append("@").append(pointer).append(" ")
                .append("M=D ");
    }

    public void setReturnAddrVar(){
        sb.append("@").append(5).append(" ")
                .append("D=A ")
                .append("@FRAME ")
                .append("A=M-D ") // A = address that contains the return address
                .append("D=M ") // D = return address
                .append("@RET ")
                .append("M=D ");
    }

    public void setReturnValue(){
        pop();
        writeDToMViaPointer("ARG");
    }

    public void restoreCallerSP(){
        sb.append("@").append("ARG ")
                .append("D=M+1 ")
                .append("@SP ")
                .append("M=D ");
    }

    public void restoreCallerPointers(){
        List<String> pointers = List.of("THAT", "THIS", "ARG", "LCL");
        for (int i = 0; i < 4; i++) {
            modifyPointerViaFrame(i+1, pointers.get(i));
        }
    }

    public void initializeLocals(int numLocals){
        for(int i=0; i<numLocals; i++){
            push(i+"", "");
            pop("LCL", i+"");
        }
    }
}