import java.io.IOException;
import java.util.List;

public class Translator {
    public static void main(String[] args) throws IOException {
        List<String> paths = List.of(
                "BasicTest",
                "PointerTest",
                "SimpleAdd",
                "StackTest",
                "StaticTest"
        );

        for(var p : paths){
            new CodeWriter("VM Translator/test/"+p);
        }
    }
}