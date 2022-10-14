package filter;

import de.biofid.services.crawler.BhlHarvester;
import de.biofid.services.crawler.Configuration;
import de.biofid.services.crawler.DummyConfigurator;
import de.biofid.services.crawler.configuration.FilterConfiguration;
import de.biofid.services.crawler.filter.*;
import de.biofid.services.crawler.zobodat.ZobodatHarvester;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class TestFilterFactory {
    @Test
    public void testCreateFilterForHarvesterFromGeneralSettings() throws IOException, FilterFactory.UnsupportedFilterTypeException {
        List<FilterConfiguration> filterConfigurations = getConfiguration(ZobodatHarvester.ZOBODAT_STRING);

        List<Filter> filters = FilterFactory.create(filterConfigurations);

        assertEquals(1, filters.size());

        Filter expectedFilter = new IntegerFilter("year", 1923, ComparisonResult.EQUAL_LESS);
        assertTrue(expectedFilter.equals(filters.get(0)));
    }

    @Test
    public void testCreateFilterForHarvesterWithOverridingGeneralSettings() throws IOException, FilterFactory.UnsupportedFilterTypeException {
        List<FilterConfiguration> filterConfigurations = getConfiguration(BhlHarvester.BHL_STRING);

        List<Filter> filters = FilterFactory.create(filterConfigurations);

        assertEquals(1, filters.size());

        Filter expectedFilter = new StringFilter("title", "test", ComparisonResult.CONTAINS);
        assertTrue(expectedFilter.equals(filters.get(0)));
    }

    private List<FilterConfiguration> getConfiguration(String harvesterName) throws IOException {
        DummyConfigurator configurator = getConfigurator();
        Configuration configuration = configurator.getConfigurationForHarvesterName(harvesterName);
        return configuration.getFilterConfiguration();
    }

    private DummyConfigurator getConfigurator() throws IOException {
        String configurationFilePathString = "src/test/resources/configurations/item-filter-config.yml";

        DummyConfigurator configurator = new DummyConfigurator();

        // Read Harvester configuration from config file and dump all items and collections
        configurator.readConfigurationYamlFile(configurationFilePathString);

        return configurator;
    }
}
