package org.helioviewer.jhv.viewmodel.jp2view.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ProtocolException;

import javax.annotation.Nullable;

/**
 * The class <code>ChunkedInputStream</code> allows to decode HTTP chunked
 * responses with a simple format. Does not support internal chunk headers.
 */
public class ChunkedInputStream extends InputStream
{
	/** The last chunk length */
	private int chunkLength = 0;

	private boolean eof = false;

	/** The base input stream */
	private final InputStream in;

	/**
	 * Constructs a new object with a <code>InputStream</code> base object.
	 * 
	 * @param _in
	 *            A <code>InputStream</code> object as a base stream.
	 */
	public ChunkedInputStream(InputStream _in)
	{
		in = _in;
	}

	/**
	 * This kind of stream does not support marking.
	 * 
	 * @return <code>False</code>
	 */
	public boolean markSupported()
	{
		return false;
	}

	/**
	 * Used internally to read lines from the input stream and control if the
	 * end of stream is reached unexpectedly.
	 * 
	 * @return A new line from the input stream
	 * @throws java.io.IOException
	 * 
	 */
	private String readLine() throws IOException
	{
		String res = LineReader.readLine(in);
		if (res != null)
			return res;
		else
			throw new EOFException("Unexpected end of stream decoding chunk");
	}

	/**
	 * Reads the next byte of the chunked content. It will return -1 if there is
	 * no more chunks to decode. If the end of stream is reached before decoding
	 * correctly all the chunks, a <code>EOFException</code> is launched.
	 * 
	 * @return The next byte read, or -1 is there is no more data.
	 * @throws java.io.IOException
	 */
	public int read() throws IOException
	{
		for (;;)
			switch (read(tmpRead, 0, 1))
			{
				case -1:
					return -1;
				case 1:
					return tmpRead[0] & 0xff;
				default:
					throw new IllegalArgumentException();
			}
	}

	private byte[] tmpRead = new byte[1];

	@Override
	public int read(@Nullable byte[] b, int off, int len) throws IOException
	{
		for (;;)
		{
			if (eof)
				return -1;

			if (chunkLength > 0)
			{
				int read = in.read(b, off, Math.min(chunkLength, len));
				if (read != -1)
				{
					chunkLength -= read;

					if (chunkLength == 0)
						if (readLine().length() > 0)
							throw new ProtocolException("An empty new line was expected after chunk");
				}

				return read;
			}

			if (chunkLength == 0)
			{
				String line = readLine();
				try
				{
					chunkLength = Integer.parseInt(line, 16);
					if (chunkLength <= 0)
						eof = true;
				}
				catch (NumberFormatException ex)
				{
					throw new ProtocolException("Invalid chunk length format.");
				}
			}
		}
	}
}
