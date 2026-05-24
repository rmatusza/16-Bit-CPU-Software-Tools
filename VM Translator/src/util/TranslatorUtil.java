package util;

import java.util.List;
import java.util.Map;

public class TranslatorUtil {
    public final List<String> returnPointers = List.of("THAT", "THIS", "ARG", "LCL");
    public final List<String> callPointers = List.of("LCL", "ARG", "THIS", "THAT");
    public final Map<String, String> conditionals = Map.of("true", "true_", "false", "false_", "continue", "continue_");
    public final Map<String, String> operations = Map.of(
            "add", "+",
            "sub", "-",
            "and", "&",
            "or", "|",
            "not", "!",
            "neg", "-"
    );
}
