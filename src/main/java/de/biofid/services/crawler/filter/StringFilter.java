package de.biofid.services.crawler.filter;

import de.biofid.services.crawler.Item;

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
        String itemMetadataValue = item.getItemMetadata().optString(metadataParameterName, defaultValue);

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
}
