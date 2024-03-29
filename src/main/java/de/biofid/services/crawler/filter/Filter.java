package de.biofid.services.crawler.filter;

import de.biofid.services.crawler.Item;
import de.biofid.services.crawler.configuration.FilterConfiguration;

/**
 * A Filter object holds information/conditions if an Item should be removed.
 */
abstract public class Filter {

    protected boolean isStrict;

    Filter(FilterConfiguration configuration) {
        isStrict = configuration.isStrict;
    }

    /**
     * Evaluates an Item object.
     * @param item An Item object to evaluate.
     * @return True, if the Item holds the internal condition or is not set (i.e. null). False otherwise.
     */
    abstract public boolean isItemValid(Item item);
    abstract public boolean equals(Filter other);
    abstract public String toString();
}
