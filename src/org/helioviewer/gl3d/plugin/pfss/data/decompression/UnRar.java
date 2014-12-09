package org.helioviewer.gl3d.plugin.pfss.data.decompression;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.tika.io.TikaInputStream;
import org.helioviewer.gl3d.plugin.pfss.data.PfssData;

import de.innosystec.unrar.Archive;
import de.innosystec.unrar.exception.RarException;
import de.innosystec.unrar.rarfile.FileHeader;

public class UnRar {

	/**
	 * 
	 * @param data
	 * @return
	 * @throws IOException
	 */
	public static ByteArrayOutputStream unrarData(PfssData data) throws IOException {
		try {
			
			File file = TikaInputStream.get(new ByteArrayInputStream(data.getData())).getFile();
            Archive archive = new Archive(file, null);
			FileHeader fh = archive.nextFileHeader();
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			archive.extractFile(fh, stream);
			
			return stream;
		} catch (RarException e) {
			throw new IOException(e);
		}
	}
	
	public static void main(String[] args) {

	}
}
