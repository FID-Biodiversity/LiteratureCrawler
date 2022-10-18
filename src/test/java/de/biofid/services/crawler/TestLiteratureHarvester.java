package de.biofid.services.crawler;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class TestLiteratureHarvester {
	@TempDir
	public Path testOutputDirectory;
	private static final String ITEM_ARRAY = "items";
	private static final String TITLE_ARRAY = "titles";
	
	DummyConfigurator configurator = null;
	LiteratureHarvester literatureHarvester = null;

	private String testConfigFilePath = "src/test/resources/configurations/test-config.yml";

	@Test
	public void testHarvesterInstantiation() {
		Configuration configuration = configurator.getConfigurationForHarvesterName(BhlHarvester.BHL_STRING);
		Harvester instantiatedHarvester = literatureHarvester.instantiateHarvester(configuration);
		assertEquals("BHL", instantiatedHarvester.getFolderName());
	}

	@Test
	public void testNoFilteredItemIsProcessed() throws IOException {
		testOutputDirectory = Paths.get("/tmp/harvester");

		try {
			Harvester.setOutputDirectory(testOutputDirectory.toString());
			String configurationFilePath = "src/test/resources/configurations/filtered-zobodat-config.yml";
			LiteratureHarvester.CONFIGURATION_FILE_PATH_STRING = configurationFilePath;
			literatureHarvester = new LiteratureHarvester();
			literatureHarvester.start();

			Path fileThatShouldExist = testOutputDirectory.resolve("zobodat/text/pdf/501285.pdf");
			assertTrue(fileThatShouldExist.toFile().exists());

			Path fileThatShouldNotExist = testOutputDirectory.resolve("zobodat/text/pdf/483244.pdf");
			assertFalse(fileThatShouldNotExist.toFile().exists());
		} finally {
			FileUtils.deleteDirectory(testOutputDirectory.toFile());
		}
	}

	@BeforeEach
	public void setup() throws IOException {
		Harvester.setOutputDirectory("path/to/output");
		LiteratureHarvester.CONFIGURATION_FILE_PATH_STRING = testConfigFilePath;
		literatureHarvester = new LiteratureHarvester();

		configurator = getConfigurator(testConfigFilePath);
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
