package de.biofid.services.crawler.configuration;

import de.biofid.services.configuration.ConfigurationKeys;
import de.biofid.services.crawler.filter.ComparisonResult;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Digests the user's filter configuration and inferences the {@link FilterConfiguration} for each configured
 * {@link de.biofid.services.crawler.Harvester}. The {@link FilterConfiguration} for the respective
 * {@link de.biofid.services.crawler.Harvester} can be retrieved by the Harvester's name.
 */
public class FilterConfigurationFactory {
    private final HashMap<String, List<FilterConfiguration>> filterConfigurations = new HashMap<>();
    public void parseFilterConfigurations(JSONObject filterConfigurations) {
        JSONObject generalSettings = (JSONObject) filterConfigurations.remove(ConfigurationKeys.GENERAL_SETTINGS);

        if (generalSettings != null) {
            List<FilterConfiguration> generalFilterConfigurations = createHarvesterFilterConfiguration(generalSettings);
            this.filterConfigurations.put(ConfigurationKeys.GENERAL_SETTINGS, generalFilterConfigurations);
        }

        for (Iterator<String> it = filterConfigurations.keys(); it.hasNext(); ) {
            String harvesterKeyName = it.next();
            JSONObject configuration = filterConfigurations.getJSONObject(harvesterKeyName);
            List<FilterConfiguration> configurations = createHarvesterFilterConfiguration(configuration);
            this.filterConfigurations.put(harvesterKeyName, configurations);
        }
    }

    /**
     * Allows to access the setup {@link FilterConfiguration}s for the name of a {@link de.biofid.services.crawler.Harvester}.
     * If no {@link FilterConfiguration} for the given {@link de.biofid.services.crawler.Harvester} name exists, the
     * {@link FilterConfiguration} that was set under "General" will be returned. If this was not configured either,
     * an empty list will be returned.
     * @param harvesterName The name of a {@link de.biofid.services.crawler.Harvester}.
     * @return A list of {@link FilterConfiguration}s.
     */
    public List<FilterConfiguration> getFilterConfigurationsForHarvesterName(String harvesterName) {
        List<FilterConfiguration> defaultValue = this.filterConfigurations.getOrDefault(
                ConfigurationKeys.GENERAL_SETTINGS, new ArrayList<>()
        );
        return this.filterConfigurations.getOrDefault(harvesterName, defaultValue);
    }

    private List<FilterConfiguration> createHarvesterFilterConfiguration(JSONObject configuration) {
        List<FilterConfiguration> filterConfigurations = new ArrayList<>();

        for (Iterator<String> it = configuration.keys(); it.hasNext(); ) {
            String parameterName = it.next();
            JSONObject condition = configuration.getJSONObject(parameterName);
            String conditionName = condition.keys().next();
            Object expectedValue = condition.get(conditionName);
            Object valueType = expectedValue.getClass();
            ComparisonResult comparisonResult = ComparisonResult.fromString(conditionName);

            FilterConfiguration filterConfiguration = new FilterConfiguration(
                    valueType, parameterName, expectedValue, comparisonResult
            );

            filterConfigurations.add(filterConfiguration);
        }

        return filterConfigurations;
    }
}
