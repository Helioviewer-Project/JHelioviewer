package org.helioviewer.jhv.gui.actions.filefilters;

import javafx.stage.FileChooser.ExtensionFilter;

public class FileFilter extends ExtensionFileFilter{

	public enum IMPLEMENTED_FILE_FILTER{
		JPG("JPG files (\"*.jpg\", \"*.jpeg\")", new String[]{ "*.jpg", "*.jpeg" }),
		PNG("PNG files (\"*.png\")", new String[]{ "*.png" }),
		JP2("JPG2000 files (\"*.jp2\", \"*.jpx\")", new String[] { "*.jp2", "*.jpx" }),
		FITS("FITS files (\"*.fts\", \"*.fits\")", new String[] { "*.fits", "*.fts" }),
		XML("XML files (\"*.xml\")", new String[] {"*.xml"});
		
		
		private FileFilter fileFilter;
		private IMPLEMENTED_FILE_FILTER(String description, String[] extensions) {
			fileFilter = new FileFilter(description, extensions);
		}
		
		public FileFilter getFileFilter(){
			return fileFilter;
		}
	}
	
	
	
	private String description;
	
	public FileFilter(String description, String[] extensions) {
		this.description = description;
		this.extensions = extensions;
	}

	@Override
	public String getDescription() {
		return description;
	}
	
	public ExtensionFilter getExtensionFilter(){
		return new ExtensionFilter(description, extensions);
	}
}
