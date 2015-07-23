package org.helioviewer.jhv.gui.dialogs;

import java.io.File;

import javafx.stage.FileChooser.ExtensionFilter;

import javax.swing.filechooser.FileFilter;

import com.xuggle.xuggler.ICodec;

public class MovieFileFilter extends FileFilter{
	public enum ImplementedMovieFilter{	
		MP4("*.mp4","MPEG-4 (.mp4)", MovieTypes.MP4, ICodec.ID.CODEC_ID_MPEG4), 
		MOV("*.mov", "Quicktime (.mov)", MovieTypes.MOV, ICodec.ID.CODEC_ID_MPEG4), 
		JPG("*.jpg", "Set of JPG images (.jpg)", ImageTypes.JPG, "jpg"), 
		JPEG("*.jpeg", "Set of JPEG images (.jpeg)", ImageTypes.JPEG, "jpeg"), 
		PNG("*.png", "Set of PNG images (.png)", ImageTypes.PNG, "png"), 
		ZIP_JPG("*.zip", "ZIP archive of JPG images (.zip)", CompressedTypes.ZIP, JPG),
		ZIP_JPEG("*.zip", "ZIP archive of JPEG images (.zip)", CompressedTypes.ZIP, JPEG), 
		ZIP_PNG("*.zip", "ZIP archive of PNG images (.zip)", CompressedTypes.ZIP, PNG);
		
		private MovieFileFilter movieFilter;
		
		private ImplementedMovieFilter(String extension, String description, MovieTypes movieType, ICodec.ID codec){
			movieFilter = new MovieFileFilter(extension, description, movieType, codec);
		}
		
		private ImplementedMovieFilter(String extension, String description , CompressedTypes compressedType, ImplementedMovieFilter innerMovieFilter){
			movieFilter = new MovieFileFilter(extension, description, compressedType, innerMovieFilter);
		}
	
		private ImplementedMovieFilter(String extension, String description, ImageTypes imageType, String fileType){
			movieFilter = new MovieFileFilter(extension, description, imageType, fileType);
		}
		
		public MovieFileFilter getMovieFilter(){
			return this.movieFilter;
		}
		
		public ExtensionFilter getExtensionFilter(){
			System.out.println("ext : " + movieFilter.extension);
			return new ExtensionFilter(movieFilter.description, movieFilter.extension);
		}

		public boolean isEqual(ExtensionFilter selectedExtensionFilter) {
			return selectedExtensionFilter.getExtensions().equals(movieFilter.extension) && selectedExtensionFilter.getDescription().equals(movieFilter.description);
		}
	}
	
	public enum MovieTypes{
		MOV, MP4;
	}
	
	public enum CompressedTypes{
		ZIP;
	}
	
	public enum ImageTypes{
		JPG, JPEG, PNG;
	}

	private String extension;
	private String description;
	private String fileType;
	
	private ICodec.ID codec = null;
	private ImageTypes imageType = null;
	private CompressedTypes compressedType = null;
	private ImplementedMovieFilter innerMovieFilter = null;
	private MovieTypes movieType = null;
	
	private boolean isMovieFile = false;
	private boolean isImageFile = false;
	private boolean isCompressedFile = false;
	
	private MovieFileFilter(String extension, String description, MovieTypes movieType, ICodec.ID codec){
		this.extension = extension;
		this.description = description;
		this.movieType = movieType;
		this.codec = codec;
		this.isMovieFile = true;
	}
	
	private MovieFileFilter(String extension, String description , CompressedTypes compressedType, ImplementedMovieFilter innerMovieFilter){
		this.extension = extension;
		this.description = description;
		this.compressedType = compressedType;
		this.innerMovieFilter = innerMovieFilter;
		this.isCompressedFile = true;
		
	}

	private MovieFileFilter(String extension, String description, ImageTypes imageType, String fileType){
		this.extension = extension;
		this.description = description;
		this.imageType = imageType;
		this.isImageFile = true;
		this.fileType = fileType;
	}

	public String getExtension(){
		return this.extension.substring(1);
	}
	
	public ICodec.ID getCodec(){
		return this.codec;
	}
	
	public ImageTypes getImageType(){
		return this.imageType;
	}
	
	public CompressedTypes getCompressedType(){
		return this.compressedType;
	}
	
	public MovieTypes getMovieType(){
		return this.movieType;
	}

	public boolean isMovieFile() {
		return isMovieFile;
	}

	public boolean isImageFile() {
		return isImageFile;
	}

	public boolean isCompressedFile() {
		return isCompressedFile;
	}

	public String getFileType() {
		return this.fileType;
	}
	
	public MovieFileFilter getInnerMovieFilter(){
		return this.innerMovieFilter.getMovieFilter();
	}
	
	@Override
	public boolean accept(File pathname) {
        return pathname.getName().toLowerCase().endsWith(this.extension);
	}
	
	public String getDescription(){
		return this.description;
	}


}


  