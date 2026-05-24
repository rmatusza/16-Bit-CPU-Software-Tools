package util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class CodeWriterUtil {
    private String filename;
    private String modifier;
    public final Set<String> hasOffsetAddress = new HashSet<>(Set.of("local", "argument", "this", "that"));
    public final Map<String, String> conditionalBranchMap = Map.of(
            "eq", "D;JEQ",
            "lt", "D;JLT",
            "gt", "D;JGT",
            "ne", "D;JNE"
    );

    public final Map<String, String> tempLocations = new HashMap<>(Map.of(
            "0", "R5",
            "1", "R6",
            "2", "R7",
            "3", "R8",
            "4", "R9",
            "5", "R10",
            "6", "R11",
            "7", "R12"
    ));
    private final Map<String, Supplier<String>> addressMap = new HashMap<>(Map.ofEntries(
            Map.entry("temp", () -> tempLocations.get(modifier)),
            Map.entry("local", () -> "LCL"),
            Map.entry("argument", () -> "ARG"),
            Map.entry("static", () -> filename+"."+modifier),
            Map.entry("pointer", () -> (modifier.equalsIgnoreCase("0") ? "THIS" : "THAT")),
            Map.entry("this", () -> "THIS"),
            Map.entry("that", () -> "THAT")
    ));

    public Supplier<String> getAddressOrDefault(String address, String modifier, String filename){
        this.filename = filename;
        this.modifier = modifier;
        return addressMap.getOrDefault(address, () -> modifier);
    }

    public Supplier<String> getAddress(String address, String modifier, String filename){
        this.filename = filename;
        this.modifier = modifier;
        return addressMap.get(address);
    }
}