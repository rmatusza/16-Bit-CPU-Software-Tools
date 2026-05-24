import java.io.IOException;
import java.util.List;

public class Translator {
    public static void main(String[] args) throws IOException {
        boolean ENFORCE_SYS;

//        String baseDir = "VM Translator/test/Part1";
//        List<String> paths = List.of(
//                "BasicTest",
//                "PointerTest",
//                "SimpleAdd",
//                "StackTest",
//                "StaticTest"
//        );

        String baseDir = "VM Translator/test/Part2/";
        List<String> paths = List.of(
                "NestedCall",
                "FibonacciElement"

        );

        try{
            ENFORCE_SYS = Boolean.parseBoolean(args[0]);
        }
        catch (RuntimeException e) {
            ENFORCE_SYS = true;
        }

        for(var p : paths){
            new CodeWriter(baseDir+p, ENFORCE_SYS);
        }
    }
}