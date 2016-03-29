package org.helioviewer.jhv.gui;

import java.io.File;

import javax.annotation.Nullable;
import javax.swing.filechooser.FileFilter;

import com.xuggle.xuggler.ICodec;

import javafx.stage.FileChooser.ExtensionFilter;

public class PredefinedFileFilter extends FileFilter
{
	public static final PredefinedFileFilter MP4 = new PredefinedFileFilter(".mp4","MPEG-4 (*.mp4)", MovieTypes.MP4, ICodec.ID.CODEC_ID_MPEG4);
	public static final PredefinedFileFilter MOV = new PredefinedFileFilter(".mov", "Quicktime (*.mov)", MovieTypes.MOV, ICodec.ID.CODEC_ID_MPEG4);
	public static final PredefinedFileFilter JPG_SET = new PredefinedFileFilter(".jpg", "Set of JPG images (*.jpg)", ImageTypes.JPG, "jpg");
	public static final PredefinedFileFilter PNG_SET = new PredefinedFileFilter(".png", "Set of PNG images (*.png)", ImageTypes.PNG, "png");
	public static final PredefinedFileFilter ZIP_JPG = new PredefinedFileFilter(".zip", "ZIP archive of JPG images (*.zip)", CompressedTypes.ZIP, JPG_SET);
	public static final PredefinedFileFilter ZIP_PNG = new PredefinedFileFilter(".zip", "ZIP archive of PNG images (*.zip)", CompressedTypes.ZIP, PNG_SET);
	public static final PredefinedFileFilter JPG_SINGLE = new PredefinedFileFilter("JPG files (*.jpg, *.jpeg)", new String[]{ ".jpg", ".jpeg" });
	public static final PredefinedFileFilter PNG_SINGLE = new PredefinedFileFilter("PNG files (*.png)", new String[]{ ".png" });
	public static final PredefinedFileFilter JP2 = new PredefinedFileFilter("JPG2000 files (*.jp2, *.jpx)", new String[] { ".jp2", ".jpx" });
	public static final PredefinedFileFilter FITS = new PredefinedFileFilter("FITS files (*.fts, *.fits)", new String[] { ".fits", ".fts" });
	public static final PredefinedFileFilter XML = new PredefinedFileFilter("XML files (*.xml)", new String[] {".xml"});
	public static final PredefinedFileFilter JPX = new PredefinedFileFilter("JPG2000 files (.jpx)", new String[]{ ".jpx" });
	public static final PredefinedFileFilter JHV = new PredefinedFileFilter("JHelioviewer state files (*.jhv)", new String[]{ ".jhv" });
	
    public static final PredefinedFileFilter ALL_SUPPORTED_IMAGE_TYPES = new PredefinedFileFilter(
    				"All supported files (*.jpg, *.jpeg, *.png, *.fts, *.fits, *.jp2, *.jpx)",
    				new String[] { ".jpg", ".jpeg", ".png", ".fts", ".fits", ".jp2", ".jpx" }
    			);

	public static final PredefinedFileFilter[] SaveMovieFileFilter=new PredefinedFileFilter[]{MP4,MOV,JPG_SET,PNG_SET,ZIP_JPG,ZIP_PNG};
		
	enum MovieTypes
	{	
		MOV, MP4
	}
	
	enum CompressedTypes
	{
		ZIP
	}
	
	enum ImageTypes
	{
		JPG, JPEG, PNG
	}
	
    @Override
    public boolean accept(@Nullable File f)
    {
    	if(f==null)
    		return false;
    	
        if (f.isDirectory())
            return true;

        String testName = f.getName().toLowerCase();
        for (String ext : extensions)
            if (testName.endsWith(ext))
                return true;

        return false;
    }

    /**
     * Returns the default extensions of this file filter.
     * 
     * By default, the first element of the list of accepted extensions will be
     * considered as the default extension.
     * 
     * @return The default extension
     */
    public String getDefaultExtension()
    {
    	return extensions[0];
    }


    private final String[] extensions;
	public final @Nullable String description;
	public final @Nullable String fileType;
	
	public final @Nullable ICodec.ID codec;
	public final @Nullable ImageTypes imageType;
	public final @Nullable CompressedTypes compressedType;
	private final @Nullable PredefinedFileFilter innerFileFilter;
	public final @Nullable MovieTypes movieType;
	
	public final ExtensionFilter extensionFilter;
	
	private PredefinedFileFilter(String _extension, String _description, MovieTypes _movieType, ICodec.ID _codec)
	{
		extensions = new String[] { _extension };
		description = _description;
		movieType = _movieType;
		codec = _codec;
		fileType = null;
		imageType=null;
		compressedType=null;
		innerFileFilter = null;
		extensionFilter = new ExtensionFilter(description, prependStar(extensions));
	}
	
	private String[] prependStar(String[] _in)
	{
		String[] out=new String[_in.length];
		for(int i=0;i<_in.length;i++)
			out[i]="*"+_in[i];
		return out;
	}
	
	private PredefinedFileFilter(String _extension, String _description, CompressedTypes _compressedType, PredefinedFileFilter _innerMovieFilter)
	{
		extensions = new String[] { _extension };
		description = _description;
		compressedType = _compressedType;
		innerFileFilter = _innerMovieFilter;
		fileType=null;
		movieType=null;
		codec=null;
		imageType=null;
		extensionFilter = new ExtensionFilter(description, prependStar(extensions));
	}

	private PredefinedFileFilter(String _extension, String _description, ImageTypes _imageType, String _fileType)
	{
		extensions = new String[] { _extension };
		description = _description;
		imageType = _imageType;
		fileType = _fileType;
		innerFileFilter=null;
		movieType=null;
		codec=null;
		compressedType=null;
		extensionFilter = new ExtensionFilter(description, prependStar(extensions));
	}

	public PredefinedFileFilter(String _description, String[] _extensions)
	{
		extensions = _extensions;
		description = _description;
		imageType = null;
		fileType = null;
		innerFileFilter=null;
		movieType=null;
		codec=null;
		compressedType=null;
		extensionFilter = new ExtensionFilter(description, prependStar(extensions));
	}

	public boolean isMovieFile()
	{
		return movieType!=null;
	}

	public boolean isImageFile()
	{
		return imageType!=null;
	}

	public boolean isCompressedFile()
	{
		return compressedType!=null;
	}

	public @Nullable PredefinedFileFilter getInnerMovieFilter()
	{
		return innerFileFilter;
	}

	public @Nullable String getDescription()
	{
		return description;
	}
}


  