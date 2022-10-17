package de.biofid.services.crawler;

import de.biofid.services.configuration.ConfigurationKeys;
import de.biofid.services.crawler.configuration.FilterConfiguration;
import org.json.JSONObject;

import java.util.List;

/***
 * A single configuration holding all necessary settings for a single {@link Harvester}.
 *
 * @author Adrian Pachzelt (University Library Johann Christian Senckenberg, Frankfurt)
 * @version 1.0
 */
public class Configuration {

	private static final boolean OVERWRITE_FILES_DEFAULT = true;
	private static final boolean ONLY_METADATA_DEFAULT = false;
	private static final int REQUEST_DELAY_DEFAULT = 0;

	private String apiKey = null;
	private long delayBetweenRequestsInMilliseconds = 0;
	private String harvesterClassName;
	private String harvesterName;
	private boolean isOverwrittingEnabled = OVERWRITE_FILES_DEFAULT;
	private boolean onlyMetadata = ONLY_METADATA_DEFAULT;
	private List<FilterConfiguration> filterConfigurations;
	private JSONObject jsonConfiguration;
	
	public Configuration(Configuration conf) {
		this.harvesterName = conf.harvesterName;
		this.harvesterClassName = conf.harvesterClassName;
		this.jsonConfiguration = new JSONObject(conf.jsonConfiguration.toString());
		this.apiKey = conf.apiKey;
		this.isOverwrittingEnabled = conf.isOverwrittingEnabled();
		this.delayBetweenRequestsInMilliseconds = conf.getRequestDelay();
		this.onlyMetadata = conf.onlyMetadata;
	}
	
	public Configuration(String harvesterName, String harvesterClassName, JSONObject jsonConfiguration) {
		this.harvesterName = harvesterName;
		this.harvesterClassName = harvesterClassName;
		this.jsonConfiguration = jsonConfiguration;

		setupConfigurationsFromJson(jsonConfiguration);
	}
	
	public String getHarvesterApiKey() {
		return apiKey;
	}
	
	public String getHarvesterClassName() {
		return harvesterClassName;
	}
	
	public JSONObject getHarvesterJsonConfiguration() {
		return jsonConfiguration;
	}
	
	public String getHarvesterName() {
		return harvesterName;
	}
	
	public long getRequestDelay() {
		return delayBetweenRequestsInMilliseconds;
	}
	
	public boolean isOverwrittingEnabled() {
		return isOverwrittingEnabled;
	}

	public boolean isOnlyMetadata() {
		return onlyMetadata;
	}

	public List<FilterConfiguration> getFilterConfiguration() {
		return filterConfigurations;
	}
	
	public void setHarvesterApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
	
	public void setOverwritting(boolean isOverwrittingEnabled) {
		this.isOverwrittingEnabled = isOverwrittingEnabled;
	}
	
	public void setRequestDelay(long delayInMilliseconds) {
		this.delayBetweenRequestsInMilliseconds = delayInMilliseconds;
	}

	public void setOnlyMetadata(boolean onlyMetadata) {
		this.onlyMetadata = onlyMetadata;
	}

	public void setFilterConfigurations(List<FilterConfiguration> filterConfigurations) {
		this.filterConfigurations = filterConfigurations;
	}

	private void setupConfigurationsFromJson(JSONObject jsonConfiguration) {
		setHarvesterApiKey(jsonConfiguration.optString(ConfigurationKeys.API_KEY));
		setOnlyMetadata(jsonConfiguration.optBoolean(ConfigurationKeys.METADATA_ONLY, ONLY_METADATA_DEFAULT));
		setOverwritting(jsonConfiguration.optBoolean(ConfigurationKeys.OVERWRITE_FILES, OVERWRITE_FILES_DEFAULT));
		setRequestDelay(jsonConfiguration.optInt(ConfigurationKeys.REQUEST_DELAY, REQUEST_DELAY_DEFAULT));
	}

	@Override
	public String toString() {
		return "{" +
				"apiKey='" + apiKey + '\'' +
				", delayBetweenRequestsInMilliseconds=" + delayBetweenRequestsInMilliseconds +
				", harvesterClassName='" + harvesterClassName + '\'' +
				", harvesterName='" + harvesterName + '\'' +
				", isOverwrittingEnabled=" + isOverwrittingEnabled +
				", onlyMetadata=" + onlyMetadata +
				", jsonConfiguration=" + jsonConfiguration +
				", filters=" + filterConfigurations +
				'}';
	}
}
