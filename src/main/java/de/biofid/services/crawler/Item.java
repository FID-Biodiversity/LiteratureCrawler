package de.biofid.services.crawler;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.texttechnologylab.utilities.helper.FileUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/***
 * An item holds all metadata of a single book or monography.
 * This class is responsible for downloading and saving the item.
 * 
 * @author Adrian Pachzelt (University Library Johann Christian Senckenberg, Frankfurt)
 * @author https://www.biofid.de
 * @version 1.0
 */
public class Item {
	
	public enum ItemType {
		ARTICLE, BOOK
	}
	
	public enum FileType {
		PDF {
			@Override
			public String getFileSuffix() {
				return PDF.toString();
			}
		}, 
		TXT {
			@Override
			public String getFileSuffix() {
				return TXT.toString();
			}
		}, 
		XML {
			@Override
			public String getFileSuffix() {
				return XML.toString();
			}
		}, 
		JSON {
			@Override
			public String getFileSuffix() {
				return JSON.toString();
			}
		}, 
		ABBYY {
			@Override
			public String getFileSuffix() {
				return "gz";
			}
		},
		CHOCR {
			@Override
			public String getFileSuffix() {
				return "html.gz";
			}
		};
		
		public abstract String getFileSuffix();
	}

	public enum CopyrightStatus {
		NOT_IN_COPYRIGHT,
		IN_COPYRIGHT,
		CREATIVE_COMMONS_LICENSE
	}
	
	public static String TEXT_OUTPUT_FOLDER_NAME = "text";
	public static String METADATA_OUTPUT_FOLDER_NAME = "metadata";
	
	public static String METADATA_ROOT_NAME = "Metadata";
	public static String METADATA_ITEM_ID_STRING = "itemID";
	public static String METADATA_URL_STRING = "Url";
	public static String METADATA_ITEM_TEXT_URLS_PARENT_STRING = "TextUrls";
	public static String METADATA_ITEM_TEXT_URL_STRING = "TextUrl";
	public static String METADATA_ITEM_TEXT_FILE_TYPE_STRING = "Filetype";
	public static String METADATA_ITEM_SOURCE_STRING = "Source";
	
	public static final int JSON_N_SPACES_FOR_INDENTATION = 2;
	
	private static final Logger logger = LogManager.getLogger(LiteratureHarvester.LOGGER_NAME);
		
	private long itemID = -1;
	private String dataSource = "";
	private URL itemUrl;
	private ArrayList<URL> textFileUrls = new ArrayList<>();
	private ArrayList<FileType> textFileTypes = new ArrayList<>();
	private JSONObject itemMetadata = new JSONObject();
	private JSONObject documentMetadata = itemMetadata;
	private HashSet<String> createdTextFiles = new HashSet<>();
	private boolean saveMetadataOnly = true;

	/***
	 * Add a new key with an object to the metadata.
	 */
	public void addMetdata(String key, Object value) {
		this.itemMetadata.put(key, value);
	}
	
	/***
	 * Append an Object to a list in the metadata.
	 * 
	 * If the list does not exist, it will be created.
	 * @param listName The name of the list to access.
	 * @param value The object to append to the list.
	 */
	public void addMetadataToArray(String listName, Object value) {
		JSONArray jsonArray;
		
		if (itemMetadata.has(listName)) {
			jsonArray = itemMetadata.getJSONArray(listName);
		} else {
			jsonArray = new JSONArray();
		}
		
		itemMetadata.put(listName, jsonArray.put(value));
	}
	
	/***
	 * Add a file on the list to download.
	 * @param sUrl The URL where the file can be downloaded.
	 * @param type The type of the file (e.g. PDF or TXT).
	 */
	public void addTextFileUrl(String sUrl, FileType type) {
		URL url;
		try {
			url = new URL(sUrl);
		} catch (MalformedURLException ex) {
			logger.error(ex.getMessage());
			return;
		}
		
		if (textFileUrls.contains(url)) {
			return;
		}
	
		this.textFileUrls.add(url);
		this.textFileTypes.add(type);
	}
	
	public long getItemId() {
		return this.itemID;
	}
	
	public void digestItemData(Item other) {
		this.itemID = other.getItemId();
		this.itemUrl = other.getUrl();
		this.dataSource = other.getDataSource();
		this.textFileTypes = other.getTextFileTypes();
		this.textFileUrls = other.getTextFileUrls();
		this.itemMetadata = other.getItemMetadata();
		this.documentMetadata = other.getDocumentMetadata();
	}
	
	public void setDataSource(String dataSource) {
		this.dataSource = dataSource;
	}
	
	public String getDataSource() {
		return dataSource;
	}
	
	public URL getUrl() {
		return itemUrl;
	}

	/**
	 * Checks the Item's metadata for copyright information.
	 * Checks, if there is a field saying "NOT_IN_COPYRIGHT" or "Public domain" and if a field holds a Creative Commons URL.
	 * If the Creative Commons URL is given, this will have priority.
	 *
	 * @return A CopyrightStatus object.
	 */
	public CopyrightStatus getCopyrightStatus() {
		JSONObject documentMetadata = getDocumentMetadata();

		String copyrightStatusKey = "CopyrightStatus";
		String licenseUrlKey = "LicenseUrl";

		// Default is "in copyright"
		CopyrightStatus copyrightStatus = CopyrightStatus.IN_COPYRIGHT;
		if (documentMetadata.has(copyrightStatusKey)) {
			String copyrightString = documentMetadata.getString(copyrightStatusKey);
			if (copyrightString.equalsIgnoreCase("not_in_copyright") ||
				copyrightString.toLowerCase().contains("public domain")) {
				copyrightStatus = CopyrightStatus.NOT_IN_COPYRIGHT;
			}

			if (documentMetadata.has(licenseUrlKey)) {
				String licenseUrlString = documentMetadata.getString(licenseUrlKey);
				if (licenseUrlString.contains("//creativecommons.org/licenses")) {
					copyrightStatus = CopyrightStatus.CREATIVE_COMMONS_LICENSE;
				}
			}
		}

		return copyrightStatus;
	}
	
	public ArrayList<URL> getTextFileUrls() {
		return textFileUrls;
	}
	
	public ArrayList<FileType> getTextFileTypes() {
		return textFileTypes;
	}

	/**
	 * Returns any non-normalized item metadata.
	 */
	public JSONObject getItemMetadata() {
		return itemMetadata;
	}

	/**
	 * Returns only Metadata like author(s), publication year etc.
	 */
	public JSONObject getDocumentMetadata() {
		return documentMetadata;
	}

	public boolean getSaveMetadataOnly() {
		return saveMetadataOnly;
	}

	/***
	 * The item id HAS to be set! From the ID all file names are derived!
	 */
	public void setItemId(long id) {
		this.itemID = id;
	}
	
	public void setItemUrl(String sUrl) {
		URL url;
		try {
			url = new URL(sUrl);
		} catch (MalformedURLException ex) {
			logger.error(ex.getMessage());
			return;
		}
		setItemUrl(url);
	}
	
	public void setItemUrl(URL url) {
		this.itemUrl = url;
	}

	public void setSaveMetadataOnly(boolean toSave) {
		this.saveMetadataOnly = toSave;
	}

	public void setDocumentMetadata(JSONObject documentMetadata) {
		this.documentMetadata = documentMetadata;
	}
	
	public Path writeMetadataFile(String outputDirectory, FileType outputFormat) 
			throws UnsupportedOutputFormatException {
		addObjectVariableDataToMetadata();
		
		Path outputPath = Paths.get(outputDirectory, METADATA_OUTPUT_FOLDER_NAME);
		Path filePath = getLocaleItemFilePath(outputPath, outputFormat);
		createDirectoryIfNotExisting(filePath.getParent());
		
		logger.info("Preparing to write metadata into {}", filePath.toAbsolutePath());
		
		String metadataOutputString = getMetadataStringForFileType(outputFormat);
		
		logger.info("Writing metadata");
		
		writeStringToFile(filePath, metadataOutputString);
		
		return filePath;
	}
	
	public List<Path> writeTextFiles(String outputDirectory, boolean overwriteExistingFiles) {

		if (shallOnlyMetadataBeSaved()) {
			return Collections.emptyList();
		}

		Path outputPath = Paths.get(outputDirectory, TEXT_OUTPUT_FOLDER_NAME);

		ArrayList<Path> downloadedFiles = new ArrayList<>();
		int failedFileDownloadCount = 0;

		for (int i = 0; i < textFileUrls.toArray().length; ++i) {
			URL fileUrl = textFileUrls.get(i);
			FileType fileType = textFileTypes.get(i);
			Path textFilePath = getLocaleItemFilePath(outputPath, fileType);
			
			if (textFilePath.toFile().exists() && !overwriteExistingFiles) {
				logger.info("File {} exists already! Skipping!", textFilePath);
				continue;
			}
			
			createDirectoryIfNotExisting(textFilePath.getParent());
			try {
				if (downloadFile(fileUrl, textFilePath)) {
					downloadedFiles.add(textFilePath);
				}
			} catch (DownloadFailedException ex) {
				logger.info("Ignoring failed download! Continue processing item downloads!");
				++failedFileDownloadCount;
			}
		}

		if (failedFileDownloadCount == textFileUrls.size()) {
			logger.error("All file downloads of item ID " + itemID + " failed downloading!");
		}
		
		return downloadedFiles;
	}

	public String getMetadataStringForFileType(FileType outputFormat)
			throws UnsupportedOutputFormatException {
		switch (outputFormat) {
			case XML:
				return XML.toString(itemMetadata, METADATA_ROOT_NAME);
			case JSON:
				return itemMetadata.toString(JSON_N_SPACES_FOR_INDENTATION);
			default:
				throw new UnsupportedOutputFormatException("The given FileType '" + outputFormat.toString() +
						"' is not supported! Please choose: XML or JSON .");

		}
	}

	public boolean shallOnlyMetadataBeSaved() {
		return this.getSaveMetadataOnly();
	}
	
	private void addObjectVariableDataToMetadata() {
		itemMetadata.put(METADATA_ITEM_ID_STRING, itemID);
		itemMetadata.put(METADATA_URL_STRING, itemUrl);
		itemMetadata.put(METADATA_ITEM_SOURCE_STRING, dataSource);
		
		JSONArray textUrls = new JSONArray();
		for (int i = 0; i < this.textFileUrls.toArray().length; ++i) {
			JSONObject instanceJson = new JSONObject();
			instanceJson.put(METADATA_URL_STRING, this.textFileUrls.get(i));
			instanceJson.put(METADATA_ITEM_TEXT_FILE_TYPE_STRING, this.textFileTypes.get(i));
			textUrls.put(instanceJson);
		}
		JSONObject textUrlList = new JSONObject();
		textUrlList.put(METADATA_ITEM_TEXT_URL_STRING, textUrls);
		itemMetadata.put(METADATA_ITEM_TEXT_URLS_PARENT_STRING, textUrlList);
	}
	
	private boolean createDirectoryIfNotExisting(Path directoryPath) {
		if (!directoryPath.toFile().exists()) {
			return directoryPath.toFile().mkdirs();
		}
		return false;
	}
	
	private Path getLocaleItemFilePath(Path outputPath, FileType fileType) {
		boolean allowDuplication = false;
		outputPath = outputPath.resolve(fileType.toString().toLowerCase());
		String textFileName = getItemFileNameForFiletype(fileType, allowDuplication);
		createdTextFiles.add(textFileName);
		return outputPath.resolve(textFileName);
	}
	
	private String getItemFileNameForFiletype(FileType type, boolean allowDuplication) {
		String fileName = itemID + "." + type.getFileSuffix().toLowerCase();
		
		if (!allowDuplication && checkFileNameForDuplication(fileName)) {
			fileName = deduplicateFileName(fileName);
		}

		return fileName;
	}
	
	private boolean checkFileNameForDuplication(String fileName) {
		return createdTextFiles.contains(fileName);
	}
	
	private String deduplicateFileName(String fileName) {
		String fileNameSuffix = FilenameUtils.getExtension(fileName);
		String fileNameBase = FilenameUtils.getBaseName(fileName);
		
		int counter = 1;
		while (true) {
			fileName = fileNameBase + "-" + Integer.toString(counter) + "." + fileNameSuffix;
			
			if (checkFileNameForDuplication(fileName)) {
				++counter;
			} else {
				break;
			}
		}
		
		return fileName;
	}
	
	private boolean downloadFile(URL sourceUrl, Path sinkFilePath) throws DownloadFailedException {
		try {
			logger.info("Downloading file '{}' from URL: {}", sinkFilePath, sourceUrl);
			FileUtils.downloadFile(sinkFilePath.toFile(), sourceUrl.toString());
			logger.info("Download done!");
		} catch (IOException ex) {
			throw new DownloadFailedException("An error happened while downloading from URL '" + 
					sourceUrl + "'.\n" + "Error Message: " + ex.getMessage());
		}
		
		return true;
	}
	
	private void writeStringToFile(Path filePath, String content) {
		try {
			Files.write(filePath, content.getBytes());
		} catch (IOException ex) {
			logger.error(Arrays.toString(ex.getStackTrace()));
		}
	}
	
	class DownloadFailedException extends Exception {
		private static final long serialVersionUID = 558535317153362842L;

		DownloadFailedException(String s) {
			super(s);
		}
	}
	
	class UnsupportedOutputFormatException extends Exception {
		private static final long serialVersionUID = 7620152992815263665L;

		UnsupportedOutputFormatException(String s) {
			super(s);
		}
	}
}
