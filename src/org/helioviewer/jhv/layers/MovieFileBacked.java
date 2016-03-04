package org.helioviewer.jhv.layers;

import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.opengl.Texture;
import org.helioviewer.jhv.viewmodel.TimeLine.DecodeQualityLevel;
import org.helioviewer.jhv.viewmodel.jp2view.kakadu.KakaduUtils;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.metadata.MetaDataFactory;
import org.helioviewer.jhv.viewmodel.metadata.UnsuitableMetaDataException;
import org.w3c.dom.Document;

import com.google.common.io.Files;

import kdu_jni.Jp2_threadsafe_family_src;
import kdu_jni.Jpx_codestream_source;
import kdu_jni.Jpx_input_box;
import kdu_jni.Jpx_layer_source;
import kdu_jni.Jpx_source;
import kdu_jni.KduException;
import kdu_jni.Kdu_cache;
import kdu_jni.Kdu_channel_mapping;
import kdu_jni.Kdu_codestream;
import kdu_jni.Kdu_coords;
import kdu_jni.Kdu_dims;
import kdu_jni.Kdu_global;
import kdu_jni.Kdu_quality_limiter;
import kdu_jni.Kdu_region_decompressor;

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
