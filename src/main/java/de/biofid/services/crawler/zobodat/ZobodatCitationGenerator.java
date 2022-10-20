package de.biofid.services.crawler.zobodat;

import de.biofid.services.crawler.metadata.Citation;
import de.biofid.services.crawler.metadata.MetadataElement;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZobodatCitationGenerator {
    private static final Pattern authorAndYearPattern = ZobodatHarvester.REGEX_PATTERN_AUTHOR_AND_YEAR;
    private static final Pattern issueNumberPattern = ZobodatHarvester.REGEX_PATTERN_ISSUE_NUMBER;
    private static final Pattern pagesPattern = ZobodatHarvester.REGEX_PATTERN_PAGES;
    private static final Pattern titleAndJournalNamePattern = ZobodatHarvester.REGEX_PATTERN_TITLE_AND_JOURNAL_NAME;
    private static final String defaultMetadataElementUrlString = "";

    public static final String HYPERLINK_REFERENCE_TO_AUTHOR = "personen.php";
    public static final String HYPERLINK_REFERENCE_TO_PUBLICATION_NAME = "publikation_series.php";
    public static final String HYPERLINK_REFERENCE_TO_PUBLICATION_VOLUME = "publikation_volumes.php";

    public static Citation generateCitationFromHtmlElement(Element citationElement) {
        Citation citation = new Citation();
        parseHyperlinkTextsToRespectiveClassField(citation, citationElement);
        tryToFillEmptyClassFieldsFromContainerText(citation, citationElement);
        return citation;
    }

    public static void addAuthor(Citation citation, MetadataElement author) {
        if (!citation.authors.contains(author)) {
            citation.authors.add(author);
        }
    }

    public static void addAuthors(Citation citation, MetadataElement[] authors) {
        for (MetadataElement author : authors) {
            addAuthor(citation, author);
        }
    }

    private static boolean doesHyperlinkReferenceContainSubstring(Element hyperlink, String subString) {
        return hyperlink.attr(ZobodatHarvester.ATTRIBUTE_HREF).contains(subString);
    }

    private static void parseHyperlinkTextsToRespectiveClassField(Citation citation, Element citationContainer) {
        Elements links = citationContainer.select(ZobodatHarvester.SELECTOR_HYPERLINKS);

        for (Element link : links) {
            if (doesHyperlinkReferenceContainSubstring(link, HYPERLINK_REFERENCE_TO_AUTHOR)) {
                citation.authors.add(createMetadataElementFromHtmlElement(link));
            } else if (doesHyperlinkReferenceContainSubstring(link, HYPERLINK_REFERENCE_TO_PUBLICATION_NAME)) {
                citation.journalName = createMetadataElementFromHtmlElement(link);
            } else if (doesHyperlinkReferenceContainSubstring(link, HYPERLINK_REFERENCE_TO_PUBLICATION_VOLUME)) {
                citation.issueNumber = createMetadataElementFromHtmlElement(link);
            }
        }
    }

    private static void tryToFillEmptyClassFieldsFromContainerText(Citation citation, Element citationContainer) {
        String citationText = citationContainer.text();

        // It may appear at Zobodat that a single author in the author list is linked, but the others not.
        // Hence, we cannot simply check, if the author list is not empty!
        Matcher authorAndYearMatcher = ZobodatCitationGenerator.authorAndYearPattern.matcher(citationText);
        while (authorAndYearMatcher.find()) {
            String authorsString = authorAndYearMatcher.group(1);
            for (String author : authorsString.split(",")) {
                addAuthor(citation, createMetadataElementFromHtmlElement(author));
            }
            int year = Integer.parseInt(authorAndYearMatcher.group(ZobodatHarvester.PUBLICATION_YEAR_FIRST_YEAR));
            if (!authorAndYearMatcher.group(ZobodatHarvester.PUBLICATION_YEAR_SECOND_YEAR).isEmpty()){
                String yearString = authorAndYearMatcher.group(ZobodatHarvester.PUBLICATION_YEAR_SECOND_YEAR);

                // The condition avoid setting a value of "05" parsed from a year "1904/05"
                if (yearString.length() == 4) {
                    year = Integer.parseInt(yearString);
                }
            }
            citation.year = year;
        }

        if (citation.issueNumber.isEmpty()) {
            Matcher issueNumberMatcher = issueNumberPattern.matcher(citationText);
            while (issueNumberMatcher.find()) {
                citation.issueNumber = createMetadataElementFromHtmlElement(issueNumberMatcher.group(1));
            }
        }

        if (citation.firstPage.isEmpty()) {
            Matcher pageMatcher = pagesPattern.matcher(citationText);
            while (pageMatcher.find()) {
                citation.firstPage = pageMatcher.group(1);
                citation.lastPage = pageMatcher.group(2);
            }
        }

        if (citation.title.isEmpty() || citation.journalName.isEmpty()) {
            Matcher titleAndJournalNameMatcher = titleAndJournalNamePattern.matcher(citationText);
            while (titleAndJournalNameMatcher.find()) {
                if (citation.title.isEmpty()) {
                    citation.title = titleAndJournalNameMatcher.group(1);
                }
                if (citation.journalName.isEmpty()) {
                    citation.journalName = createMetadataElementFromHtmlElement(titleAndJournalNameMatcher.group(2));
                }
            }
        }
    }

    private static MetadataElement createMetadataElementFromHtmlElement(Element htmlElement) {
        String uri = htmlElement.attr("href");
        if (!uri.startsWith("http") && !uri.isEmpty()) {
            uri = ZobodatHarvester.ZOBODAT_URL + "/" + uri;
        }
        return new MetadataElement(htmlElement.text(), uri);
    }

    private static MetadataElement createMetadataElementFromHtmlElement(String label) {
        return new MetadataElement(label, ZobodatCitationGenerator.defaultMetadataElementUrlString);
    }

}