package de.biofid.services.crawler.metadata;

import java.net.URL;

public class Metadata {
    public Citation citation;
    public long id;
    public URL pdfUrl;
    public URL url;

    public Metadata(long id, URL pdfUrl, Citation citation, URL url) {
        this.pdfUrl = pdfUrl;
        this.citation = citation;
        this.id = id;
        this.url = url;
    }

    public Metadata(long id, URL pdfUrl, Citation citation) {
        this(id, pdfUrl, citation, null);
    }
}
