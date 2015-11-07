package org.helioviewer.jhv.viewmodel.jp2view.io.http;

/**
 * An enum with the some frequently used HTTP message headers. These are
 * documented in the RFC.
 */
public enum HTTPHeaderKey {
    CACHE_CONTROL("Cache-Control"), CONNECTION("Connection"), TRANSFER_ENCODING("Transfer-Encoding"), HOST("Host"), USER_AGENT("User-Agent"), CONTENT_LENGTH("Content-Length"), CONTENT_TYPE("Content-Type");
    private final String str;

    HTTPHeaderKey(final String _str) {
        str = _str;
    }

    /** Over ridden toString returns the HTTP/1.1 compatible header */
    public String toString() {
        return str;
    }
}
