package org.helioviewer.jhv.plugins.pfssplugin.data.decompression;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.helioviewer.jhv.plugins.pfssplugin.data.PfssCompressed;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;

/**
 * Wrapper class around the jUnrar library.
 */
public class UnRar {

	/**
	 * Unrar PFSSData object
	 * @param data to unrar
	 * @return raw byte stream of unrar data
	 * @throws IOException The jUnrar library seems unable to decompress a byteArrayInputStream and needs to have a file on the local filesystem. Throws an IOException if it was unable to write the temp file.
	 */
	public static ByteArrayOutputStream unrarData(PfssCompressed data) throws IOException {
		Archive archive = null;
		
		try {
			archive = new Archive(data.getVolumeManage());
            
            for(;;)
            {
    			FileHeader fh = archive.nextFileHeader();
    			if(fh==null)
    			    throw new IOException("Unsupported PFSS file format version");
    			
    			if("v1.fits".equals(fh.getFileNameString()))
    			{
        			ByteArrayOutputStream stream = new ByteArrayOutputStream();
        			archive.extractFile(fh, stream);
        			return stream;
    			}
            }
		} catch (RarException e) {
			throw new IOException(e);
		}

		finally {
			if (archive != null)
			archive.close();
		}
	}
	
}
