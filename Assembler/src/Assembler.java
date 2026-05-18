public class Assembler {

    public static void main(String[] args) {
//        try{
//            boolean writeDecFile = Boolean.parseBoolean(args[1]);
//            new Code(args[0], writeDecFile);
//        }
//        catch (Exception e){
//            new Code(args[0], false);
//        }
        new Code("Assembler/test/FibonacciElement.asm", false);
//        new Code("test/pong.asm", false);
//        new Code("C:\\Program Files\\nand2tetris\\projects\\6\\rect\\Rect.asm", false);
    }
}