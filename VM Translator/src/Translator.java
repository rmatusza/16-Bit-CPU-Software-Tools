import java.io.IOException;
import java.util.List;

public class Translator {
    public static void main(String[] args) throws IOException {
//        List<String> paths = List.of(
//                "BasicTest",
//                "PointerTest",
//                "SimpleAdd",
//                "StackTest",
//                "StaticTest"
//        );
        List<String> paths = List.of(
                "FibonacciElement"
        );

        boolean ENFORCE_SYS;

        try{
            ENFORCE_SYS = Boolean.parseBoolean(args[0]);
        }
        catch (RuntimeException e) {
            ENFORCE_SYS = true;
        }


        for(var p : paths){
            new CodeWriter("VM Translator/test/"+p, ENFORCE_SYS);
        }
    }
}