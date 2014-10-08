package org.helioviewer.viewmodel.view.jp2view.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * Input stream with a fixed size. After reading the expected number of bytes
 * this input stream will behave as if the end of the stream has been reached.
 * 
 * @author Andre Dau
 */
public class FixedSizedInputStream extends InputStream {
    /** Expected length of the HTTP input stream. */
    private int remainingBytes;

    /** Underlying input stream. */
    private InputStream in;

    /**
     * Constructor for an input stream with a fixed size.
     * 
     * @param in
     *            The underlying input stream from which to read.
     * @param expectedSizeInBytes
     *            Expected number of bytes in the input stream.
     */
    public FixedSizedInputStream(InputStream in, int expectedSizeInBytes) {
        this.remainingBytes = expectedSizeInBytes;
        this.in = in;
    }

    /** @override */
    public int read() throws IOException {
        if (remainingBytes > 0) {
            --remainingBytes;
            return in.read();
        } else {
            return -1;
        }
    }

    /** @override */
    public int read(byte[] b, int off, int len) throws IOException {
        if (remainingBytes > 0) {
            int bytesRead = in.read(b, off, remainingBytes < len ? remainingBytes : len);
            remainingBytes -= bytesRead;
            return bytesRead;
        } else {
            return -1;
        }
    }

    /** @override */
    public int read(byte[] b) throws IOException {
        if (remainingBytes > 0) {
            int bytesRead = in.read(b, 0, remainingBytes < b.length ? remainingBytes : b.length);
            remainingBytes -= bytesRead;
            return bytesRead;
        } else {
            return -1;
        }
    }
}
