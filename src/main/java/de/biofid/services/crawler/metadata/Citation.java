package de.biofid.services.crawler.metadata;

import java.util.ArrayList;
import java.util.List;


public class Citation {
    public List<MetadataElement> authors = new ArrayList<>();
    public String firstPage = ""; // No Integer, because page numbers can also be roman!
    public MetadataElement issueNumber = new MetadataElement();
    public MetadataElement journalName = new MetadataElement();
    public String lastPage = "";
    public String title = "";
    public int year = -1;

    public String toString() {
        return "Authors: " + authors.toString() + "\n Title: " + title + "\n Year: " + year + "\n"
                + " First Page: " + firstPage + "\n Last Page: " + lastPage + "\n Journal: " + journalName
                + "\n Issue number: " + issueNumber;
    }
}
