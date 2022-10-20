package de.biofid.services.crawler;

import de.biofid.services.crawler.metadata.Citation;
import de.biofid.services.crawler.metadata.Metadata;
import de.biofid.services.crawler.metadata.MetadataElement;
import de.biofid.services.crawler.zobodat.ZobodatHarvester;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;


public class TestZobodatHarvester {
	
	private static final String CITATION_AUTHORS = "authors";
	private static final String CITATION_FIRST_PAGE = "firstPage";
	private static final String CITATION_JOURNAL_NAME = "journalName";
	private static final String CITATION_LAST_PAGE = "lastPage";
	private static final String CITATION_TITLE = "title";
	private static final String CITATION_YEAR = "year";
	private static final String CITATION_ISSUE_NUMBER = "issueNumber";
	private static final String CITATION_URL = "url";
	
	private static final String ITEM_ARRAY = "items";
	
	private static final String METADATA_CITATION = "citation";
	private static final String METADATA_PDF_URL = "pdfUrl";
	
	private static final String TEST_OUTPUT_DIRECTORY_STRING = "/tmp/test";
	
	private boolean didTestDirectoryExistBeforeTest = true;
	private File testDirectory = null;

	@Test
	public void testZobodatInstantiation() throws MalformedURLException {
		JSONObject parametersFromConfigFile = new JSONObject(
				"{\"metadata-only\": true, \"overwrite\": true, \"titles\": [1234, 6789]}");

		Configuration configuration = new Configuration(
				"Zobodat",
				"de.biofid.services.crawler.zobodat.ZobodatHarvester",
				parametersFromConfigFile
		);

		LiteratureHarvester literatureHarvester = new LiteratureHarvester();
		Harvester instantiatedHarvester = literatureHarvester.instantiateHarvester(configuration);

		assertNotNull(instantiatedHarvester);
		assertTrue(instantiatedHarvester instanceof ZobodatHarvester);
		assertTrue(instantiatedHarvester.configuration.isOnlyMetadata());
		assertTrue(instantiatedHarvester.configuration.isOverwrittingEnabled());

		Item item = new Item();
		((ZobodatHarvester) instantiatedHarvester).addMetadataToItem(item,
				new Metadata(1234, new URL("https://www.biofid.de"), new Citation()));
		assertTrue(item.shallOnlyMetadataBeSaved());
	}

	@Test
	public void testZobodatInstantiationOnlyForFulltextHarvesting() throws IOException {
		HarvesterConfigurator configurator = getConfigurator("src/test/resources/configurations/zobodat-fulltext-only-config.yml");
		Configuration configuration = configurator.getConfigurationForHarvesterName(ZobodatHarvester.ZOBODAT_STRING);

		LiteratureHarvester literatureHarvester = new LiteratureHarvester();
		ZobodatHarvester instantiatedHarvester = (ZobodatHarvester) literatureHarvester.instantiateHarvester(configuration);

		assertNotNull(instantiatedHarvester);
		assertEquals(21, instantiatedHarvester.getListOfItemsToProcess().size());
	}

	@Test
	public void testFetchingItemListDirectly() throws IOException {
		DummyConfigurator configurator = setup();
		ZobodatHarvester zobodatHarvester = new ZobodatHarvester(
				configurator.getConfigurationForHarvesterName(ZobodatHarvester.ZOBODAT_STRING));
		Document zobodatHtml = loadDocumentHtml("src/test/resources/html/zobodatIndex.html");

		Elements itemList = zobodatHarvester.getItemListFromWebsite(zobodatHtml);
		assertEquals(2, itemList.size());
		
		zobodatHarvester.iterateItems(itemList);
		assertEquals(2, zobodatHarvester.getMetadataListSize());
		
		JSONArray itemMetadataJson = zobodatHarvester.getMetadataListAsJSONArray();
		assertEquals(2, itemMetadataJson.length());
		
		for (int i = 0; i < itemMetadataJson.length(); ++i) {
			JSONObject item = itemMetadataJson.getJSONObject(i);
			areAllMetadataFieldsSerialized(item);
		}
		
		JSONObject item2 = itemMetadataJson.getJSONObject(0);
		JSONObject item2Citation = item2.getJSONObject(METADATA_CITATION);
		JSONArray authors = item2Citation.getJSONArray(CITATION_AUTHORS);
		assertEquals(1, authors.length());
		assertEquals( "Hugo Krüss", authors.getJSONObject(0).get("label"));
		assertEquals(JSONObject.NULL, authors.getJSONObject(0).get("uri"));
		assertEquals(1884, item2Citation.getInt(CITATION_YEAR));
		assertEquals("Eine neue Form des Bunsen-Photometers", item2Citation.get(CITATION_TITLE));
		assertEquals("Abhandlungen aus dem Gebiete der Naturwissenschaften Hamburg", item2Citation.getJSONObject(CITATION_JOURNAL_NAME).get("label"));
		assertEquals("8", item2Citation.getJSONObject(CITATION_ISSUE_NUMBER).get("label"));
		assertEquals("1", item2Citation.get(CITATION_FIRST_PAGE));
		assertEquals("8", item2Citation.get(CITATION_LAST_PAGE));
		assertEquals("https://www.zobodat.at/publikation_articles.php?id=378556", item2.getString(CITATION_URL));
		
		JSONObject item4 = itemMetadataJson.getJSONObject(1);
		JSONObject item4Citation = item4.getJSONObject(METADATA_CITATION);
		authors = item4Citation.getJSONArray(CITATION_AUTHORS);
		assertEquals(1, authors.length());
		assertEquals("Heinrich Gustav Kirchenpauer", authors.getJSONObject(0).get("label"));
		assertEquals("https://www.zobodat.at/personen.php?id=144953", authors.getJSONObject(0).get("uri"));
		assertEquals(1884, item4Citation.getInt(CITATION_YEAR));
		assertEquals("Nordische Gatungen und Arten von Sertulariden", item4Citation.get(CITATION_TITLE));
		assertEquals("Abhandlungen aus dem Gebiete der Naturwissenschaften Hamburg", item4Citation.getJSONObject(CITATION_JOURNAL_NAME).get("label"));
		assertEquals("8", item4Citation.getJSONObject(CITATION_ISSUE_NUMBER).get("label"));
		assertEquals("1", item4Citation.get(CITATION_FIRST_PAGE));
		assertEquals("56", item4Citation.get(CITATION_LAST_PAGE));
	}
	
	@Test
	public void testFetchingItemListsFromJournalOverviewSite() throws IOException {
		// Uses data from https://www.zobodat.at/publikation_series.php?id=20987 ; Accessed 2021-06-10
		DummyConfigurator configurator = setup();
		
		ZobodatHarvester zobodatHarvester = new ZobodatHarvester(
				configurator.getConfigurationForHarvesterName(ZobodatHarvester.ZOBODAT_STRING));
		Document journalOverviewPage = loadDocumentHtml("src/test/resources/html/journalOverviewPage.html");

		Elements itemList = zobodatHarvester.getItemListFromWebsite(journalOverviewPage);

		assertEquals(7, itemList.size());
	}

	@Test
	public void testGetEmptyCitationFromUrl() throws Exception {
		DummyConfigurator configurator = setup();
		String startingUrl = "https://www.zobodat.at/foo";
		Document emptyCitationHtml = loadDocumentHtml("src/test/resources/html/zobodat/citations/emptyCitationContainer.html");

		ZobodatHarvester zobodatHarvester = new ZobodatHarvester(
				configurator.getConfigurationForHarvesterName(ZobodatHarvester.ZOBODAT_STRING));
		ZobodatHarvester zobodatHarvesterSpy = Mockito.spy(zobodatHarvester);
		Mockito.doReturn(emptyCitationHtml).when(zobodatHarvesterSpy).getDocumentFromUrl(startingUrl);

		assertDoesNotThrow(() -> zobodatHarvesterSpy.getCitationFromUrl(new URL(startingUrl)));
	}

	@Test
	public void testMakeItemDownloadable() throws IOException {
		DummyConfigurator configurator = setup();
		configurator.addItemToArray(ZobodatHarvester.ZOBODAT_STRING, "titles", 7392);

		ZobodatHarvester zobodatHarvester = new ZobodatHarvester(
				configurator.getConfigurationForHarvesterName(ZobodatHarvester.ZOBODAT_STRING));

		Item item = new Item();
		item.setSaveMetadataOnly(false);
		Citation citation = new Citation();
		citation.journalName = new MetadataElement("foo", "https://www.zobodat.at/publikation_series.php?id=7392");
		Metadata metadata = new Metadata(1234, new URL("https://www.test.de"), citation);

		assertFalse(item.getSaveMetadataOnly());

		zobodatHarvester.addMetadataToItem(item, metadata);

		assertFalse(item.getSaveMetadataOnly());
	}

	private void areAllMetadataFieldsSerialized(JSONObject item) {
		assertTrue(item.has(METADATA_PDF_URL));
		assertTrue(item.has(METADATA_CITATION));
		JSONObject itemCitation = item.getJSONObject(METADATA_CITATION);
		assertTrue(itemCitation.has(CITATION_AUTHORS));
		assertTrue(itemCitation.has(CITATION_TITLE));
		assertTrue(itemCitation.has(CITATION_YEAR));
		assertTrue(itemCitation.has(CITATION_FIRST_PAGE));
		assertTrue(itemCitation.has(CITATION_LAST_PAGE));
		assertTrue(itemCitation.has(CITATION_JOURNAL_NAME));
	}
	
	@AfterEach
	public void cleanAfterTest() throws IOException {
		if (!didTestDirectoryExistBeforeTest && testDirectory.exists()) {
			FileUtils.deleteDirectory(testDirectory);
		}
	}
	
	private DummyConfigurator getConfigurator() throws IOException {
		String configurationFilePathString = "src/test/resources/configurations/test-config.yml";
		
		DummyConfigurator configurator = new DummyConfigurator();
		
		// Read Harvester configuration from config file and dump all items and collections
		configurator.readConfigurationYamlFile(configurationFilePathString);
		configurator.removeKeyFromConfiguration(ZobodatHarvester.ZOBODAT_STRING, ITEM_ARRAY);
		
		return configurator;
	}
	
	private DummyConfigurator setup() throws IOException {
		Harvester.setOutputDirectory(TEST_OUTPUT_DIRECTORY_STRING);
		
		testDirectory = Paths.get(TEST_OUTPUT_DIRECTORY_STRING).toFile();
		didTestDirectoryExistBeforeTest = testDirectory.exists();
		
		return getConfigurator();
	}

	private Document loadDocumentHtml(String filePath) throws IOException {
		File testFilePath = new File(filePath);
		return Jsoup.parse(testFilePath, "UTF-8");
	}

	private DummyConfigurator getConfigurator(String testConfigFilePath) throws IOException {
		DummyConfigurator configurator = new DummyConfigurator();
		configurator.readConfigurationYamlFile(testConfigFilePath);
		return configurator;
	}
}
