package de.biofid.services.crawler.filter;

import de.biofid.services.crawler.Item;
import org.json.JSONObject;

/**
 * A filter that evaluates Items by criteria that are integers.
 */
public class IntegerFilter extends Filter {

    private final Integer expectedValue;
    private final String metadataParameterName;
    private final ComparisonResult comparisonResult;

    public IntegerFilter(String metadataParameterName, int expectedValue, ComparisonResult comparisonResult) {
        this.expectedValue = expectedValue;
        this.metadataParameterName = metadataParameterName;
        this.comparisonResult = comparisonResult;
    }

    @Override
    public boolean isItemValid(Item item) {
        int defaultValue = -99999;
        boolean evaluationResult;
        int itemMetadataValue = getValueForCaseInsensitiveKey(
                metadataParameterName, item.getDocumentMetadata(), defaultValue);

        if (itemMetadataValue == defaultValue) {
            evaluationResult = true;
        } else {
            Integer itemMetadataInteger = itemMetadataValue;
            int numericResult = itemMetadataInteger.compareTo(expectedValue);
            evaluationResult = comparisonResult.equals(numericResult);
        }

        return evaluationResult;
    }

    @Override
    public boolean equals(Filter other) {
        if (!(other instanceof IntegerFilter)) {
            return false;
        } else {
            IntegerFilter otherFilter = (IntegerFilter) other;
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

    private int getValueForCaseInsensitiveKey(String key, JSONObject jsonObject, int defaultValue) {
        int itemMetadataValue = jsonObject.optInt(key, defaultValue);
        if (itemMetadataValue == defaultValue) {
            itemMetadataValue = jsonObject.optInt(key.toLowerCase(), defaultValue);
        }
        return itemMetadataValue;
    }
}
