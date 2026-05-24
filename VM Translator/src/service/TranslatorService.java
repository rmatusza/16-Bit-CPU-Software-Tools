package service;

import util.TranslatorUtil;

import java.util.List;

public class TranslatorService {
    private final AssemblyService assemblyService = new AssemblyService();
    private final TranslatorUtil util = new TranslatorUtil();
    private int id = 0;

    public void incrementSymbolId(){
        id++;
    }

    public List<String> getAssembly(){
        return assemblyService.getInstructions();
    }

    /* ARITHMETIC, LOGIC, MEMORY ACCESS */

    public void popToD(){
        assemblyService.popToD();
    }

    public void popToAddress(String address, String offset){
        if(offset.isEmpty()){
            popToAddress(address);
        }
        else{
            popToAddress(address, Integer.parseInt(offset));
        }
    }

    public void popToAddress(String address){
        assemblyService.popToD();
        assemblyService.writeDToMA(address);
    }

    public void popToAddress(String address, int offset){
        assemblyService.saveOffsetAddressToTemp(address, offset, "R13", util.operations.get("add"));
        assemblyService.popToD();
        assemblyService.writeDToMAMA("R13");
    }

    public void push(String address, String offset, String constant){
        if(offset.isEmpty() && constant.isEmpty()){
            push(address);
        }
        else if(!constant.isEmpty()){
            push(Integer.parseInt(constant));
        }
        else{
            push(address, Integer.parseInt(offset));
        }
    }

    public void push(int val){
        assemblyService.writeAToD(val);
        assemblyService.writeDToMAMA("SP");
        assemblyService.incrementSP();
    }

    public void push(String address){
        assemblyService.writeMAToD(address);
        assemblyService.writeDToMAMA("SP");
        assemblyService.incrementSP();
    }

    public void push(String address, int offset){
        assemblyService.writeAToD(offset);
        assemblyService.offsetAByMAOpD(address, util.operations.get("add"));
        assemblyService.writeMAToD();
        assemblyService.writeDToMAMA("SP");
        assemblyService.incrementSP();
    }

    public void binaryOp(String op) {
        // (sp-2) operation (sp-1) >> if stack is 5, 2 with 2 being the top then sub command would be 5 - 2
        assemblyService.popToD();
        assemblyService.getNextStackAddress();
        assemblyService.writeMAMAOpDToM(util.operations.get(op));
        assemblyService.incrementSP();
    }

    public void unaryOp(String op) {
        assemblyService.popToD();
        assemblyService.writeMAMAOpDToM(util.operations.get(op));
        assemblyService.incrementSP();
    }

    public void writeIsTrueHandler() {
        assemblyService.writeLabel(util.conditionals.get("true"), id);
        assemblyService.writeGenericToMAMA("SP", "-1");
        assemblyService.incrementSP();
    }

    public void writeIsFalseHandler() {
        assemblyService.writeLabel(util.conditionals.get("false"), id);
        assemblyService.writeGenericToMAMA("SP", "0");
        assemblyService.incrementSP();
    }

    public void writeIsContinueHandler(){
        assemblyService.writeLabel(util.conditionals.get("continue"), id);
    }

    public void writeIsTrueCondition(String op){
        assemblyService.writeRef(util.conditionals.get("true"), id);
        assemblyService.jump(op);
    }

    public void writeIsFalseCondition(){
        assemblyService.writeRef(util.conditionals.get("false"), id);
        assemblyService.jump();
    }

    public void writeIsContinueCondition(){
        assemblyService.writeRef(util.conditionals.get("continue"), id);
        assemblyService.jump();
    }

    /* PROGRAM CONTROL */

    public void initialize(){
        assemblyService.initializeStackPointer();
    }

    public void writeLabel(String label){
        assemblyService.writeLabel(label);
    }

    public void writeReturnAddressLabel(String functionName){
        assemblyService.writeReturnAddressLabel(functionName, id);
    }

    public void writeLabel(String qualifier, String label){
        assemblyService.writeLabel(qualifier, label);
    }

    public void pushReturnAddress(String returnAdd) {
        assemblyService.writeReturnAddressRef(returnAdd, id);
        assemblyService.writeAToD();
        assemblyService.pushD();
    }

    public void writeGoto(String label){
        assemblyService.setA(label);
        assemblyService.jump();
    }

    public void writeGoto(String qualifier, String label){
        assemblyService.writeLabelRef(qualifier, label);
        assemblyService.jump();
    }

    public void writeGotoVar(String label){
        assemblyService.writeMAToA(label);
        assemblyService.jump();
    }

    public void writeIf(String filename, String label, String cond){
        assemblyService.popToD();
        assemblyService.writeLabelRef(filename, label);
        assemblyService.jump(cond);
    }

    public void pushCallerPointers() {
        for(var p : util.callPointers) {
            assemblyService.writeMAToD(p);
            assemblyService.pushD();
        }
    }

    public void repositionCalleeArg(int numArgs) {
        int i = numArgs + 5;
        assemblyService.writeMAToD("SP");
        assemblyService.offsetDByA(i, util.operations.get("sub"));
        assemblyService.writeDToMA("ARG");
    }

    public void repositionCalleeLcl(){
        assemblyService.writeMAToD("SP");
        assemblyService.writeDToMA("LCL");
    }

    public void setFrameVar() {
        assemblyService.writeMAToD("LCL");
        assemblyService.writeDToMA("FRAME");
    }

    public void setReturnAddrVar(){
        assemblyService.writeAToD("5");
        assemblyService.writeMAMAOpDToD("FRAME", util.operations.get("sub"));
        assemblyService.writeDToMA("RET");
    }

    public void setReturnValue(){
        assemblyService.popToD();
        assemblyService.writeDToMAMA("ARG");
    }

    public void restoreCallerSP(){
        assemblyService.writeMAOpConstToD("ARG", util.operations.get("add"), 1);
        assemblyService.writeDToMA("SP");
    }

    public void restoreCallerPointers(){
        for (int i = 0; i < 4; i++) {
            assemblyService.writeAToD(i+1+"");
            assemblyService.writeMAMAOpDToD("FRAME", util.operations.get("sub"));
            assemblyService.writeDToMA(util.returnPointers.get(i));
        }
    }

    public void initializeLocals(int numLocals){
        for(int i=0; i<numLocals; i++){
            assemblyService.writeAToD(0);
            assemblyService.pushD();
        }
    }
}