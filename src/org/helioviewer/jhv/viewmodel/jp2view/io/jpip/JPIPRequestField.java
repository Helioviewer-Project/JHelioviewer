package org.helioviewer.jhv.viewmodel.jp2view.io.jpip;

/**
 * A simple enum with many of the commonly used JPIP-request-field names.
 */
public enum JPIPRequestField {
    CNEW("cnew"), CCLOSE("cclose"), TYPE("type"), TID("tid"), STREAM("stream"), LEN("len"), CID("cid"), METAREQ("metareq"), ROFF("roff"), RSIZ("rsiz"), FSIZ("fsiz"), MODEL("model"), CONTEXT("context"), LAYERS("layers");

    private final String str;

    JPIPRequestField(final String _str) {
        str = _str;
    }

    public String toString() {
        return str;
    }
}
