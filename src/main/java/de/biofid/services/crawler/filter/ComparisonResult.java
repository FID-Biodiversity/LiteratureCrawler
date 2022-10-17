package de.biofid.services.crawler.filter;

import java.util.*;

/**
 * An enum class to standardize possible comparison results for Item evaluation.
 */
public enum ComparisonResult {
    LESS_THAN(List.of(-1), "lessthan"),
    EQUAL_LESS(List.of(-1 , 0), "equalless"),
    LESS_EQUAL(List.of(-1, 0), "lessequal"),  // Swapped "Duplicate" for convenience in the configuration file
    EQUAL(List.of(0), "equal"),
    EQUAL_GREATER(List.of(0, 1), "equalgreater"),
    GREATER_EQUAL(List.of(0, 1), "greaterequal"),  // Swapped "Duplicate" for convenience in the configuration file
    GREATER_THAN(List.of(1), "greaterthan"),

    CONTAINS(List.of(), "contains");


    private final List<Integer> value;
    private final String label;

    private static final Map<String, ComparisonResult> ENUM_LABEL_MAP;

    ComparisonResult(List<Integer> integerValue, String label) {
        this.value = integerValue;
        this.label = label;
    }

    // Generate a Map to look up an Enum value for a given String fast
    static {
        Map<String, ComparisonResult> map = new HashMap<>();
        for (ComparisonResult instance : ComparisonResult.values()) {
                map.put(instance.label, instance);
        }
        ENUM_LABEL_MAP = Collections.unmodifiableMap(map);
    }

    /**
     * Compares a given int to the internal value of this ComparisonResult object,
     * @param value A result from a comparison, ranging from -1 (less), 0 (equal), to 1 (greater).
     * @return True, if the given value is within the range of this Comparison object. False, otherwise.
     */
    public boolean equals(int value) {
        return this.value.contains(value);
    }

    /**
     * Compares a given ComparisonResult object to this object.
     * @param result Another ComparisonResult object.
     * @return True, if the two objects have the same values.
     */
    public boolean equals(ComparisonResult result) {
        return this.label.equals(result.label);
    }

    static public ComparisonResult fromString(String label) {
        String normalizedString = label.toLowerCase().strip();
        return ENUM_LABEL_MAP.get(normalizedString);
    }
}