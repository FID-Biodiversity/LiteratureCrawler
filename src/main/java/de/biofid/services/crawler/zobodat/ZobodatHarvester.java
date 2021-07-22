package de.biofid.services.crawler.zobodat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.biofid.services.crawler.Configuration;
import de.biofid.services.crawler.Harvester;
import de.biofid.services.crawler.Item;
import de.biofid.services.crawler.metadata.Citation;
import de.biofid.services.crawler.metadata.Metadata;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/***
 * A Harvester to crawl Zobodat.at literature.
 * 
 * @author Adrian Pachzelt (University Library Johann Christian Senckenberg, Frankfurt)
 * @author https://www.biofid.de
 * @version 1.0
 */
public class ZobodatHarvester extends Harvester {
	
	public static final String ZOBODAT_LITERATURE_BASE_URL = "https://www.zobodat.at/publikation_series.php";
	public static final String ZOBODAT_STRING = "Zobodat";
	
	public static final String ATTRIBUTE_HREF = "href";

	private static final String ITEM_COMPLETE_METADATA = "Item";
	private static final String CONFIGURATION_ITEM_LIST = "items";
	private static final String CONFIGURATION_TITLE_LIST = "titles";
	private static final String CONFIGURATION_CRAWL_ALL_ITEMS = "crawl-all-items";

	public static final Pattern REGEX_PATTERN_AUTHOR_AND_YEAR = Pattern.compile("^(.*?) ?\\(([0-9]{4})-?[0-9]{0,4}\\)");
	public static final Pattern REGEX_PATTERN_ISSUE_NUMBER = Pattern.compile("– ([^–]*?): ");
	public static final Pattern REGEX_PATTERN_ITEM_ID_IN_ZOBODAT_URL = Pattern.compile("\\?id=([0-9]*)");
	public static final Pattern REGEX_PATTERN_PAGES = Pattern.compile(": ([XI0-9]*?) - ([XI0-9]*?)\\.$");
	public static final Pattern REGEX_PATTERN_TITLE_AND_JOURNAL_NAME = Pattern.compile("\\([0-9]{4}\\): (.*?) – (.*) – ");
	
	public static final String SELECTOR_CITATION_CONTAINER = "#publikation_articles .text";
	public static final String SELECTOR_CONTENT = "div.content";
	public static final String SELECTOR_DIV = "div";
	public static final String SELECTOR_HYPERLINKS = "a";
	public static final String SELECTOR_ITEM_FROM_DOCUMENT_LIST = "ul.search-results-list li.result";
	public static final String SELECTOR_ITEM_URL = ".content a.red";
	public static final String SELECTOR_PUBLICATION_LINK = "a.publication-link";
	
	public static final String ZOBODAT_URL = "https://www.zobodat.at";
	public static final String ZOBODAT_ARTICLE_BASE_URL = "https://www.zobodat.at/publikation_articles.php?id=";
	public static final String ZOBODAT_TITLE_BASE_URL = "https://www.zobodat.at/publikation_series.php?id=";
	
	private boolean isMetadataCollected = false;
	private Iterator<Metadata> itemMetadataIterator = null;
	private final List<Metadata> itemMetadataList = new ArrayList<>();
	private boolean crawlAllItems;
	
	private List<Object> listOfItemsToProcess = new ArrayList<>();

	public ZobodatHarvester(Configuration configuration) throws IOException {
		super(configuration);
		
		JSONObject jsonConfiguration = configuration.getHarvesterJsonConfiguration();
		
		if (jsonConfiguration.has(CONFIGURATION_ITEM_LIST)) {
    		JSONArray itemListFromConfiguration = jsonConfiguration.getJSONArray(CONFIGURATION_ITEM_LIST);
    		listOfItemsToProcess = StreamSupport.stream(itemListFromConfiguration.spliterator(), false)
					.map(itemId -> idStringToZobodatArticleUrl((String) itemId))
					.collect(Collectors.toList());
    	}

		if (jsonConfiguration.has(CONFIGURATION_TITLE_LIST)) {
			JSONArray titleListFromConfiguration = jsonConfiguration.getJSONArray(CONFIGURATION_TITLE_LIST);
			List<Object> listOfTitles = StreamSupport.stream(titleListFromConfiguration.spliterator(), false)
					.map(itemId -> (Object) idToZobodatTitleUrl((int) itemId))
					.collect(Collectors.toList());
			listOfItemsToProcess.addAll(listOfTitles);
		}

		crawlAllItems = jsonConfiguration.optBoolean(CONFIGURATION_CRAWL_ALL_ITEMS, true);
	}
	
	public Document getDocumentFromUrl(String url) throws IOException {
		return Jsoup.connect(url).get();
	}
	
	public String getFolderName() {
		return ZOBODAT_STRING;
	}
	
	public Elements getItemListFromWebsite(Document website) {
		return website.select(SELECTOR_ITEM_FROM_DOCUMENT_LIST);
	}
	
	public JSONArray getMetadataListAsJSONArray() {
		return toJSONArray(itemMetadataList);
	}
	
	public int getMetadataListSize() {
		return itemMetadataList.size();
	}
	
	/***
	 * Run over a given list of item elements from the Zobodat page.
	 * 
	 * This function finds out, if the given list represents a list of articles or if it needs
	 * to delve deeper. If the latter, all given items are crawled recursively.
	 * If it is a list with articles, their metadata are extracted and stored.
	 * @param itemList A list of items from the Zobodat page.
	 */
	public void iterateItems(Elements itemList) {
		int sumOfAllItemsReferencedFromThisSite = 0;
		
		for (Element item : itemList) {
			String urlString = item.select(SELECTOR_ITEM_URL).attr(ATTRIBUTE_HREF);
			urlString = generateZobodatUrlStringFromString(urlString);
			
			if (!urlString.isEmpty()) {
				sumOfAllItemsReferencedFromThisSite = crawlUrlRecursively(urlString);
			}
		}
		
		if (sumOfAllItemsReferencedFromThisSite == 0) {
			logger.debug("Is article list!");
			itemMetadataList.addAll(extractItemMetadataFromArticleList(itemList));
		}
	}
	
	public boolean nextItem(Item item) {
		if (!isMetadataCollected) {
			logger.info("Start crawling metadata!");
			if (!listOfItemsToProcess.isEmpty() && !crawlAllItems) {
				logger.info("Crawling the following item ID list: " + listOfItemsToProcess);
				for (Object obj : listOfItemsToProcess) {
					String itemUrl = (String) obj;
					crawlUrlRecursively(itemUrl);
				}
			} else {
				logger.info("Start crawling all of Zobodat!");
				crawlUrlRecursively(ZOBODAT_LITERATURE_BASE_URL);
			}
			isMetadataCollected = true;
			itemMetadataIterator = itemMetadataList.iterator();
			logger.info("Crawling of metadata complete!");
		}
		
		if (itemMetadataIterator.hasNext()) {
			logger.debug("Getting next metadata!");
			Metadata itemMetadata = itemMetadataIterator.next();
			addMetadataToItem(item, itemMetadata);
		} else {
			return false;
		}
		
		return true;
	}

	public String idStringToZobodatArticleUrl(String itemId) {
		return ZOBODAT_ARTICLE_BASE_URL + itemId;
	}

	public String idToZobodatTitleUrl(int itemId) {
		return idToZobodatTitleUrl(Integer.toString(itemId));
	}

	public String idToZobodatTitleUrl(String itemId) {
		return ZOBODAT_TITLE_BASE_URL + itemId;
	}
	
	public void addMetadataToItem(Item item, Metadata itemMetadata) {
		ObjectMapper mapper = new ObjectMapper();
		String metadataJSONString;
		try {
			metadataJSONString = mapper.writeValueAsString(itemMetadata);
		} catch (JsonProcessingException e) {
			logger.error("Could not create metadata JSON from item " + item.getItemId());
			return;
		}
		
		JSONObject itemMetadataJSON = new JSONObject(metadataJSONString);

		boolean shallItemBeSaved = isItemInListOfPublicationsToStore(itemMetadataJSON);
		item.setSaveMetadataOnly(shallItemBeSaved);
		
		long itemID = Long.parseLong(itemMetadataJSON.remove("id").toString());
    	logger.debug("Processing Item ID " + itemID);
		item.setDataSource(ZOBODAT_STRING);
		item.setItemId(itemID);
		item.addTextFileUrl((String) itemMetadataJSON.remove("pdfUrl"), Item.FileType.PDF);
		item.addMetdata(ITEM_COMPLETE_METADATA, itemMetadataJSON);
	}

	public boolean isItemInListOfPublicationsToStore(JSONObject itemMetadata) {
		if (itemMetadata.isEmpty()) {
			return false;
		}

		Object journalUrl = null;
		try {
			journalUrl = itemMetadata.getJSONObject("citation").getJSONObject("journalName").get("uri");
		} catch (JSONException ex) {
			logger.info("There was an issue with processing this data: " + itemMetadata);
		}

		if (journalUrl != null) {
			return listOfItemsToProcess.contains(journalUrl.toString());
		} else {
			return false;
		}
	}

	private int crawlUrlRecursively(String url) {
		if (url.isEmpty()) {
			return 0;
		}
		
		pause();
		
		logger.info("Processing URL " + url + "");
		
		Document website;
		try {
			website = getDocumentFromUrl(url);
		} catch (IOException ex) {
			logger.error("Could not fetch URL " + url);
			return 0;
		}
		
		Elements itemList = getItemListFromWebsite(website);
		iterateItems(itemList);
		
		return itemList.size();
	}
	
	private List<Metadata> extractItemMetadataFromArticleList(Elements itemList) {
		List<Metadata> metadataList = new ArrayList<>();
		
		logger.info("Found " + itemList.size() + " items on this site!");
		
		for (Element item : itemList) {
			URL itemPdfUrl = getItemPdfUrl(item);
			
			if (itemPdfUrl != null) {
				URL citationUrl = getCitationUrl(item);
				long itemID = getItemIDFromUrl(citationUrl);
				Citation citation = getCitationFromUrl(citationUrl);
				metadataList.add(new Metadata(itemID, itemPdfUrl, citation, citationUrl));
			}
		}
		
		
		return metadataList;
	}
	
	private String generateZobodatUrlStringFromString(String url) {
		if (url.isEmpty()) {
			return url;
		}
		
		if (url.startsWith(ZOBODAT_URL)) {
			return url;
		} else if (!url.startsWith("/")) {
			url = "/" + url;
		}
		
		return ZOBODAT_URL + url;
	}

	public Citation getCitationFromUrl(URL url) {
		Citation citation;
		try {
			Document citationSite = getDocumentFromUrl(url.toString());
			Element citationContainer = citationSite.selectFirst(SELECTOR_CITATION_CONTAINER);
			if (citationContainer == null) {
				throw new IOException("No citation container available");
			}

			citation = ZobodatCitationGenerator.generateCitationFromHtmlElement(citationContainer);

			logger.debug("Generated citation: " + citation);
		} catch (IOException e) {
			logger.error("Could not collect citation site: " + url);
			citation = null;
		}

		return citation;
	}

	private URL getCitationUrl(Element item) {
		Element contentContainer = item.selectFirst(SELECTOR_CONTENT);
		Element citationLinkElement = contentContainer.select(SELECTOR_DIV).last();
		String citationUrlString = generateZobodatUrlStringFromString(
				citationLinkElement.select(SELECTOR_HYPERLINKS).attr(ATTRIBUTE_HREF));
		URL citationUrl;
		try {
			citationUrl = new URL(citationUrlString);
			logger.debug("Found citation URL: " + citationUrlString);
		} catch (MalformedURLException e) {
			logger.error("Malformed Citation URL: " + citationUrlString);
			citationUrl = null;
		}

		return citationUrl;
	}

	private long getItemIDFromUrl(URL url) {
		Matcher itemIdMatcher = REGEX_PATTERN_ITEM_ID_IN_ZOBODAT_URL.matcher(url.toString());

		long itemID = -1;
		if (itemIdMatcher.find()) {
			 itemID = Long.parseLong(itemIdMatcher.group(1));
		}

		logger.debug("Generated Item ID: " + itemID);

		return itemID;
	}

	private URL getItemPdfUrl(Element item) {
		Element pdfElement = item.selectFirst(SELECTOR_PUBLICATION_LINK);
		if (pdfElement != null) {
			String pdfUrlSubString = pdfElement.attr(ATTRIBUTE_HREF);
			String pdfUrlString = generateZobodatUrlStringFromString(pdfUrlSubString);
			URL pdfUrl;
			try {
				pdfUrl = new URL(pdfUrlString);
				logger.debug("Found PDF URL: " + pdfUrlString);
			} catch (MalformedURLException e) {
				logger.error("Malformed PDF URL: " + pdfUrlString);
				pdfUrl = null;
			}

			return pdfUrl;
		}

		return null;
	}

	private JSONArray toJSONArray(Object obj) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			String jsonStringOfList = mapper.writeValueAsString(obj);
			return new JSONArray(jsonStringOfList);
		} catch (JsonProcessingException ex) {
			logger.error("Could not convert item metadata list to JSON!");
		}

		return null;
	}
}
