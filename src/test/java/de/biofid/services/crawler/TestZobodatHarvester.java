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
	public void testZobodatInstantiation() {
		JSONObject parametersFromConfigFile = new JSONObject("{\"metadata-only\": true, \"titles\": [1234, 6789]}");

		Configuration configuration = new Configuration(
				"Zobodat",
				"de.biofid.services.crawler.zobodat.ZobodatHarvester",
				parametersFromConfigFile
		);

		LiteratureHarvester literatureHarvester = new LiteratureHarvester();
		Harvester instantiatedHarvester = literatureHarvester.instantiateHarvester(configuration);

		assertNotNull(instantiatedHarvester);
		assertTrue(instantiatedHarvester instanceof ZobodatHarvester);
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
	public void testCitationGeneration() throws Exception {
		DummyConfigurator configurator = setup();
		String startingUrl = "https://www.zobodat.at/foo";
		Document emptyCitationHtml = loadDocumentHtml("src/test/resources/html/zobodat/citations/fullCitationPage.html");

		ZobodatHarvester zobodatHarvester = new ZobodatHarvester(
				configurator.getConfigurationForHarvesterName(ZobodatHarvester.ZOBODAT_STRING));
		ZobodatHarvester zobodatHarvesterSpy = Mockito.spy(zobodatHarvester);
		Mockito.doReturn(emptyCitationHtml).when(zobodatHarvesterSpy).getDocumentFromUrl(startingUrl);

		Citation citation = zobodatHarvesterSpy.getCitationFromUrl(new URL(startingUrl));

		assertEquals(7, citation.authors.size());
		assertEquals(new MetadataElement("Herbert Zettel", "https://www.zobodat.at/personen.php?id=348"), citation.authors.get(0));
		assertEquals(new MetadataElement("Herbert Christian Wagner", "https://www.zobodat.at/personen.php?id=53860"), citation.authors.get(1));
		assertEquals(new MetadataElement("Reinhard Franz Seyfert", "https://www.zobodat.at/personen.php?id=9719"), citation.authors.get(6));
		assertEquals("1", citation.firstPage);
		assertEquals("20", citation.lastPage);
		assertEquals("Aculeate Hymenoptera am GEO-Tag der Artenvielfalt 2009 in Pfaffstätten, Niederösterreich.", citation.title);
		assertEquals(2009, citation.year);
		assertEquals(new MetadataElement("Sabulosi", "https://www.zobodat.at/publikation_series.php?id=7392"), citation.journalName);
		assertEquals(new MetadataElement("02", "https://www.zobodat.at/publikation_volumes.php?id=33586"), citation.issueNumber);
	}

	@Test
	public void testCitationGenerationWithNoLinks() throws Exception {
		Citation citation = generateCitationFromZobodatHtmlFile("src/test/resources/html/zobodat/citations/zobodat-citation-with-no-links.html");

		assertEquals(new MetadataElement("Christian Schrenk", null), citation.authors.get(0));
		assertEquals(new MetadataElement("NS187", null), citation.issueNumber);

		// This fails due to bad metadata by the provider!
		//assertEquals(new MetadataElement("Kataloge des OÖ. Landesmuseums N.F.", null), citation.journalName);
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

		assertTrue(item.getSaveMetadataOnly());
	}

	@Test
	public void testRecognizeJournalUrl() throws IOException {
		Citation citation = generateCitationFromZobodatHtmlFile("src/test/resources/html/zobodat/citations/another-citation.html");

		assertEquals(new URL("https://www.zobodat.at/publikation_series.php?id=20832"), citation.journalName.uri);
	}

	private Citation generateCitationFromZobodatHtmlFile(String filePath) throws IOException {
		DummyConfigurator configurator = setup();
		String startingUrl = "https://www.zobodat.at/foo";
		Document noUrlsCitationHtml = loadDocumentHtml(filePath);

		ZobodatHarvester zobodatHarvester = new ZobodatHarvester(
				configurator.getConfigurationForHarvesterName(ZobodatHarvester.ZOBODAT_STRING));
		ZobodatHarvester zobodatHarvesterSpy = Mockito.spy(zobodatHarvester);
		Mockito.doReturn(noUrlsCitationHtml).when(zobodatHarvesterSpy).getDocumentFromUrl(startingUrl);

		return zobodatHarvesterSpy.getCitationFromUrl(new URL(startingUrl));
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
}
