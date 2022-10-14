package de.biofid.services.crawler.filter;

import de.biofid.services.crawler.configuration.FilterConfiguration;

import javax.lang.model.type.UnknownTypeException;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds Filter objects according to the given configurations.
 */
public class FilterFactory {
    public static List<Filter> create(List<FilterConfiguration> configurations) throws UnsupportedFilterTypeException {
        if (configurations == null) {
            return new ArrayList<>();
        }

        List<Filter> filters = new ArrayList<>();
        for (FilterConfiguration configuration : configurations) {
            Filter filter;
            String typeName = ((Class<?>) configuration.valueType).getName();

            if (typeName.contains("Integer")) {
                filter = createIntegerFilter(configuration);
            } else if (typeName.contains("String")) {
                filter = createStringFilter(configuration);
            } else {
                throw new UnsupportedFilterTypeException("The given filter type" + typeName + "is not implemented!");
            }

            filters.add(filter);
        }

        return filters;
    }

    private static Filter createIntegerFilter(FilterConfiguration configuration) {
        return new IntegerFilter(
                configuration.metadataParameterName,
                (int) configuration.expectedValue,
                configuration.comparisonResult
        );
    }

    private static Filter createStringFilter(FilterConfiguration configuration) {
        return new StringFilter(
                configuration.metadataParameterName,
                (String) configuration.expectedValue,
                configuration.comparisonResult
        );
    }

    public static class UnsupportedFilterTypeException extends Exception {
        public UnsupportedFilterTypeException(String str) {
            super(str);
        }
    }

}
