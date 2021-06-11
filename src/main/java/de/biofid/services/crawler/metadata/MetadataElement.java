package de.biofid.services.crawler.metadata;

import java.net.MalformedURLException;
import java.net.URL;

public class MetadataElement {
    public String label;
    public URL uri;

    public MetadataElement(String label, String uri) {
        this.label = label.trim();
        try {
            this.uri = new URL(uri);
        } catch (MalformedURLException ex) {
            this.uri = null;
        }
    }

    public boolean isEmpty() {
        return this.label.isEmpty();
    }

    public boolean equals(Object object) {
        if (object instanceof MetadataElement) {
            MetadataElement comparator = (MetadataElement) object;
            return this.label.equals(comparator.label);
        }
        else {
            return false;
        }
    }
}
