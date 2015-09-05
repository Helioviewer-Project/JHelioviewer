package org.helioviewer.jhv.layers;

import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.helioviewer.jhv.JHVException;
import org.helioviewer.jhv.JHVException.MetaDataException;
import org.helioviewer.jhv.JHVException.TextureException;
import org.helioviewer.jhv.base.FutureValue;
import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.base.downloadmanager.AbstractDownloadRequest;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.opengl.MainPanel;
import org.helioviewer.jhv.layers.filter.LUT;
import org.helioviewer.jhv.opengl.OpenGLHelper;
import org.helioviewer.jhv.opengl.TextureCache;
import org.helioviewer.jhv.opengl.camera.CameraMode;
import org.helioviewer.jhv.viewmodel.TimeLine;
import org.helioviewer.jhv.viewmodel.jp2view.newjpx.Cache;
import org.helioviewer.jhv.viewmodel.jp2view.newjpx.KakaduRender;
import org.helioviewer.jhv.viewmodel.jp2view.newjpx.UltimateLayer;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.json.JSONException;
import org.json.JSONObject;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

public class ImageLayer extends AbstractImageLayer
{
	private static final String IS_LOCALFILE = "isLocalFile";
	private static final String LOCAL_PATH = "localPath";
	private static final String ID = "id";
	private static final String CADENCE = "cadence";
	private static final String START_DATE_TIME = "startDateTime";
	private static final String END_DATE_TIME = "endDateTime";
	private static final String NAME = "name";

	private static final ExecutorService exDecoder = Executors.newWorkStealingPool();

	private LayerRayTrace layerRayTrace;
	private int sourceID;
	
	private UltimateLayer ultimateLayer;

	private static int shaderprogram = -1;

	public ImageLayer(int sourceID, LocalDateTime start, LocalDateTime end, int cadence, String name)
	{
		super();
		this.sourceID = sourceID;
		this.start = start;
		this.end = end;
		this.cadence = cadence;
		this.name = name;
		this.isDownloadable = ChronoUnit.SECONDS.between(start, end) / cadence < 1000;
		this.ultimateLayer = new UltimateLayer(id, sourceID, this);
		this.ultimateLayer.setTimeRange(start, end, cadence);
		layerRayTrace = new LayerRayTrace(this);
	}

	public ImageLayer(String uri, KakaduRender newRender)
	{
		super();
		this.localPath = uri;
		this.ultimateLayer = new UltimateLayer(id, uri, newRender, this);
		this.name = ultimateLayer.getMetaData(0, uri).getFullName();
		layerRayTrace = new LayerRayTrace(this);

		start = ultimateLayer.getLocalDateTimes().first();
		end = ultimateLayer.getLocalDateTimes().last();
		this.cadence = (int) (ChronoUnit.SECONDS.between(start, end) / ultimateLayer.getLocalDateTimes().size());
	}

	@Override
	public int getTexture(MainPanel compenentView, ByteBuffer _imageData, Dimension size) throws TextureException
	{
		//FIXME: is _imageData ever null?!
		if (_imageData != null)
		{
			OpenGLHelper.bindByteBufferToGLTexture(this.getLastDecodedImageRegion(), _imageData, this.getLastDecodedImageRegion().getImageSize());
		}

		if (this.getLastDecodedImageRegion() != null)
			return getLastDecodedImageRegion().getTextureID();

		return -1;
	}

	@Override
	public LocalDateTime getTime()
	{
		try
		{
			if (getMetaData(TimeLine.SINGLETON.getCurrentDateTime()) != null)
				return getMetaData(TimeLine.SINGLETON.getCurrentDateTime()).getLocalDateTime();
		}
		catch (MetaDataException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	@Deprecated
	public ConcurrentSkipListSet<LocalDateTime> getLocalDateTime()
	{
		return ultimateLayer.getLocalDateTimes();
	}

	@Override
	protected MetaData getMetaData() throws JHVException.MetaDataException
	{
		if (getLastDecodedImageRegion() == null	|| getLastDecodedImageRegion().getMetaData() == null)
			throw new JHVException.MetaDataException("No imagedata available");
		return this.getLastDecodedImageRegion().getMetaData();
	}

	public ImageRegion getLastDecodedImageRegion()
	{
		return ultimateLayer.getImageRegion();
	}

	@Override
	public String getURL()
	{
		return ultimateLayer.getURL();
	}

	@Override
	public MetaData getMetaData(LocalDateTime currentDateTime) throws MetaDataException
	{
		MetaData metaData = null;
		if (getMetaData().getLocalDateTime().isEqual(currentDateTime) && getLastDecodedImageRegion().getID() == this.id)
			return getMetaData();
		metaData = ultimateLayer.getMetaData(currentDateTime);
		if (metaData == null)
			throw new JHVException.MetaDataException("No metadata available");

		return metaData;
	}

	public void writeStateFile(JSONObject jsonLayer)
	{
		try
		{
			jsonLayer.put(IS_LOCALFILE, ultimateLayer.isLocalFile());
			jsonLayer.put(LOCAL_PATH, getLocalFilePath());
			jsonLayer.put(ID, sourceID);
			jsonLayer.put(CADENCE, cadence);
			jsonLayer.put(START_DATE_TIME, start);
			jsonLayer.put(END_DATE_TIME, end);
			jsonLayer.put(NAME, name);
			super.writeStateFile(jsonLayer);
		}
		catch (JSONException e)
		{
			
			e.printStackTrace();
		}
	}

	public static ImageLayer readStateFile(JSONObject jsonLayer, KakaduRender kakaduRender)
	{
		try
		{
			if (jsonLayer.getBoolean(IS_LOCALFILE))
			{
				return new ImageLayer(jsonLayer.getString(LOCAL_PATH), kakaduRender);
			}
			else if (jsonLayer.getInt(CADENCE) >= 0)
			{
				LocalDateTime start = LocalDateTime.parse(jsonLayer.getString(START_DATE_TIME));
				LocalDateTime end = LocalDateTime.parse(jsonLayer.getString(END_DATE_TIME));
				return new ImageLayer(jsonLayer.getInt(ID), start, end, jsonLayer.getInt(CADENCE), jsonLayer.getString(NAME));
			}
		}
		catch (JSONException e)
		{
			
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public CacheableImageData getCacheStatus(LocalDateTime localDateTime)
	{
		return Cache.getCacheElement(id, localDateTime);
	}

	public RenderResult renderLayer(GL2 gl, Dimension canvasSize, MainPanel mainPanel, ByteBuffer _imageData)
	{
		if (shaderprogram < 0)
			initShaders(gl);
		
		try
		{
			int layerTexture = getTexture(mainPanel, _imageData, canvasSize);
			if (layerTexture < 0)
				return RenderResult.RETRY_LATER;
			
			LocalDateTime currentDateTime = TimeLine.SINGLETON.getCurrentDateTime();
			Rectangle2D physicalSize = getMetaData(currentDateTime).getPhysicalImageSize();
			if (physicalSize.getWidth() <= 0 || physicalSize.getHeight() <= 0)
				return RenderResult.RETRY_LATER;

			MetaData metaData = getMetaData(currentDateTime);
			float xSunOffset = (float) ((metaData.getSunPixelPosition().x - metaData
					.getResolution().getWidth() / 2.0) / (float) metaData
					.getResolution().getWidth());
			float ySunOffset = -(float) ((metaData.getSunPixelPosition().y - metaData
					.getResolution().getHeight() / 2.0) / (float) metaData
					.getResolution().getHeight());

			Vector3d currentPos = mainPanel.getRotation().toMatrix().multiply(new Vector3d(0, 0, 1));
			Vector3d startPos = metaData.getRotation().toMatrix().multiply(new Vector3d(0, 0, 1));

			double angle = Math.toDegrees(Math.acos(currentPos.dot(startPos)));
			double maxAngle = 60;
			double minAngle = 30;
			float opacityCorona = (float) ((Math.abs(90 - angle) - minAngle) / (maxAngle - minAngle));
			opacityCorona = opacityCorona > 1 ? 1f : opacityCorona;
			if (!Layers.getCoronaVisibility())
				opacityCorona = 0;
			gl.glEnable(GL2.GL_DEPTH_TEST);
			gl.glDepthFunc(GL2.GL_LEQUAL);
			gl.glDepthMask(true);
			gl.glColor4f(1, 1, 1, 1);
			gl.glEnable(GL2.GL_BLEND);
			gl.glEnable(GL2.GL_TEXTURE_2D);
			gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE);
			gl.glActiveTexture(GL.GL_TEXTURE0);
			gl.glBindTexture(GL2.GL_TEXTURE_2D, layerTexture);

			gl.glEnable(GL2.GL_VERTEX_PROGRAM_ARB);
			gl.glEnable(GL2.GL_FRAGMENT_PROGRAM_ARB);

			gl.glUseProgram(shaderprogram);

			gl.glActiveTexture(GL.GL_TEXTURE1);
			gl.glBindTexture(GL2.GL_TEXTURE_2D, LUT.getTexture());

			gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "texture"), 0);
			gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "lut"), 1);
			gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "fov"), (float) Math.toRadians(MainPanel.FOV / 2.0));
			gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "physicalImageWidth"), (float) metaData
					.getPhysicalImageWidth());
			gl.glUniform2f(gl.glGetUniformLocation(shaderprogram, "sunOffset"), xSunOffset, ySunOffset);
			gl.glUniform4f(
					gl.glGetUniformLocation(shaderprogram, "imageOffset"),
					getLastDecodedImageRegion().getTextureOffsetX(),
					getLastDecodedImageRegion().getTextureOffsetY(),
					getLastDecodedImageRegion().getTextureScaleWidth(),
					getLastDecodedImageRegion().getTextureScaleHeight());
			gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "opacity"), (float) opacity);
			gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "gamma"), (float) gamma);
			gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "sharpen"), (float) sharpness);
			gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "lutPosition"), getLut().ordinal());
			gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "lutInverted"), invertedLut ? 1:0);
			gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "redChannel"), redChannel ? 1:0);
			gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "greenChannel"), greenChannel ? 1:0);
			gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "blueChannel"), blueChannel ? 1:0);
			gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "opacityCorona"), opacityCorona);
			gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "cameraMode"), CameraMode.getCameraMode());
			gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "contrast"), (float) contrast);

			float clipNear = (float) Math.max(mainPanel.getTranslation().z - 4 * Constants.SUN_RADIUS, MainPanel.CLIP_NEAR);
			gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "near"), clipNear);
			gl.glUniform1f(
					gl.glGetUniformLocation(shaderprogram, "far"),
					(float) (mainPanel.getTranslation().z + 4 * Constants.SUN_RADIUS));
			float[] transformation = mainPanel.getTransformation().toFloatArray();
			gl.glUniformMatrix4fv(gl.glGetUniformLocation(shaderprogram, "transformation"), 1, true, transformation, 0);

			float[] layerTransformation = getMetaData(currentDateTime).getRotation().toMatrix().toFloatArray();
			gl.glUniformMatrix4fv(gl.glGetUniformLocation(shaderprogram, "layerTransformation"), 1, true, layerTransformation, 0);
			
			float[] layerInv = getMetaData(currentDateTime).getRotation().inversed().toMatrix().toFloatArray();
			gl.glUniformMatrix4fv(gl.glGetUniformLocation(shaderprogram, "layerInv"), 1, true, layerInv, 0);

			gl.glUniform2f(
					gl.glGetUniformLocation(shaderprogram, "imageResolution"),
					getLastDecodedImageRegion().textureHeight,
					getLastDecodedImageRegion().textureHeight);

			gl.glBegin(GL2.GL_QUADS);

			gl.glTexCoord2f(0.0f, 1.0f);
			gl.glVertex2d(-1, -1);
			gl.glTexCoord2f(1.0f, 1.0f);
			gl.glVertex2d(1, -1);
			gl.glTexCoord2f(1.0f, 0.0f);
			gl.glVertex2d(1, 1);
			gl.glTexCoord2f(0.0f, 0.0f);
			gl.glVertex2d(-1, 1);

			gl.glEnd();
			gl.glUseProgram(0);
			// gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
			gl.glActiveTexture(GL.GL_TEXTURE0);
			// gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
			gl.glDisable(GL2.GL_BLEND);
			gl.glDisable(GL2.GL_TEXTURE_2D);
			gl.glDisable(GL2.GL_FRAGMENT_PROGRAM_ARB);
			gl.glDisable(GL2.GL_VERTEX_PROGRAM_ARB);
			gl.glEnable(GL2.GL_DEPTH_TEST);
			gl.glDepthMask(false);
			return RenderResult.OK;
		}
		catch (MetaDataException | TextureException e)
		{
			return RenderResult.RETRY_LATER;
		}

	}

	private void initShaders(GL2 gl)
	{
		int vertexShader = gl.glCreateShader(GL2.GL_VERTEX_SHADER);
		int fragmentShader = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);

		String vertexShaderSrc = OpenGLHelper.loadShaderFromFile("/shader/MainVertex.glsl");
		String fragmentShaderSrc = OpenGLHelper.loadShaderFromFile("/shader/MainFragment.glsl");

		gl.glShaderSource(vertexShader, 1, new String[] { vertexShaderSrc }, (int[]) null, 0);
		gl.glCompileShader(vertexShader);

		gl.glShaderSource(fragmentShader, 1, new String[] { fragmentShaderSrc }, (int[]) null, 0);
		gl.glCompileShader(fragmentShader);

		shaderprogram = gl.glCreateProgram();
		gl.glAttachShader(shaderprogram, vertexShader);
		gl.glAttachShader(shaderprogram, fragmentShader);
		gl.glLinkProgram(shaderprogram);
		gl.glValidateProgram(shaderprogram);

		IntBuffer intBuffer = IntBuffer.allocate(1);
		gl.glGetProgramiv(shaderprogram, GL2.GL_LINK_STATUS, intBuffer);
		if (intBuffer.get(0) != 1)
		{
			gl.glGetProgramiv(shaderprogram, GL2.GL_INFO_LOG_LENGTH, intBuffer);
			int size = intBuffer.get(0);
			System.err.println("Program link error: ");
			if (size > 0)
			{
				ByteBuffer byteBuffer = ByteBuffer.allocate(size);
				gl.glGetProgramInfoLog(shaderprogram, size, intBuffer, byteBuffer);
				for (byte b : byteBuffer.array())
					System.err.print((char) b);

			}
			else
			{
				System.out.println("Unknown");
			}
			throw new RuntimeException("Could not compile shader.");
		}
		gl.glUseProgram(0);
	}

	@Override
	public String getFullName()
	{
		try
		{
			return getMetaData(TimeLine.SINGLETON.getCurrentDateTime()).getFullName();
		}
		catch (MetaDataException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	@Override
	void remove()
	{
		ultimateLayer.cancelAllDownloadsForThisLayer();
	}

	@Override
	public void retryFailedRequests()
	{
		AbstractDownloadRequest[] requests;
		synchronized(failedRequests)
		{
			requests = new AbstractDownloadRequest[failedRequests.size()];
			failedRequests.toArray(requests);
			failedRequests.clear();
		}
		ultimateLayer.retryFailedRequests(requests);
		MainFrame.LAYER_PANEL.repaintPanel();
	}

	@Override
	public LocalDateTime getFirstLocalDateTime()
	{
		return start;
	}

	@Override
	public LocalDateTime getLastLocalDateTime()
	{
		return end;
	}

	@Override
	public Future<ByteBuffer> prepareImageData(final MainPanel mainPanel, final Dimension size) throws MetaDataException
	{
		final LocalDateTime currentDateTime = TimeLine.SINGLETON.getCurrentDateTime();
		final MetaData metaData = ultimateLayer.getMetaData(currentDateTime);
		if (metaData == null)
			throw new MetaDataException();
		
		if(lut==null)
			lut=metaData.getDefaultLUT();
		
		final ImageRegion imageRegion = layerRayTrace.getCurrentRegion(mainPanel, metaData, size);
		if (imageRegion.getImageSize().getWidth() < 0 || imageRegion.getImageSize().getHeight() < 0)
			return new FutureValue<ByteBuffer>(null);
		
		LocalDateTime nextLocalDateTime = ultimateLayer.getNextLocalDateTime(currentDateTime);
		if (nextLocalDateTime == null)
			nextLocalDateTime = ultimateLayer.localDateTimes.last();
		
		ImageRegion cachedRegion = TextureCache.search(id, imageRegion, nextLocalDateTime);
		if(cachedRegion != null)
		{
			ultimateLayer.imageRegion = cachedRegion;
			return new FutureValue<ByteBuffer>(null);
		}
		
		final LocalDateTime finalNextLocalDateTime = nextLocalDateTime;
		return exDecoder.submit(new Callable<ByteBuffer>()
		{
			@Override
			public ByteBuffer call() throws Exception
			{
				return ultimateLayer.getImageData(finalNextLocalDateTime, imageRegion, metaData);
			}
		});
	}
}
