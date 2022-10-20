package de.biofid.services.crawler.zobodat;

import de.biofid.services.crawler.DummyConfigurator;
import de.biofid.services.crawler.Harvester;
import de.biofid.services.crawler.metadata.Citation;
import de.biofid.services.crawler.metadata.MetadataElement;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCitationGenerator {

    private static final String TEST_OUTPUT_DIRECTORY_STRING = "/tmp/test";
    private static final String ITEM_ARRAY = "items";

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

    /**
     * Publication year recognition, when the citation looks like "Smith, J. (1998/1999)"
     */
    @Test
    public void testCitationGenerationWithSlashedPublicationYear() throws Exception {
        Citation citation = generateCitationFromZobodatHtmlFile("src/test/resources/html/zobodat/citations/slashed-publication-year.html");

        assertEquals(new MetadataElement("Konrad Thaler", null), citation.authors.get(0));
        assertEquals(new MetadataElement("Barbara Knoflach-Thaler", null), citation.authors.get(1));
        assertEquals(1999, citation.year);
    }

    /**
     * Publication year recognition, when the citation looks like "Smith, J. (1998/99)"
     */
    @Test
    public void testCitationGenerationWithSlashedFractionalPublicationYear() throws Exception {
        Citation citation = generateCitationFromZobodatHtmlFile("src/test/resources/html/zobodat/citations/slashed-fractional-publication-year.html");

        assertEquals(new MetadataElement("Konrad Thaler", null), citation.authors.get(0));
        assertEquals(new MetadataElement("Barbara Knoflach-Thaler", null), citation.authors.get(1));
        assertEquals(1904, citation.year);
    }

    @Test
    public void testRecognizeJournalUrl() throws IOException {
        Citation citation = generateCitationFromZobodatHtmlFile("src/test/resources/html/zobodat/citations/another-citation.html");

        assertEquals(new URL("https://www.zobodat.at/publikation_series.php?id=20832"), citation.journalName.uri);
    }

    private Document loadDocumentHtml(String filePath) throws IOException {
        File testFilePath = new File(filePath);
        return Jsoup.parse(testFilePath, "UTF-8");
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

    private DummyConfigurator setup() throws IOException {
        Harvester.setOutputDirectory(TEST_OUTPUT_DIRECTORY_STRING);

        return getConfigurator();
    }

    private DummyConfigurator getConfigurator() throws IOException {
        String configurationFilePathString = "src/test/resources/configurations/test-config.yml";

        DummyConfigurator configurator = new DummyConfigurator();

        // Read Harvester configuration from config file and dump all items and collections
        configurator.readConfigurationYamlFile(configurationFilePathString);
        configurator.removeKeyFromConfiguration(ZobodatHarvester.ZOBODAT_STRING, ITEM_ARRAY);

        return configurator;
    }
}
