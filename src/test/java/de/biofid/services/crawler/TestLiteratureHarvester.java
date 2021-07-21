package de.biofid.services.crawler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class TestLiteratureHarvester {
	
	private static final String ITEM_ARRAY = "items";
	private static final String TITLE_ARRAY = "titles";
	
	DummyConfigurator configurator = null;

	@Test
	public void testHarvesterInstantiation() {
		Harvester.setOutputDirectory("path/to/output");
		LiteratureHarvester literatureHarvester = new LiteratureHarvester();
		Configuration configuration = configurator.getConfigurationForHarvesterName(BhlHarvester.BHL_STRING);
		Harvester instantiatedHarvester = literatureHarvester.instantiateHarvester(configuration);
		assertEquals("BHL", instantiatedHarvester.getFolderName());
	}
	
	@BeforeEach
	public void setup() throws IOException {
		configurator = getConfigurator();
	}
	
	@AfterEach
	public void cleanup() {
		configurator = null;
	}
	
	
	private DummyConfigurator getConfigurator() throws IOException {
		String configurationFilePathString = "src/test/resources/test-config.yml";
		
		DummyConfigurator configurator = new DummyConfigurator();
		
		// Read Harvester configuration from config file and dump all items and collections
		configurator.readConfigurationYamlFile(configurationFilePathString);
		configurator.removeKeyFromConfiguration(BhlHarvester.BHL_STRING, ITEM_ARRAY);
		configurator.removeKeyFromConfiguration(BhlHarvester.BHL_STRING, TITLE_ARRAY);
		
		return configurator;
	}
}
