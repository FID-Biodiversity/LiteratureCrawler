package filter;

import de.biofid.services.crawler.*;
import de.biofid.services.crawler.zobodat.ZobodatHarvester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestItemFiltering {
    private static final String ITEM_ARRAY = "items";
    private static final String TITLE_ARRAY = "titles";
    DummyConfigurator configurator = null;
    LiteratureHarvester literatureHarvester = null;

    private static final String TEST_OUTPUT_DIRECTORY_STRING = "/tmp/test";
    private static final String METADATA_SUBDIRECTORY = "/bhl/metadata/xml";
    private static final String TEXT_SUBDIRECTORY = "/bhl/text";


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
        assertTrue(instantiatedHarvester.isFilteredOut(item));

        item = new Item();
        item.addMetdata("Title", "A Story about Testing");
        assertFalse(instantiatedHarvester.isFilteredOut(item));
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
        assertFalse(instantiatedHarvester.isFilteredOut(item));

        item = new Item();
        item.addMetdata("year", 1924);
        assertTrue(instantiatedHarvester.isFilteredOut(item));

        item = new Item();
        item.addMetdata("year", 1923);
        assertFalse(instantiatedHarvester.isFilteredOut(item));

        item = new Item();
        item.addMetdata("year", 1900);
        assertFalse(instantiatedHarvester.isFilteredOut(item));
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
        assertTrue(instantiatedHarvester.isFilteredOut(item));

        item = new Item();
        item.addMetdata("year", "2000");
        assertTrue(instantiatedHarvester.isFilteredOut(item));

        item = new Item();
        item.addMetdata("year", "2023");
        assertFalse(instantiatedHarvester.isFilteredOut(item));
    }

    @Test
    public void testStrictItemFilteringInBhl(@TempDir Path tempDir) throws IOException {
        String configurationFilePath = "src/test/resources/configurations/strict-filter-bhl-config.yml";
        LiteratureHarvester.CONFIGURATION_FILE_PATH_STRING = configurationFilePath;

        DummyConfigurator configurator = new DummyConfigurator();
        configurator.readConfigurationYamlFile(configurationFilePath);

        literatureHarvester = new LiteratureHarvester();
        Harvester bhlHarvester = literatureHarvester.instantiateHarvester(
                configurator.getConfigurationForHarvesterName(BhlHarvester.BHL_STRING)
        );

        String testDirString = tempDir.toString();
        Harvester.setOutputDirectory(testDirString);

        long itemID = 659;

        Path expectedTextDirectory = Paths.get(testDirString + TEXT_SUBDIRECTORY);
        Path expectedMetadataDirectory = Paths.get(testDirString + METADATA_SUBDIRECTORY);

        Path expectedMetadataFilePath = expectedMetadataDirectory.resolve(itemID + ".xml");
        Path expectedPdfFilePath = expectedTextDirectory.resolve("pdf/" + itemID + ".pdf");

        assertFalse(expectedPdfFilePath.toFile().exists());
        assertFalse(expectedMetadataFilePath.toFile().exists());

        bhlHarvester.run();

        assertTrue(expectedPdfFilePath.toFile().exists());
        assertTrue(expectedMetadataFilePath.toFile().exists());
    }

    @Test
    public void testStrictItemFilteringInBotGardenMadrid(@TempDir Path tempDir) throws IOException {
        String configurationFilePath = "src/test/resources/configurations/strict-filter-bot-garden-madrid-config.yml";
        LiteratureHarvester.CONFIGURATION_FILE_PATH_STRING = configurationFilePath;

        DummyConfigurator configurator = new DummyConfigurator();
        configurator.readConfigurationYamlFile(configurationFilePath);

        literatureHarvester = new LiteratureHarvester();
        Harvester bhlHarvester = literatureHarvester.instantiateHarvester(
                configurator.getConfigurationForHarvesterName(BhlHarvester.BHL_STRING)
        );

        String testDirString = tempDir.toString();
        Harvester.setOutputDirectory(testDirString);

        long itemID = 142973;

        Path expectedTextDirectory = Paths.get(testDirString + TEXT_SUBDIRECTORY);
        Path expectedMetadataDirectory = Paths.get(testDirString + METADATA_SUBDIRECTORY);

        Path expectedMetadataFilePath = expectedMetadataDirectory.resolve(itemID + ".xml");
        Path expectedPdfFilePath = expectedTextDirectory.resolve("pdf/" + itemID + ".pdf");

        assertFalse(expectedPdfFilePath.toFile().exists());
        assertFalse(expectedMetadataFilePath.toFile().exists());

        bhlHarvester.run();

        assertTrue(expectedPdfFilePath.toFile().exists());
        assertTrue(expectedMetadataFilePath.toFile().exists());
    }

    @Test
    public void testStrictItemFilteringWithOpenCopyrightStatus(@TempDir Path tempDir) throws IOException {
        String configurationFilePath = "src/test/resources/configurations/strict-filter-bot-garden-madrid-config.yml";
        LiteratureHarvester.CONFIGURATION_FILE_PATH_STRING = configurationFilePath;

        DummyConfigurator configurator = new DummyConfigurator();
        configurator.readConfigurationYamlFile(configurationFilePath);

        literatureHarvester = new LiteratureHarvester();
        BhlHarvester bhlHarvester = (BhlHarvester) literatureHarvester.instantiateHarvester(
                configurator.getConfigurationForHarvesterName(BhlHarvester.BHL_STRING)
        );

        bhlHarvester.clearListOfItems();
        bhlHarvester.addItemId("276258");

        String testDirString = tempDir.toString();
        Harvester.setOutputDirectory(testDirString);

        long itemID = 276258;

        Path expectedTextDirectory = Paths.get(testDirString + TEXT_SUBDIRECTORY);
        Path expectedMetadataDirectory = Paths.get(testDirString + METADATA_SUBDIRECTORY);

        Path expectedMetadataFilePath = expectedMetadataDirectory.resolve(itemID + ".xml");
        Path expectedPdfFilePath = expectedTextDirectory.resolve("pdf/" + itemID + ".pdf");

        assertFalse(expectedPdfFilePath.toFile().exists());
        assertFalse(expectedMetadataFilePath.toFile().exists());

        bhlHarvester.run();

        assertTrue(expectedPdfFilePath.toFile().exists());
        assertTrue(expectedMetadataFilePath.toFile().exists());
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
