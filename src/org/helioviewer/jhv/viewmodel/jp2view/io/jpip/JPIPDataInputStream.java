package org.helioviewer.jhv.viewmodel.jp2view.io.jpip;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ProtocolException;

/**
 * The class <code>JpipDataInputStream</code> allows to read JPIP data-bin
 * segments, as it is defined in the Part 9 of the the JPEG2000 standard.
 */
class JPIPDataInputStream {

    /** The last class identifier read. */
    private long classId = 0;

    /** The last code-stream index read. */
    private long codestream = 0;

    /** The total length in bytes of the last VBAS read. */
    private int vbasLength = 0;

    /** The first byte of the last VBAS read. */
    private int vbasFstByte = 0;

    /** Number of bytes read up until now. */
    private int bytesRead = 0;

    /** The <code>InputStream</code> base. */
    private InputStream in;

    /** Constructs a object based on the indicated <code>InputStream</code>. */
    public JPIPDataInputStream(InputStream in) {
        this.in = in;
    }

    /**
     * @return The number of bytes read from the underlying input stream up
     *         until now.
     */
    public int getNumberOfBytesRead() {
        return bytesRead;
    }

    /**
     * Reads a byte and increments the byte counter.
     * 
     * @return The next byte of data, or -1 if the end of the stream is reached.
     * @throws IOException
     */
    private int read() throws IOException {
        int b = in.read();
        if (b != -1) {
            ++bytesRead;
        }
        return b;
    }

    /**
     * Reads up to len bytes of data from the input stream into an array of
     * bytes. An attempt is made to read as many as len bytes, but a smaller
     * number may be read. The number of bytes actually read is returned as an
     * integer.
     * 
     * @param b
     *            The buffer into which the data is read.
     * @param off
     *            The start offset in array b at which the data is written.
     * @param len
     *            The maximum number of bytes to read.
     * @return The total number of bytes read into the buffer, or -1 if there is
     *         no more data because the end of the stream has been reached.
     * @throws IOException
     */
    private int read(byte b[], int off, int len) throws IOException {
        int bytesJustRead = in.read(b, off, len);
        if (bytesJustRead != -1) {
            bytesRead += bytesJustRead;
        }
        return bytesJustRead;
    }

    /**
     * Reads an VBAS integer from the stream. The length in bytes of the VBAS is
     * stored in the <code>vbasLength</code>variable, and the first byte of the
     * VBAS is stored in the <code>vbasFstByte</code> variable.
     * 
     * @throws java.io.IOException
     */
    private long readVBAS() throws IOException {
        int c;
        long value = 0;

        vbasLength = 0;

        do {
            if (vbasLength >= 9)
                throw new ProtocolException("VBAS length not supported");

            if ((c = read()) < 0) {
                if (vbasLength > 0)
                    throw new EOFException("EOF reached before completing VBAS");
                else
                    return -1;
            }

            value = (value << 7) | (long) (c & 0x7F);

            if (vbasLength == 0)
                vbasFstByte = c;
            vbasLength++;

        } while ((c & 0x80) != 0);

        return value;
    }

    /**
     * Reads the next data segment from the stream, and stores its information
     * in the <code>JpipDataSegment</code> object passed as parameter. The data
     * buffer is not reallocated every time. It is only reallocated if the next
     * data length is bigger than the previous one.
     * 
     * @throws java.io.IOException
     * @return Returns <code>true</code> if a new data segment was read, or
     *         <code>false</code> if the end of stream was reached.
     */
    public JPIPDataSegment readSegment() throws IOException {
        int m;
        long id;
        if ((id = readVBAS()) < 0)
            return null;

        JPIPDataSegment seg = new JPIPDataSegment();

        seg.binID = id;

        if (vbasFstByte == 0) {
            seg.isEOR = true;

            if ((seg.binID = read()) < 0)
                throw new EOFException("EOF reached before completing EOR message");

            seg.length = (int) readVBAS();

        } else {
            seg.isEOR = false;
            seg.binID &= (long) ~(0x70L << ((vbasLength - 1) * 7));

            seg.isFinal = ((vbasFstByte & 0x10) != 0);

            m = (vbasFstByte & 0x7F) >> 5;

            if (m == 0)
                throw new ProtocolException("Invalid Bin-ID value format");
            else if (m >= 2) {
                classId = readVBAS();
                if (m > 2)
                    codestream = readVBAS();
            }
            seg.codestreamID = codestream;

            for (JPIPDatabinClass idEnum : JPIPDatabinClass.values())
                if (classId == idEnum.getStandardClassID())
                    seg.classID = idEnum;
            if (seg.classID == null)
                throw new ProtocolException("Invalid databin classID");

            seg.offset = (int) readVBAS();
            seg.length = (int) readVBAS();

            if ((classId == JPIPConstants.EXTENDED_PRECINCT_DATA_BIN_CLASS) || (classId == JPIPConstants.EXTENDED_TILE_DATA_BIN_CLASS))
                seg.aux = readVBAS();
        }

        if (seg.length > 0) {
            // Assign new array if it was null
            if (seg.data == null)
                seg.data = new byte[seg.length];
            // Assign larger array if needed.
            seg.data = seg.data.length < seg.length ? new byte[seg.length] : seg.data;

            int offset = 0;
            int len = seg.length;
            
            while(len!=0)
            {
                int read=read(seg.data, offset, len);
                if(read==-1)
                    throw new EOFException("Unexpected EOF");
                
                len-=read;
                offset+=read;
            }
        }

        return seg;
    }
};
