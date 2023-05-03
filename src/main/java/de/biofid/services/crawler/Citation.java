package de.biofid.services.crawler;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/***
 * An abstract class holding the citation data for an item.
 *
 * @author Adrian Pachzelt (University Library Johann Christian Senckenberg, Frankfurt)
 * @author https://www.biofid.de
 * @version 1.0
 */
public class Citation {

	// This name makes it consistent with the BHL naming of the same parameter
	public static final String SERIALIZATION_NAME_ITEM_PUBLICATION_YEAR = "year";
	public static final int integerDefault = -99999;

	private static final Pattern pattern = Pattern.compile("^[0-9]{4}");
	
	private List<String> authors = new ArrayList<>();
	private String firstPage = ""; // No Integer, because page numbers can also be roman!
	private String issueNumber = "";
	private String journalName = "";
	private String lastPage = "";
	private String title = "";
	private int year = integerDefault;
	
	public void addAuthor(String author) {
		if (author == null) {
			return;
		}
		if (!this.authors.contains(author)) {
			author = author.trim();
			this.authors.add(author);
		}
	}
	
	public void addAuthors(String[] authors) {
		if (authors == null) {
			return;
		}
		for (String author : authors) {
			addAuthor(author);
		}
	}
	
	public String getFirstPage() {
		return firstPage;
	}
	
	public String getIssueNumber() {
		return issueNumber;
	}
	
	public String getJournalName() {
		return journalName;
	}
	
	public String getLastPage() {
		return lastPage;
	}

	@JsonProperty("Year")
	public int getPublicationYear() {
		return year;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setIssueNumber(String issueNumber) {
		this.issueNumber = issueNumber;
	}
	
	public void setJournalName(String journalName) {
		this.journalName = journalName;
	}
	
	public void setPages(String firstPage, String lastPage) {
		this.firstPage = firstPage;
		this.lastPage = lastPage;
	}
	
	public void setPublicationYear(int year) {
		this.year = year;
	}
	
	public void setPublicationYear(String year) {
		Matcher matcher = pattern.matcher(year);
		if (matcher.find())
		{
			this.year = Integer.parseInt(matcher.group());
		}
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String toString() {
		return "Authors: " + authors.toString() + "\n Title: " + title + "\n Year: " + year + "\n"
				+ " First Page: " + firstPage + "\n Last Page: " + lastPage + "\n Journal: " + journalName
				+ "\n Issue number: " + issueNumber;			
	}
}
