package filter;

import de.biofid.services.crawler.*;
import de.biofid.services.crawler.zobodat.ZobodatHarvester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestItemFiltering {
    private static final String ITEM_ARRAY = "items";
    private static final String TITLE_ARRAY = "titles";
    DummyConfigurator configurator = null;
    LiteratureHarvester literatureHarvester = null;


    @Test
    public void testItemFilteringWithStringFilter() throws IOException {
        Harvester.setOutputDirectory("path/to/output");
        String configurationFilePath = "src/test/resources/configurations/item-filter-config.yml";
        LiteratureHarvester.CONFIGURATION_FILE_PATH_STRING = configurationFilePath;
        literatureHarvester = new LiteratureHarvester();

        configurator = getConfigurator(configurationFilePath);
        Configuration configuration = configurator.getConfigurationForHarvesterName(BhlHarvester.BHL_STRING);
        Harvester instantiatedHarvester = literatureHarvester.instantiateHarvester(configuration);

        Item item = new Item();
        item.addMetdata("Title", "About Birds!");
        assertFalse(instantiatedHarvester.isItemValid(item));

        item = new Item();
        item.addMetdata("Title", "A Story about Testing");
        assertTrue(instantiatedHarvester.isItemValid(item));
    }

    @Test
    public void testItemFilteringWithIntegerFilter() throws IOException {
        Harvester.setOutputDirectory("path/to/output");
        String configurationFilePath = "src/test/resources/configurations/item-filter-config.yml";
        LiteratureHarvester.CONFIGURATION_FILE_PATH_STRING = configurationFilePath;
        literatureHarvester = new LiteratureHarvester();

        configurator = getConfigurator(configurationFilePath);
        Configuration configuration = configurator.getConfigurationForHarvesterName(ZobodatHarvester.ZOBODAT_STRING);
        Harvester instantiatedHarvester = literatureHarvester.instantiateHarvester(configuration);

        Item item = new Item();
        // Default: The item passes, if the requested metadata is not set.
        assertTrue(instantiatedHarvester.isItemValid(item));

        item = new Item();
        item.addMetdata("year", 1924);
        assertFalse(instantiatedHarvester.isItemValid(item));

        item = new Item();
        item.addMetdata("year", 1923);
        assertTrue(instantiatedHarvester.isItemValid(item));

        item = new Item();
        item.addMetdata("year", 1900);
        assertTrue(instantiatedHarvester.isItemValid(item));
    }

    @Test
    public void testStrictItemFilteringWithIntegerFilter() throws IOException {
        Harvester.setOutputDirectory("path/to/output");
        String configurationFilePath = "src/test/resources/configurations/strict-filter-zobodat-config.yml";
        LiteratureHarvester.CONFIGURATION_FILE_PATH_STRING = configurationFilePath;
        literatureHarvester = new LiteratureHarvester();

        configurator = getConfigurator(configurationFilePath);
        Configuration configuration = configurator.getConfigurationForHarvesterName(ZobodatHarvester.ZOBODAT_STRING);
        Harvester instantiatedHarvester = literatureHarvester.instantiateHarvester(configuration);

        Item item = new Item();
        // When the filter is set "strict: true", the metadata has to exist to be passed.
        assertFalse(instantiatedHarvester.isItemValid(item));
    }

    @AfterEach
    public void cleanup() {
        configurator = null;
    }


    private DummyConfigurator getConfigurator(String configurationFilePathString) throws IOException {
        DummyConfigurator configurator = new DummyConfigurator();

        // Read Harvester configuration from config file and dump all items and collections
        configurator.readConfigurationYamlFile(configurationFilePathString);
        configurator.removeKeyFromConfiguration(BhlHarvester.BHL_STRING, ITEM_ARRAY);
        configurator.removeKeyFromConfiguration(BhlHarvester.BHL_STRING, TITLE_ARRAY);

        return configurator;
    }
}
