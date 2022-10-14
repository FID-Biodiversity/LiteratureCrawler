package de.biofid.services.crawler.filter;

import de.biofid.services.crawler.Item;

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
        int itemMetadataValue = item.getItemMetadata().optInt(metadataParameterName, defaultValue);

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
}
