package de.biofid.services.crawler.filter;

import de.biofid.services.crawler.Item;
import org.json.JSONObject;

/**
 * A filter that evaluates Items by criteria that are Strings.
 * String comparisons are case-insensitive!
 * Given strings are not considered words, but only need to be part of the given text.
 * Example:
 *      Given is the string "test". Following titles will be filtered out:
 *          "A Story of Testing", "I test my code"
 */
public class StringFilter extends Filter {

    private final String expectedValue;
    private final String metadataParameterName;
    private final ComparisonResult comparisonResult;

    public StringFilter(String metadataParameterName, String expectedValue, ComparisonResult comparisonResult) {
        this.expectedValue = expectedValue;
        this.metadataParameterName = metadataParameterName;
        this.comparisonResult = comparisonResult;
    }

    @Override
    public boolean isItemValid(Item item) {
        String defaultValue = null;
        boolean evaluationResult;
        String itemMetadataValue = getValueForCaseInsensitiveKey(
                metadataParameterName, item.getDocumentMetadata(), defaultValue);

        if (itemMetadataValue != null) {
            itemMetadataValue = itemMetadataValue.toLowerCase();
        }

        if (itemMetadataValue.equals(defaultValue)) {
            evaluationResult = true;
        } else if (comparisonResult.equals(ComparisonResult.CONTAINS)) {
            evaluationResult = itemMetadataValue.contains(expectedValue);
        } else {
            int numericResult = itemMetadataValue.compareTo(expectedValue);
            evaluationResult = comparisonResult.equals(numericResult);
        }

        return evaluationResult;
    }

    @Override
    public boolean equals(Filter other) {
        if (!(other instanceof StringFilter)) {
            return false;
        } else {
            StringFilter otherFilter = (StringFilter) other;
            return otherFilter.metadataParameterName.equals(this.metadataParameterName) &&
                    otherFilter.expectedValue.equals(this.expectedValue) &&
                    otherFilter.comparisonResult.equals(this.comparisonResult);
        }
    }

    @Override
    public String toString() {
        return "{expectedValue: " + this.expectedValue +
                ", parameterName: " + this.metadataParameterName +
                ", comparison: " + this.comparisonResult + "}";
    }

    private String getValueForCaseInsensitiveKey(String key, JSONObject jsonObject, String defaultValue) {
        String itemMetadataValue = jsonObject.optString(key, defaultValue);
        if (itemMetadataValue.equals(defaultValue)) {
            itemMetadataValue = jsonObject.optString(key.toLowerCase(), defaultValue);
        }
        return itemMetadataValue;
    }
}
