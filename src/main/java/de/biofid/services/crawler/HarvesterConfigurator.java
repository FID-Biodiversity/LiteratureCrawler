package de.biofid.services.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import de.biofid.services.configuration.ConfigurationKeys;
import de.biofid.services.crawler.configuration.FilterConfiguration;
import de.biofid.services.crawler.configuration.FilterConfigurationFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/***
 * A class holding all configurations given in a YAML file.
 * 
 * @author Adrian Pachzelt (University Library Johann Christian Senckenberg, Frankfurt)
 * @author https://www.biofid.de
 * @version 1.0
 */
public class HarvesterConfigurator {

	public static final String GENERAL_SETTINGS = ConfigurationKeys.GENERAL_SETTINGS;
	public static final String HARVESTER_CONFIGURATIONS_PARENT = ConfigurationKeys.HARVESTER_SETTINGS;

	private static final boolean OVERWRITE_DEFAULT = true;
	private static final long REQUEST_DELAY_DEFAULT = 0;
	private static final String LOGGER_LEVEL_DEFAULT = "INFO";
	
	protected Map<String, String> apiKeysForHarvesters = new HashMap<>();
	protected String baseOutputPathString = null;
	protected List<Configuration> configurations = new ArrayList<>();
	protected long delayBetweenRequestsInMilliseconds = REQUEST_DELAY_DEFAULT;
	protected boolean isOverwrittingEnabled = OVERWRITE_DEFAULT;
	protected String loggerLevel = LOGGER_LEVEL_DEFAULT;

	private static final String loggerName = "Configurator";
	private static final Logger logger = LogManager.getLogger(loggerName);
	

	public String getBaseOutputPath() {
		return baseOutputPathString;
	}
	
	/***
	 * Returns the configuration set as defined in the configuration file for the given harvester.
	 * @param harvesterName The harvester name to look up.
	 * @return The configuration of the given harvester. Null, otherwise.
	 */
	public Configuration getConfigurationForHarvesterName(String harvesterName) {
		for (Configuration conf : configurations) {
			if (conf.getHarvesterName().equals(harvesterName)) {
				return conf;
			}
		}
		
		return null;
	}
	
	/***
	 * Returns a deep copy of the configuration list.
	 * @return A list of JSON objects, containing the configurations for the single harvesters.
	 */
	public List<Configuration> getConfigurations() {
		List<Configuration> newList = new ArrayList<>();
		
		for (Configuration config : configurations) {
			newList.add(new Configuration(config));
		}
		
		return newList;
	}
	
	public String getLoggerLevel() {
		return loggerLevel;
	}
	
	/***
	 * Reads a YAML file to configure the Harvesters.
	 * 
	 * @param configurationFileString The file path of the file to read.
	 * @throws IOException
	 */
	@SuppressWarnings("rawtypes")
	public void readConfigurationYamlFile(String configurationFileString) throws IOException {
		File configurationFile = Paths.get(configurationFileString).toFile();

		logger.info("Reading configuration from file " + configurationFile.getAbsolutePath());
		
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		JsonNode configurationJson = mapper.readTree(configurationFile);
		
		JsonNode generalSettingsTree = configurationJson.get(GENERAL_SETTINGS);
		baseOutputPathString = generalSettingsTree.get(ConfigurationKeys.OUTPUT_PATH).asText();
		delayBetweenRequestsInMilliseconds = generalSettingsTree.get(ConfigurationKeys.REQUEST_DELAY).asLong();
		
		if (generalSettingsTree.has(ConfigurationKeys.LOGGER_LEVEL)) {
			loggerLevel = generalSettingsTree.get(ConfigurationKeys.LOGGER_LEVEL).asText().toLowerCase();
		}
		
		if (generalSettingsTree.has(ConfigurationKeys.OVERWRITE_FILES)) {
			isOverwrittingEnabled = generalSettingsTree.get(ConfigurationKeys.OVERWRITE_FILES).asBoolean();
		}
		
		JsonNode harvesterConfigurationTree = configurationJson.get(HARVESTER_CONFIGURATIONS_PARENT);
		
		@SuppressWarnings("unchecked")
		List<LinkedHashMap> harvesterConfigurationList = new ObjectMapper().convertValue(
				harvesterConfigurationTree, ArrayList.class);

		FilterConfigurationFactory filterConfigurationFactory = setupFilterConfigurationFactory(configurationJson);

		String configurationFileDirectoryPathString = configurationFile.getParent();
		for (LinkedHashMap harvesterConfiguration : harvesterConfigurationList) {
			String harvesterName = (String) harvesterConfiguration.keySet().toArray()[0];

			JSONObject jsonConfiguration = new JSONArray(harvesterConfiguration.values()).getJSONObject(0);
			Configuration config =
					createHarvesterConfigurations(
							harvesterName, jsonConfiguration, configurationFileDirectoryPathString);

			List<FilterConfiguration> filterConfigurations =
					filterConfigurationFactory.getFilterConfigurationsForHarvesterName(config.getHarvesterName());
			config.setFilterConfigurations(filterConfigurations);

			this.configurations.add(config);
		}
	}

	private Configuration createHarvesterConfigurations(String harvesterName, JSONObject harvesterConfiguration,
											   String configurationFileDirectoryPathString) throws IOException {
		String harvesterClassName = harvesterConfiguration.getString(ConfigurationKeys.CLASS_NAME);

		String apiKey = getApiKey(harvesterConfiguration);
		if (harvesterConfiguration.has(ConfigurationKeys.API_KEY)) {
			apiKeysForHarvesters.put(harvesterName, apiKey);
		}

		readItemsAndTitlesToArrays(harvesterConfiguration, configurationFileDirectoryPathString);

		setDefaultConfigurationIfParameterIsNotPresent(harvesterConfiguration);

		Configuration config = new Configuration(harvesterName, harvesterClassName, harvesterConfiguration);
		config.setHarvesterApiKey(apiKey);

		return config;
	}

	/**
	 * If for either the keyword title or item a single string (not a list) is given,
	 * this string will be interpreted as a file path and the content of the file will be read.
	 * The configuration is updated in-place.
	 * @param jsonConfiguration The Harvester configuration as JSON.
	 * @throws IOException
	 */
	private void readItemsAndTitlesToArrays(JSONObject jsonConfiguration, String configurationFileDirectoryPathString) throws IOException {
		Path configurationFilePath = Paths.get(configurationFileDirectoryPathString);
		updateConfigurationParametersWithFileContents(ConfigurationKeys.TITLES, jsonConfiguration, configurationFilePath);
		updateConfigurationParametersWithFileContents(ConfigurationKeys.ITEMS, jsonConfiguration, configurationFilePath);
	}

	private void updateConfigurationParametersWithFileContents(String parameterKey, JSONObject jsonConfiguration,
															   Path configurationFilePath) throws IOException {
		if (jsonConfiguration.has(parameterKey)) {
			Object ids = readContentFromConfigurationKey(parameterKey,
					jsonConfiguration, configurationFilePath);
			jsonConfiguration.put(parameterKey, ids);
		}
	}

	private JSONArray readContentFromConfigurationKey(String parameterKey,
														 JSONObject jsonConfiguration,
														 Path configurationFilePath) throws IOException {
		String parameterValue;
		try {
			parameterValue = jsonConfiguration.getString(parameterKey);
		} catch (JSONException e) {
			return jsonConfiguration.getJSONArray(parameterKey);
		}

		Path filePath = Paths.get(parameterValue);
		if (!filePath.isAbsolute()) {
			filePath = configurationFilePath.resolve(filePath);
		}

		String fileContent = Files.readString(filePath);
		return new JSONArray(fileContent.split("\n"));
	}

	private FilterConfigurationFactory setupFilterConfigurationFactory(JsonNode configuration) {
		FilterConfigurationFactory filterConfigurationFactory = new FilterConfigurationFactory();

		if (configuration.has(ConfigurationKeys.ITEM_FILTER_CONFIGURATIONS)) {
			logger.info("Found Item Filter criteria!");

			JsonNode itemFilterConfiguration = configuration.get(ConfigurationKeys.ITEM_FILTER_CONFIGURATIONS);

			HashMap configurationList = new ObjectMapper().convertValue(
					itemFilterConfiguration, HashMap.class);

			logger.info("Processing Item Filter criteria...");
			filterConfigurationFactory.parseFilterConfigurations(new JSONObject(configurationList));
		}

		return filterConfigurationFactory;
	}
	
	private String getApiKey(JSONObject jsonConfiguration) {
		String apiKeyString = jsonConfiguration.optString(ConfigurationKeys.API_KEY, null);
		String apiKey;
    	if (FileHandler.isStringPathOrFile(apiKeyString)) {
    		apiKey = readApiKey(apiKeyString);
    	} else {
    		apiKey = apiKeyString;
    	}
    	return apiKey;
	}

	private String readApiKey(String apiSourceFile) {
		return FileHandler.getFileContent(apiSourceFile);
    }

    private void setDefaultConfigurationIfParameterIsNotPresent(JSONObject configuration) {
		setDefaultIfNotPresent(configuration, ConfigurationKeys.OVERWRITE_FILES, OVERWRITE_DEFAULT);
		setDefaultIfNotPresent(configuration, ConfigurationKeys.REQUEST_DELAY, REQUEST_DELAY_DEFAULT);
	}

	private void setDefaultIfNotPresent(JSONObject configuration, String key, Object defaultValue) {
		if (!configuration.has(key)) {
			configuration.put(key, defaultValue);
		}
	}
}
