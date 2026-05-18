package util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ParserUtil {

    public static final Map<String, CType> cMap = Map.of(
            "push", CType.C_PUSH,
            "pop", CType.C_POP,
            "math", CType.C_ARITHMETIC,
            "label", CType.C_LABEL,
            "goto", CType.C_GOTO,
            "if-goto", CType.C_IF,
            "function", CType.C_FUNCTION,
            "return", CType.C_RETURN,
            "call", CType.C_CALL
    );

    public static final Set<String> memLocs = new HashSet<>(Set.of(
            "constant",
            "local",
            "argument",
            "this",
            "that",
            "temp",
            "pointer",
            "static"
    ));

    public static final Set<String> arithCom = new HashSet<>(Set.of(
            "add",
            "sub",
            "neg",
            "eq",
            "gt",
            "lt",
            "and",
            "or",
            "not"
    ));

    public static final Set<String> memCom = new HashSet<>(Set.of(
            "push",
            "pop"
    ));

    public static final Set<String> progFlowCom = new HashSet<>(Set.of(
            "label",
            "goto",
            "if-goto"
    ));

    public static final Set<String> funCallCom = new HashSet<>(Set.of(
            "function",
            "call",
            "return"
    ));

    // maps command name to required argument count
    public static final Map<String, Integer> comsMap = new HashMap<>();

    public static final Set<String> allComs = new HashSet<>();

    static
    {
        arithCom.forEach(c -> comsMap.put(
                c, 1
        ));
        funCallCom.forEach(c -> comsMap.put(
                c, (c.equals("return") ? 1 : 3)
        ));
        progFlowCom.forEach(c -> comsMap.put(
                c, 2
        ));
        memCom.forEach(c -> comsMap.put(
                c, 3
        ));

        allComs.addAll(comsMap.keySet());
    }
}