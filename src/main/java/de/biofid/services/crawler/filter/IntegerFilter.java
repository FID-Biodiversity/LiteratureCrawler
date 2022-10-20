package de.biofid.services.crawler.filter;

import de.biofid.services.crawler.Citation;
import de.biofid.services.crawler.Item;
import de.biofid.services.crawler.configuration.FilterConfiguration;
import org.json.JSONObject;

/**
 * A filter that evaluates Items by criteria that are integers.
 */
public class IntegerFilter extends Filter {

    private final Integer expectedValue;
    private final String metadataParameterName;
    private final ComparisonResult comparisonResult;

    public IntegerFilter(FilterConfiguration filterConfiguration) {
        super(filterConfiguration);

        this.expectedValue = (int) filterConfiguration.expectedValue;
        this.metadataParameterName = filterConfiguration.metadataParameterName;
        this.comparisonResult = filterConfiguration.comparisonResult;
    }

    /**
     * Evaluates an item's metadata and returns a boolean result.
     * The result is true, if the item does not fit the internal parameters. If the requested
     * @param item An Item object to evaluate.
     */
    @Override
    public boolean isItemValid(Item item) {
        int defaultValue = Citation.integerDefault;
        boolean evaluationResult;
        int itemMetadataValue = getValueForCaseInsensitiveKey(
                metadataParameterName, item.getDocumentMetadata(), defaultValue);

        if (itemMetadataValue == defaultValue) {
            // The parameter could not be found! Return the default.
            evaluationResult = !this.isStrict;
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
