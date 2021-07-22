package de.biofid.services.crawler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestHarvestConfigurator {

    private static final String TEST_CONFIGURATION = "src/test/resources/configurations/test-harvest-configurator.yml";

    private HarvesterConfigurator configurator;

    @Test
    public void testReadMetadataOnlyFromConfiguration() throws IOException {
        configurator.readConfigurationYamlFile(TEST_CONFIGURATION);
        Configuration zobodatConfiguration = configurator.getConfigurationForHarvesterName("Zobodat");
        assertTrue(zobodatConfiguration.isOnlyMetadata());

        Configuration bhlConfiguration = configurator.getConfigurationForHarvesterName("BHL");
        assertFalse(bhlConfiguration.isOnlyMetadata());
    }

    @Test
    public void testGetConfigurations() throws IOException {
        configurator.readConfigurationYamlFile(TEST_CONFIGURATION);
        List<Configuration> configurations = configurator.getConfigurations();

        Configuration bhlConfiguration = configurations.get(0);
        assertEquals("BHL", bhlConfiguration.getHarvesterName());
        assertFalse(bhlConfiguration.isOnlyMetadata());
        assertTrue(bhlConfiguration.isOverwrittingEnabled());

        Configuration zobodatConfiguration = configurations.get(1);
        assertEquals("Zobodat", zobodatConfiguration.getHarvesterName());
        assertTrue(zobodatConfiguration.isOnlyMetadata());
        assertFalse(zobodatConfiguration.isOverwrittingEnabled());
    }

    @BeforeEach
    public void setup() {
        configurator = new HarvesterConfigurator();
    }
}
