package de.biofid.services.crawler.metadata;

import de.biofid.services.crawler.metadata.Citation;

import java.net.URL;

public class Metadata {
    public Citation citation;
    public long id;
    public URL pdfUrl;

    public Metadata(long id, URL pdfUrl, Citation citation) {
        this.pdfUrl = pdfUrl;
        this.citation = citation;
        this.id = id;
    }
}
