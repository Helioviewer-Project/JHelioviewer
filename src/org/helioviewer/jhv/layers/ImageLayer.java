package org.helioviewer.jhv.layers;

import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.NavigableSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.helioviewer.jhv.Telemetry;
import org.helioviewer.jhv.base.FutureValue;
import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.base.downloadmanager.AbstractDownloadRequest;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.MainPanel;
import org.helioviewer.jhv.layers.LUT.Lut;
import org.helioviewer.jhv.layers.Movie.Match;
import org.helioviewer.jhv.opengl.OpenGLHelper;
import org.helioviewer.jhv.opengl.RayTrace;
import org.helioviewer.jhv.opengl.TextureCache;
import org.helioviewer.jhv.opengl.camera.CameraMode;
import org.helioviewer.jhv.viewmodel.TimeLine;
import org.helioviewer.jhv.viewmodel.jp2view.newjpx.MovieCache;
import org.helioviewer.jhv.viewmodel.jp2view.newjpx.UltimateLayer;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.json.JSONException;
import org.json.JSONObject;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;

public class ImageLayer extends AbstractLayer
{
	public enum CacheStatus
	{
		FILE_FULL, KDU_PREVIEW, NONE;
	}

	public double opacity = 1;
	public double sharpness = 0;
	public double gamma = 1;
	public double contrast = 1;
	protected Lut lut = null;
	public boolean redChannel = true;
	public boolean greenChannel = true;
	public boolean blueChannel = true;
	private boolean visible = true;
	public boolean invertedLut = false;
	protected boolean coronaVisible = true;

	protected LocalDateTime start;
	protected LocalDateTime end;
	protected int cadence = -1;
	protected String localPath;

	public boolean isVisible()
	{
		return visible;
	}
	
	public void setVisible(boolean _visible)
	{
		visible = _visible;
		MainFrame.MAIN_PANEL.repaint();
	}
	
	public Lut getLut()
	{
		return lut;
	}

	public void setLut(Lut _lutEntry)
	{
		lut = _lutEntry;
		MainFrame.FILTER_PANEL.update();
	}

	public String getLocalFilePath()
	{
		return localPath;
	}

	public void readStateFile(JSONObject jsonLayer)
	{
		try
		{
			this.opacity = jsonLayer.getDouble("opa.city");
			this.sharpness = jsonLayer.getDouble("sharpen");
			this.gamma = jsonLayer.getDouble("gamma");
			this.contrast = jsonLayer.getDouble("contrast");
			setLut(Lut.values()[jsonLayer.getInt("lut")]);
			redChannel=jsonLayer.getBoolean("redChannel");
			greenChannel=jsonLayer.getBoolean("greenChannel");
			blueChannel=jsonLayer.getBoolean("blueChannel");
			
			visible = jsonLayer.getBoolean("visibility");
			invertedLut = jsonLayer.getBoolean("invertedLut");
			coronaVisible=jsonLayer.getBoolean("coronaVisiblity");
			MainFrame.FILTER_PANEL.update();
		}
		catch (JSONException e)
		{
			Telemetry.trackException(e);
		}
	}
	
	public int getCadence()
	{
		return cadence;
	}

	public void toggleCoronaVisibility()
	{
		coronaVisible=!coronaVisible;
	}
	
	private static final ExecutorService exDecoder = Executors.newWorkStealingPool();

	private int sourceId;
	
	private UltimateLayer ultimateLayer;

	private static int shaderprogram = -1;

	public ImageLayer(int _sourceId, LocalDateTime _start, LocalDateTime _end, int _cadence, String _name)
	{
		sourceId = _sourceId;
		start = _start;
		end = _end;
		cadence = _cadence;
		name = _name;
		isDownloadable = ChronoUnit.SECONDS.between(_start, _end) / _cadence < 1000;
		
		ultimateLayer = new UltimateLayer(_sourceId,this);
		ultimateLayer.setTimeRange(_start, _end, _cadence);
	}

	private static int freeSourceId=0;
	
	public ImageLayer(String _filePath)
	{
		localPath = _filePath;
		ultimateLayer = new UltimateLayer(this, --freeSourceId, _filePath);
		name = ultimateLayer.getMetaData(0).getFullName();
		
		start = ultimateLayer.getLocalDateTimes().first();
		end = ultimateLayer.getLocalDateTimes().last();
		cadence = (int) (ChronoUnit.SECONDS.between(start, end) / ultimateLayer.getLocalDateTimes().size());
	}

	public int getTexture(MainPanel compenentView, ByteBuffer _imageData, Dimension size)
	{
		//upload new texture, if something was decoded
		if (_imageData != null)
			OpenGLHelper.bindByteBufferToGLTexture(getLastDecodedImageRegion(), _imageData, getLastDecodedImageRegion().getImageSize());
		
		if (getLastDecodedImageRegion() != null)
			return getLastDecodedImageRegion().getTextureID();

		return -1;
	}

	@Override
	public LocalDateTime getTime()
	{
		return ultimateLayer.getClosestLocalDateTime(TimeLine.SINGLETON.getCurrentDateTime());
	}

	@Deprecated
	public NavigableSet<LocalDateTime> getLocalDateTime()
	{
		return ultimateLayer.getLocalDateTimes();
	}

	//FIXME: get rid of
	public ImageRegion getLastDecodedImageRegion()
	{
		return ultimateLayer.getImageRegion();
	}

	@Override
	public String getURL()
	{
		return ultimateLayer.getURL();
	}

	public MetaData getMetaData(LocalDateTime currentDateTime)
	{
		return ultimateLayer.getMetaData(currentDateTime);
	}

	public void writeStateFile(JSONObject jsonLayer)
	{
		try
		{
			jsonLayer.put("isLocalFile", ultimateLayer.isLocalFile());
			jsonLayer.put("localPath", getLocalFilePath());
			jsonLayer.put("id", sourceId);
			jsonLayer.put("cadence", cadence);
			jsonLayer.put("startDateTime", start);
			jsonLayer.put("endDateTime", end);
			jsonLayer.put("name", name);
			jsonLayer.put("opa.city", opacity);
			jsonLayer.put("sharpen", sharpness);
			jsonLayer.put("gamma", gamma);
			jsonLayer.put("contrast", contrast);
			jsonLayer.put("lut", getLut().ordinal());
			jsonLayer.put("redChannel", redChannel);
			jsonLayer.put("greenChannel", greenChannel);
			jsonLayer.put("blueChannel", blueChannel);

			jsonLayer.put("visibility", isVisible());
			jsonLayer.put("invertedLut", invertedLut);
			jsonLayer.put("coronaVisiblity", coronaVisible);
		}
		catch (JSONException e)
		{
			Telemetry.trackException(e);
		}
	}

	public static ImageLayer createFromStateFile(JSONObject jsonLayer)
	{
		try
		{
			if (jsonLayer.getBoolean("isLocalFile"))
			{
				return new ImageLayer(jsonLayer.getString("localPath"));
			}
			else if (jsonLayer.getInt("cadence") >= 0)
			{
				LocalDateTime start = LocalDateTime.parse(jsonLayer.getString("startDateTime"));
				LocalDateTime end = LocalDateTime.parse(jsonLayer.getString("endDateTime"));
				return new ImageLayer(jsonLayer.getInt("id"), start, end, jsonLayer.getInt("cadence"), jsonLayer.getString("name"));
			}
		}
		catch (JSONException e)
		{
			Telemetry.trackException(e);
		}
		return null;
	}

	public Match getMovie(LocalDateTime _currentDateTime)
	{
		return MovieCache.findBestFrame(sourceId, _currentDateTime);
	}

	public RenderResult renderLayer(GL2 gl, Dimension canvasSize, MainPanel mainPanel, ByteBuffer _imageData)
	{
		int layerTexture = getTexture(mainPanel, _imageData, canvasSize);
		if (layerTexture < 0)
			return RenderResult.RETRY_LATER;
		
		LocalDateTime currentDateTime = TimeLine.SINGLETON.getCurrentDateTime();
		
		MetaData md=getMetaData(currentDateTime);
		if(md==null)
			return RenderResult.RETRY_LATER;
		
		Rectangle2D physicalSize = md.getPhysicalImageSize();
		if (physicalSize.getWidth() <= 0 || physicalSize.getHeight() <= 0)
			return RenderResult.RETRY_LATER;

		float xSunOffset = (float) ((md.getSunPixelPosition().x - md
				.getResolution().getWidth() / 2.0) / (float) md
				.getResolution().getWidth());
		float ySunOffset = -(float) ((md.getSunPixelPosition().y - md
				.getResolution().getHeight() / 2.0) / (float)md
				.getResolution().getHeight());

		Vector3d currentPos = mainPanel.getRotation().toMatrix().multiply(new Vector3d(0, 0, 1));
		Vector3d startPos = md.getRotation().toMatrix().multiply(new Vector3d(0, 0, 1));

		double angle = Math.toDegrees(Math.acos(currentPos.dot(startPos)));
		double maxAngle = 60;
		double minAngle = 30;
		float opacityCorona = (float) ((Math.abs(90 - angle) - minAngle) / (maxAngle - minAngle));
		opacityCorona = opacityCorona > 1 ? 1f : opacityCorona;
		if (!coronaVisible)
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
		gl.glBindTexture(GL2.GL_TEXTURE_2D, org.helioviewer.jhv.layers.LUT.getTexture());

		gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "texture"), 0);
		gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "lut"), 1);
		gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "fov"), (float) Math.toRadians(MainPanel.FOV / 2.0));
		gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "physicalImageWidth"), (float)md.getPhysicalImageWidth());
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

	public static void init()
	{
		GL2 gl = GLContext.getCurrentGL().getGL2();
		
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
				System.err.println("Unknown error during shader compilation.");
			}
			throw new RuntimeException("Could not compile shader.");
		}
		gl.glUseProgram(0);
	}

	@Override
	public String getFullName()
	{
		MetaData md=getMetaData(TimeLine.SINGLETON.getCurrentDateTime());
		if(md!=null)
			return md.getFullName();
		else
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
		MainFrame.LAYER_PANEL.updateData();
	}

	public LocalDateTime getFirstLocalDateTime()
	{
		return start;
	}

	public LocalDateTime getLastLocalDateTime()
	{
		return end;
	}

	public Future<ByteBuffer> prepareImageData(final MainPanel mainPanel, final Dimension size)
	{
		final LocalDateTime currentDateTime = TimeLine.SINGLETON.getCurrentDateTime();
		final MetaData metaData = ultimateLayer.getMetaData(currentDateTime);
		if (metaData == null)
			return new FutureValue<ByteBuffer>(null);
		
		if(lut==null)
			lut=metaData.getDefaultLUT();
		
		final ImageRegion imageRegion = getCurrentRegion(mainPanel, metaData, size);
		if (imageRegion.getImageSize().getWidth() < 0 || imageRegion.getImageSize().getHeight() < 0)
			return new FutureValue<ByteBuffer>(null);
		
		LocalDateTime nextLocalDateTime = ultimateLayer.getClosestLocalDateTime(currentDateTime);
		if (nextLocalDateTime == null)
			nextLocalDateTime = ultimateLayer.localDateTimes.last();
		
		ImageRegion cachedRegion = TextureCache.get(ultimateLayer, imageRegion, nextLocalDateTime);
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
				return ultimateLayer.getImageData(finalNextLocalDateTime, imageRegion);
			}
		});
	}
	
	private RayTrace rayTrace=new RayTrace();

	private static final int MAX_X_POINTS = 11;
	private static final int MAX_Y_POINTS = 11;

	public ImageRegion getCurrentRegion(MainPanel mainPanel, MetaData metaData)
	{
		return getCurrentRegion(mainPanel, metaData, mainPanel.getCanavasSize());
	}

	public ImageRegion getCurrentRegion(MainPanel mainPanel, MetaData metaData, Dimension size)
	{
		rayTrace = new RayTrace(metaData.getRotation().toMatrix());

		double partOfWidth = mainPanel.getWidth() / (double) (MAX_X_POINTS - 1);
		double partOfHeight = mainPanel.getHeight() / (double) (MAX_Y_POINTS - 1);

		double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;

		for (int i = 0; i < MAX_X_POINTS; i++)
		{
			for (int j = 0; j < MAX_Y_POINTS; j++)
			{
				Vector2d imagePoint = rayTrace.castTexturepos((int) (i * partOfWidth), (int) (j * partOfHeight), metaData, mainPanel);

				if (imagePoint != null)
				{
					/*
					 * JPanel panel = null; if (!(mainPanel instanceof
					 * OverViewPanel)){
					 * 
					 * panel = new JPanel(); panel.setBackground(Color.YELLOW);
					 * }
					 */
					minX = Math.min(minX, imagePoint.x);
					maxX = Math.max(maxX, imagePoint.x);
					minY = Math.min(minY, imagePoint.y);
					maxY = Math.max(maxY, imagePoint.y);

					/*
					 * if (!(mainPanel instanceof OverViewPanel)){
					 * panel.setBounds((int) (imagePoint.x *
					 * contentPanel.getWidth()) - 3,(int) (imagePoint.y *
					 * contentPanel.getHeight()) - 3, 5, 5);
					 * contentPanel.add(panel); }
					 */
				}
			}
		}
		// frame.repaint();

		Rectangle2D rectangle = new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
		ImageRegion imageRegion = new ImageRegion(getTime());
		imageRegion.setImageData(rectangle);
		imageRegion.calculateScaleFactor(this, mainPanel, metaData, size);
		return imageRegion;
		// frame.repaint();
		// frame.setVisible(true);
	}
}
