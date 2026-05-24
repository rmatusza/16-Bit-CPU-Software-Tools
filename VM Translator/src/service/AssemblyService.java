package service;

import java.util.ArrayList;
import java.util.List;

public class AssemblyService {
    private final StringBuilder sb = new StringBuilder();

    public void initializeStackPointer() {
        sb.append("@256 ")
                .append("D=A ")
                .append("@SP ")
                .append("M=D ");
    }

    public List<String> getInstructions() {
        return new ArrayList<>(List.of(sb.toString().stripTrailing().split(" ")));
    }

    public void writeReturnAddressRef(String returnAdd, int id){
        sb.append("@").append(returnAdd).append("_RETURN_").append(id).append(" ");
    }

    public void writeReturnAddressLabel(String returnAdd, int id){
        sb.append("(").append(returnAdd).append("_RETURN_").append(id).append(") ");
    }

    public void setA(String val){
        sb.append("@").append(val).append(" ");
    }

    public void setA(int val){
        sb.append("@").append(val).append(" ");
    }

    // write D to pointer address
    public void writeDToMAMA(String address) {
        setA(address);
        sb.append("A=M ").append("M=D ");
    }

    public void writeGenericToMAMA(String address, String generic) {
        setA(address);
        sb.append("A=M ").append("M=").append(generic).append(" ");
    }

    // write D to variable address
    public void writeDToMA(String address) {
        setA(address);
        sb.append("M=D ");
    }

    public void writeConstToMA(String c) {
        sb.append("M=").append(c).append(" ");
    }

    // write variable address to D
    public void writeMAToD(String address) {
        setA(address);
        sb.append("D=M ");
    }

    public void writeMAToD() {
        sb.append("D=M ");
    }

    // write constant or label to D
    public void writeAToD(String i) {
        setA(i);
        sb.append("D=A ");
    }

    public void writeAToD(){
        sb.append("D=A ");
    }

    public void writeAToD(int i) {
        setA(i);
        sb.append("D=A ");
    }

    public void writeMAMAOpDToD(String a, String op) {
        setA(a);
        sb.append("A=M").append(op).append("D ")
                .append("D=M ");
    }

    public void writeMAMAOpDToM(String op) {
        /* NOTE: A is already equal to M >> M here then is M(A(M(A))*/
        sb.append("M=M").append(op).append("D ");
    }

    public void writeMAOpConstToD(String a, String op, int c) {
        setA(a);
        sb.append("D=M").append(op).append(c).append(" ");
    }

    public void writeMAToA(String a) {
        setA(a);
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

    public void offsetDByMAOpD(String addressBase, String op) {
        setA(addressBase);
        sb.append("D=M").append(op).append("D").append(" ");
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

    public void writeRef(String ref, int id){
        sb.append("@").append(ref).append(id).append(" ");
    }

    public void writeLabel(String name, String label){
        sb.append("(").append(name).append(".").append(label).append(") ");
    }

    public void writeLabel(String label, int id){
        sb.append("(").append(label).append(id).append(") ");
    }

    public void writeLabel(String label){
        sb.append("(").append(label).append(") ");
    }

    public void jump() {
        sb.append("0;JMP").append(" ");
    }

    public void jump(String cond){
        sb.append(cond).append(" ");
    }

    public void incrementSP() {
        sb.append("@SP ")
                .append("M=M+1 ");
    }

    /* CHECK */
    public void saveOffsetAddressToTemp(String address, int offset, String temp, String op) {
        writeAToD(offset);
        offsetDByMAOpD(address, op);
        writeDToMA(temp);
    }
}
