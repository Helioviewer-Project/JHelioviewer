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

	public static void go() {
		try {
			//RARParser p = new RARParser();
			PfssData data = new PfssData(null, "file:///C:/Users/Jonas%20Schwammberger/Documents/GitHub/solution1.rar");
			data.loadData();
			File file = TikaInputStream.get(new ByteArrayInputStream(data.getData())).getFile();
            Archive archive = new Archive(file, null);
			FileHeader fh = archive.nextFileHeader();
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			archive.extractFile(fh, stream);
			int bla = stream.size();
		} catch (RarException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void main(String[] args) {
		go();
	}
}
