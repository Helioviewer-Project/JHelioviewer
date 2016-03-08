package org.helioviewer.jhv.layers;

import java.io.FileInputStream;
import java.io.IOException;

import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.metadata.UnsuitableMetaDataException;

import kdu_jni.Jpx_layer_source;
import kdu_jni.Jpx_source;
import kdu_jni.KduException;

public class MovieFileBacked extends Movie
{
	public final String filename;

	public MovieFileBacked(int _sourceId, String _filename) throws IOException, KduException
	{
		super(_sourceId);
		filename = _filename;
		
		Jpx_source jpxSrc = new Jpx_source();
		jpxSrc.Open(family_src, true);
		
		Jpx_layer_source l = jpxSrc.Access_layer(0);
		if(!l.Exists())
			throw new KduException("File contains no layers");
			
		//count frames
		int[] frameCount = new int[1];
		jpxSrc.Count_compositing_layers(frameCount);
		
		jpxSrc.Close();
		jpxSrc.Native_destroy();
		
		metaDatas = new MetaData[frameCount[0]];
		
		try(FileInputStream fis = new FileInputStream(filename))
		{
			try
			{
				family_src.Open(filename);
			}
			catch (KduException e)
			{
				Telemetry.trackException(e);
			}
		}
		
		//load timestamps
		for(int i=0;i<metaDatas.length;i++)
		{
			MetaData md = getMetaData(i);
			if(md!=null)
				timeMS[i]=md.timeMS;
		}
		
		if(getAnyMetaData()==null)
			throw new UnsuitableMetaDataException();
	}
	
	public boolean isFullQuality()
	{
		return true;
	}
	
	public boolean isBetterQualityThan(Movie _other)
	{
		return true;
	}
}
