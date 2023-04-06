package de.biofid.services.crawler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.biofid.services.crawler.BhlHarvester.ItemDoesNotExistException;
import org.apache.commons.io.FileUtils;
import org.apache.http.auth.AuthenticationException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

public class TestBhlHarvester {

    private static final String TEST_OUTPUT_DIRECTORY_STRING = "/tmp/test";
    private boolean didTestDirectoryExistBeforeTest = true;
    private final File testDirectory = Paths.get(TEST_OUTPUT_DIRECTORY_STRING).toFile();

    private static final String ITEM_ARRAY = "items";
    private static final String TITLE_ARRAY = "titles";
    private static final String METADATA_SUBDIRECTORY = "/bhl/metadata/xml";
    private static final String TEXT_SUBDIRECTORY = "/bhl/text";

    @Test
    public void testGetAllCollections() throws IOException {
        DummyConfigurator configurator = setup();
        BhlHarvester bhlHarvesterSpy = prepareMockApiDataAndGetHarvesterSpy(configurator,
                "src/test/resources/bhl/apiResponses/getCollections.json");

        Map<Long, JSONObject> collectionMap = bhlHarvesterSpy.getAllCollections();
        assertEquals(59, collectionMap.size());
    }

    @Test
    public void testGetItemMetadata() throws IOException, AuthenticationException {
        DummyConfigurator configurator = setup();
        Harvester.setOutputDirectory(TEST_OUTPUT_DIRECTORY_STRING);
        int itemId = 22497;

        configurator.addItemToArray(BhlHarvester.BHL_STRING, ITEM_ARRAY, itemId);
        BhlHarvester bhlHarvester = new BhlHarvester(
                configurator.getConfigurationForHarvesterName(BhlHarvester.BHL_STRING));

        JSONObject itemJson = bhlHarvester.getItemMetadata(itemId);
        assertEquals((int) itemJson.get("ItemID"), itemId);
        assertTrue(itemJson.has("Pages"));
        assertTrue(itemJson.has("Parts"));
        assertFalse(itemJson.has("OcrText"));
    }

    @Test
    public void testHarvestSingleElement() throws Exception {
        DummyConfigurator configurator = setup();

        long itemID = 22314;

        configurator.addItemToArray(BhlHarvester.BHL_STRING, ITEM_ARRAY, itemID);

        Harvester.setOutputDirectory(TEST_OUTPUT_DIRECTORY_STRING);
        BhlHarvester bhlHarvester = new BhlHarvester(
                configurator.getConfigurationForHarvesterName(BhlHarvester.BHL_STRING));

        bhlHarvester.run();

        Path expectedTextDirectory = Paths.get(TEST_OUTPUT_DIRECTORY_STRING + TEXT_SUBDIRECTORY);
        Path expectedMetadataDirectory = Paths.get(TEST_OUTPUT_DIRECTORY_STRING + METADATA_SUBDIRECTORY);

        Path expectedMetadataFilePath = expectedMetadataDirectory.resolve(itemID + ".xml");
        Path expectedPdfFilePath = expectedTextDirectory.resolve("pdf/" + itemID + ".pdf");
        Path expectedTxtFilePath = expectedTextDirectory.resolve("txt/" + itemID + ".txt");

        assertTrue(expectedPdfFilePath.toFile().exists());
        assertTrue(expectedMetadataFilePath.toFile().exists());
        assertTrue(expectedTxtFilePath.toFile().exists());
    }

    @Test
    public void testHarvestMultipleElements() throws Exception {
        DummyConfigurator configurator = setup();

        long[] itemIDArray = {22314, 122748};
        configurator.addItemToArray(BhlHarvester.BHL_STRING, ITEM_ARRAY, itemIDArray[0]);
        configurator.addItemToArray(BhlHarvester.BHL_STRING, ITEM_ARRAY, itemIDArray[1]);

        Harvester.setOutputDirectory(TEST_OUTPUT_DIRECTORY_STRING);
        BhlHarvester bhlHarvester = new BhlHarvester(
                configurator.getConfigurationForHarvesterName(BhlHarvester.BHL_STRING));

        bhlHarvester.run();

        Path expectedTextDirectory = Paths.get(TEST_OUTPUT_DIRECTORY_STRING + TEXT_SUBDIRECTORY);
        Path expectedMetadataDirectory = Paths.get(TEST_OUTPUT_DIRECTORY_STRING + METADATA_SUBDIRECTORY);

        for (long itemID : itemIDArray) {
            Path expectedMetadataFilePath = expectedMetadataDirectory.resolve(itemID + ".xml");
            Path expectedPdfFilePath = expectedTextDirectory.resolve("pdf/" + itemID + ".pdf");
            Path expectedTxtFilePath = expectedTextDirectory.resolve("txt/" + itemID + ".txt");

            assertTrue(expectedPdfFilePath.toFile().exists());
            assertTrue(expectedMetadataFilePath.toFile().exists());
            assertTrue(expectedTxtFilePath.toFile().exists());
        }
    }

    @Test
    public void testLoadingTitleListFromFile() throws IOException {
        DummyConfigurator configurator = setup();

        String dummyFilePath = "src/test/resources/listOfTitles.txt";
        configurator.addItemToArray(BhlHarvester.BHL_STRING, TITLE_ARRAY, dummyFilePath);

        Harvester.setOutputDirectory(TEST_OUTPUT_DIRECTORY_STRING);
        BhlHarvester bhlHarvester = new BhlHarvester(
                configurator.getConfigurationForHarvesterName(BhlHarvester.BHL_STRING));

        assertTrue(bhlHarvester.getListOfItems().size() > 599);
    }


    @Test
    public void testLoadingTitleListFromList() throws IOException {
        DummyConfigurator configurator = setup();

        configurator.addItemToArray(BhlHarvester.BHL_STRING, TITLE_ARRAY, "60");
        configurator.addItemToArray(BhlHarvester.BHL_STRING, TITLE_ARRAY, "250");

        Harvester.setOutputDirectory(TEST_OUTPUT_DIRECTORY_STRING);
        BhlHarvester bhlHarvester = new BhlHarvester(
                configurator.getConfigurationForHarvesterName(BhlHarvester.BHL_STRING));

        assertTrue(bhlHarvester.getListOfItems().size() > 160);
    }

    @Test
    public void testHarvestTitles() throws Exception {
        DummyConfigurator configurator = setup();

        long titleID = 155962;
        long[] itemsIncludedInTitles = {261598, 261814};

        Harvester.setOutputDirectory(TEST_OUTPUT_DIRECTORY_STRING);
        BhlHarvester bhlHarvester = new BhlHarvester(
                configurator.getConfigurationForHarvesterName(BhlHarvester.BHL_STRING));

        List<Long> itemsFoundForTitle = bhlHarvester.getItemsFromTitle(titleID);

        for (long itemID : itemsIncludedInTitles) {
            assertTrue(itemsFoundForTitle.contains(itemID));
        }
    }

    @Test
    public void testExternalResourceAccess() throws IOException {
        DummyConfigurator configurator = setup();

        long itemID = 147893;
        configurator.addItemToArray(BhlHarvester.BHL_STRING, ITEM_ARRAY, itemID);

        Harvester.setOutputDirectory(TEST_OUTPUT_DIRECTORY_STRING);
        BhlHarvester bhlHarvester = new BhlHarvester(
                configurator.getConfigurationForHarvesterName(BhlHarvester.BHL_STRING));

        bhlHarvester.run();

        Path expectedTextDirectory = Paths.get(TEST_OUTPUT_DIRECTORY_STRING + TEXT_SUBDIRECTORY);
        Path expectedMetadataDirectory = Paths.get(TEST_OUTPUT_DIRECTORY_STRING + METADATA_SUBDIRECTORY);

        Path expectedMetadataFilePath = expectedMetadataDirectory.resolve(itemID + ".xml");
        Path expectedPdfFilePath = expectedTextDirectory.resolve("pdf/" + itemID + ".pdf");

        assertTrue(expectedPdfFilePath.toFile().exists());
        assertTrue(expectedMetadataFilePath.toFile().exists());
    }

    @Test
    public void testHarvestCollections() throws Exception {
        DummyConfigurator configurator = setup();

        long[] collectionIDArray = {97};
        configurator.addItemToArray(BhlHarvester.BHL_STRING, ITEM_ARRAY, collectionIDArray[0]);
        // TODO: Finish test

    }

    @Test
    public void testItemDoesNotExistException()
            throws IOException {
        DummyConfigurator configurator = setup();
        Harvester.setOutputDirectory(TEST_OUTPUT_DIRECTORY_STRING);
        long itemIdThatDoesNotExist = 1;

        configurator.addItemToArray(BhlHarvester.BHL_STRING, ITEM_ARRAY, itemIdThatDoesNotExist);

        BhlHarvester bhlHarvester = new BhlHarvester(
                configurator.getConfigurationForHarvesterName(BhlHarvester.BHL_STRING));

        assertThrows(ItemDoesNotExistException.class, () -> bhlHarvester.getItemMetadata(itemIdThatDoesNotExist));
    }

    @Test
    public void testInvalidAuthorizationException()
            throws IOException {
        DummyConfigurator configurator = setup();
        Harvester.setOutputDirectory(TEST_OUTPUT_DIRECTORY_STRING);
        long itemIdThatDoesNotExist = 1;

        configurator.addItemToArray(BhlHarvester.BHL_STRING, ITEM_ARRAY, itemIdThatDoesNotExist);

        BhlHarvester bhlHarvester = new BhlHarvester(
                configurator.getConfigurationForHarvesterName(BhlHarvester.BHL_STRING));
        bhlHarvester.setBhlApiKey("th1s-4p1-k3y-1sn0t-v4l1d");
        assertThrows(AuthenticationException.class, () -> bhlHarvester.getItemMetadata(itemIdThatDoesNotExist));
    }

    @Test
    public void testChocrFileIsDownloaded() throws IOException {
        DummyConfigurator configurator = setup();

        int itemID = 100167;
        configurator.addItemToArray(BhlHarvester.BHL_STRING, ITEM_ARRAY, itemID);

        Harvester.setOutputDirectory(TEST_OUTPUT_DIRECTORY_STRING);
        BhlHarvester bhlHarvester = new BhlHarvester(
                configurator.getConfigurationForHarvesterName(BhlHarvester.BHL_STRING));

        bhlHarvester.run();

        Path expectedTextDirectory = Paths.get(TEST_OUTPUT_DIRECTORY_STRING + TEXT_SUBDIRECTORY);
        Path expectedMetadataDirectory = Paths.get(TEST_OUTPUT_DIRECTORY_STRING + METADATA_SUBDIRECTORY);

        Path expectedMetadataFilePath = expectedMetadataDirectory.resolve(itemID + ".xml");
        Path expectedHocrFilePath = expectedTextDirectory.resolve("hocr/" + itemID + ".html");
        Path expectedTxtFilePath = expectedTextDirectory.resolve("txt/" + itemID + ".txt");

        assertTrue(expectedHocrFilePath.toFile().exists());
        assertTrue(expectedMetadataFilePath.toFile().exists());
        assertTrue(expectedTxtFilePath.toFile().exists());
    }

    private DummyConfigurator getConfigurator() throws IOException {
        String configurationFilePathString = "src/test/resources/configurations/test-config.yml";

        DummyConfigurator configurator = new DummyConfigurator();

        // Read Harvester configuration from config file and dump all items and collections
        configurator.readConfigurationYamlFile(configurationFilePathString);
        configurator.removeKeyFromConfiguration(BhlHarvester.BHL_STRING, ITEM_ARRAY);
        configurator.removeKeyFromConfiguration(BhlHarvester.BHL_STRING, TITLE_ARRAY);

        return configurator;
    }

    private DummyConfigurator setup() throws IOException {
        didTestDirectoryExistBeforeTest = testDirectory.exists();

        return getConfigurator();
    }

    private JSONObject loadApiResponse(String filePath) throws IOException {
        File testFilePath = new File(filePath);
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> data = objectMapper.readValue(testFilePath, new TypeReference<Map<String, Object>>() {
        });

        return new JSONObject(data);
    }

    private BhlHarvester prepareMockApiDataAndGetHarvesterSpy(HarvesterConfigurator configurator, String filePath) throws IOException {
        Harvester.setOutputDirectory(TEST_OUTPUT_DIRECTORY_STRING);
        BhlHarvester bhlHarvester = new BhlHarvester(
                configurator.getConfigurationForHarvesterName(BhlHarvester.BHL_STRING));
        JSONObject apiResponse = loadApiResponse(filePath);

        BhlHarvester bhlHarvesterSpy = Mockito.spy(bhlHarvester);
        Mockito.doReturn(apiResponse).when(bhlHarvesterSpy).getFromBhlApi(any());

        return bhlHarvesterSpy;
    }

    @AfterEach
    public void cleanAfterTest() throws IOException {
        if (!didTestDirectoryExistBeforeTest && testDirectory.exists()) {
            FileUtils.deleteDirectory(testDirectory);
        }
    }
}
