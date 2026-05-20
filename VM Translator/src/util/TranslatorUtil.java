package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TranslatorUtil {
    private final StringBuilder sb = new StringBuilder();
    private int id = 0;
    private final List<String> returnPointers = List.of("THAT", "THIS", "ARG", "LCL");
    private final List<String> callPointers = List.of("LCL", "ARG", "THIS", "THAT");
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

    /*                     UTIL API                                                 */

    public void setA(String val){
        sb.append("@").append(val).append(" ");
    }

    // write D to pointer address
    public void writeDToMAMA(String address) {
        setA(address);
        sb.append("A=M ")
                .append("M=D ");
    }

    // write D to variable address
    public void writeDToMA(String address) {
        setA(address);
        sb.append("M=D ");
    }

    public void writeConstToMA(String c) {
        sb.append("M=").append(c).append(" ");
    }

    // write pointer address to D
    public void writeMAMAToD(String address) {
        sb.append("@").append(address).append(" ")
                .append("A=M ")
                .append("D=M ");
    }

    // write variable address to D
    public void writeMAToD(String address) {
        setA(address);
        sb.append("D=M ");
    }

    // write constant or label to D
    public void writeAToD(String i) {
        setA(i);
        sb.append("D=A ");
    }

    public void writeMAMAOpDToD(String a, String op) {
        setA(a);
        sb.append("A=M").append(op).append("D ")
                .append("D=M ");
    }

    public void writeMAOpConstToD(String a, String op, int c) {
        setA(a);
        sb.append("D=M").append(op).append(c).append(" ");
    }

    public void writeMAToA(String i) {
        setA(i);
        sb.append("A=M ");
    }

    // D +/- A
    public void offsetDByA(int offset, String op) {
        setA(offset+"");
        sb.append("D=D").append(op).append("A").append(" ");
    }

    public void offsetAByMAOpD(String addressBase, String op) {
        setA(addressBase);
        sb.append("A=M").append(op).append("D").append(" ");
    }

    // push D to stack and increment
    public void pushD() {
        setA("SP");
        sb.append("A=M ")
                .append("M=D ")
                .append("@SP ")
                .append("M=M+1 ");
    }

    // pop top of stack to D
    public void popToD() {
        setA("SP");
        sb.append("M=M-1 ")
                .append("A=M ")
                .append("D=M ");
    }

    // get top of stack address
    public void getNextStackAddress() {
        setA("SP");
        sb.append("M=M-1 ")
                .append("A=M ");
    }

    public void writeLabelRef(String name, String label){
        sb.append("@").append(name).append(".").append(label).append(" ");
    }

    public void writeLabel(String name, String label){
        sb.append("(").append(name).append(".").append(label).append(") ");
    }

    public void writeGoto(String name, String label){
        writeLabelRef(name, label);
        jump();
    }

    public void writeGotoVarAddress(String v) {
        writeMAToA(v);
        jump();
    }

    public StringBuilder generateReturnAddressName(String name) {
        StringBuilder b = new StringBuilder();
        b.append(name).append("_").append("RETURN").append("_").append(id++);
        return b;
    }

    public void jump() {
        sb.append("0;JMP").append(" ");
    }

    public void jump(String cond){
        sb.append("D;").append(cond).append(" ");
    }

    /*                          CODE WRITER API                                            */

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

    public void saveOffsetAddressToTemp(String address, String offset, String temp) {
        sb.append("@").append(address).append(" ")
                .append("D=M ")
                .append("@").append(offset).append(" ")
                .append("D=D+A ")
                .append("@").append(temp).append(" ")
                .append("M=D ");
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

    private void writeLabelRef(String name){
        sb.append("@").append(name).append(" ");
    }

    public void writeLabel(String name){
        sb.append("(").append(name).append(") ");
    }

    public void pushReturnAddress(String returnAddr) {
        writeAToD(returnAddr);
        pushD();
    }

    public void writeGoto(String name){
        writeLabelRef(name);
        jump();
    }

    public void writeIf(String filename, String label){
        popToD();
        writeLabelRef(filename, label);
        jump("JNE");
    }

    public void pushCallerPointers() {
        for(var p : callPointers) {
            writeMAToD(p);
            pushD();
        }
    }

    public void repositionCalleeArg(int numArgs) {
        int i = numArgs + 5;
        writeMAToD("SP");
        offsetDByA(i, operations.get("sub"));
        writeDToMA("ARG");
    }

    public void repositionCalleeLcl(){
        writeMAToD("SP");
        writeDToMA("LCL");
    }

    public void setFrameVar() {
        writeMAToD("LCL");
        writeDToMA("FRAME");
    }

    public void setReturnAddrVar(){
        writeAToD("5");
        writeMAMAOpDToD("FRAME", operations.get("sub"));
        writeDToMA("RET");
    }

    public void setReturnValue(){
        popToD();
        writeDToMAMA("ARG");
    }

    public void restoreCallerSP(){
        writeMAOpConstToD("ARG", operations.get("add"), 1);
        writeDToMA("SP");
    }

    public void restoreCallerPointers(){
        for (int i = 0; i < 4; i++) {
            writeAToD(i+1+"");
            writeMAMAOpDToD("FRAME", operations.get("sub"));
            writeDToMA(returnPointers.get(i));
        }
    }

    public void initializeLocals(int numLocals){
        for(int i=0; i<numLocals; i++){
           writeAToD(i+"");
           offsetAByMAOpD("LCL", operations.get("add"));
           writeConstToMA("0");
        }
    }
}