package de.biofid.services.crawler.configuration;

import de.biofid.services.crawler.filter.ComparisonResult;

/**
 * A messenger class, holding all information for creating a {@link de.biofid.services.crawler.filter.Filter}.
 */
public class FilterConfiguration {
    public Object valueType;
    public Object expectedValue;
    public String metadataParameterName;
    public ComparisonResult comparisonResult;
    public boolean isStrict;

    public FilterConfiguration(Object valueType, String metadataParameterName, Object expectedValue,
                        ComparisonResult comparisonResult, boolean isStrict) {
        this.valueType = valueType;
        this.expectedValue = expectedValue;
        this.metadataParameterName = metadataParameterName;
        this.comparisonResult = comparisonResult;
        this.isStrict = isStrict;
    }

    public String toString() {
        return "Filter for valueType '" + valueType + "'" +
                ", expectedValue: " + expectedValue +
                ", metadataParameterName: " + metadataParameterName +
                ", comparisonResult: " + comparisonResult +
                ", isStrict: " + isStrict;
    }
}
