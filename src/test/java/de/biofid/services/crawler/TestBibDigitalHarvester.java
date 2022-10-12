package de.biofid.services.crawler;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.biofid.services.crawler.Harvester.UnsetHarvesterBaseDirectoryException;

public class TestBibDigitalHarvester {
	
	private static final String TEST_OUTPUT_DIRECTORY_STRING = "/tmp/test";
	private static final String HARVESTER_NAME = BibDigitalHarvester.BIB_DIGITAL_HARVESTER;
	private static final String HARVESTER_CLASS_PATH = BibDigitalHarvester.class.getPackage().toString();
	
	@SuppressWarnings("unused")
	private DummyConfigurator dummyConfigurator = null;
	private File testDirectory = null;
	
	private boolean didTestDirectoryExistBeforeTest = true;
	private Configuration defaultConfiguration = null;

	@Test
	public void testSingleItemDownload() throws UnsetHarvesterBaseDirectoryException, MalformedURLException {
		BibDigitalHarvester harvester = new BibDigitalHarvester(defaultConfiguration);
		harvester.addItemToCollect(new URL("https://bibdigital.rjb.csic.es/idurl/1/15729"));
		harvester.run();
		
		File outputTextFile = Paths.get(TEST_OUTPUT_DIRECTORY_STRING + "/" + harvester.getFolderName() + 
				"/text/pdf/15729.pdf").toFile();
		File outputMetadataFile = Paths.get(TEST_OUTPUT_DIRECTORY_STRING + "/" + harvester.getFolderName() + 
				"/metadata/xml/15729.xml").toFile();

		// Currently (2022-10-12) the Bot. Garden of Madrid does not provide PDFs. If this is fixed,
		// the comments below can be uncommented.
		//assertTrue(outputTextFile.exists());
		assertFalse(outputTextFile.exists());
		assertTrue(outputMetadataFile.exists());
	}
	
	@BeforeEach
	public void setup() {
		Harvester.setOutputDirectory(TEST_OUTPUT_DIRECTORY_STRING);
		
		dummyConfigurator = new DummyConfigurator();
		
		testDirectory = Paths.get(TEST_OUTPUT_DIRECTORY_STRING).toFile();
		didTestDirectoryExistBeforeTest = testDirectory.exists();
		
		defaultConfiguration = new Configuration(HARVESTER_NAME, HARVESTER_CLASS_PATH, new JSONObject());
	}
	
	@AfterEach
	public void cleanUp() {
		dummyConfigurator = null;
		if (!didTestDirectoryExistBeforeTest && testDirectory.exists()) {
			try {
				FileUtils.deleteDirectory(testDirectory);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
