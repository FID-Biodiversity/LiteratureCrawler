package de.biofid.services.crawler;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestConfiguration {
    @Test
    public void testConstructor() {
        JSONObject parametersFromConfigFile = new JSONObject(
                "{\"metadata-only\": true, \"overwrite\": false, \"titles\": [1234, 6789]}");

        Configuration configuration = new Configuration(
                "Zobodat",
                "de.biofid.services.crawler.zobodat.ZobodatHarvester",
                parametersFromConfigFile
        );

        assertTrue(configuration.isOnlyMetadata());
        assertFalse(configuration.isOverwrittingEnabled());
    }
}
