package org.helioviewer.jhv.layers;

import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.base.Globals;
import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.MainPanel;
import org.helioviewer.jhv.gui.statusLabels.FramerateStatusPanel;
import org.helioviewer.jhv.layers.LUT.Lut;
import org.helioviewer.jhv.layers.Movie.Match;
import org.helioviewer.jhv.opengl.RayTrace;
import org.helioviewer.jhv.opengl.Texture;
import org.helioviewer.jhv.opengl.camera.CameraMode;
import org.helioviewer.jhv.opengl.camera.CameraMode.MODE;
import org.helioviewer.jhv.opengl.camera.animation.CameraRotationAnimation;
import org.helioviewer.jhv.viewmodel.TimeLine;
import org.helioviewer.jhv.viewmodel.TimeLine.DecodeQualityLevel;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.json.JSONObject;
import org.w3c.dom.Document;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;

public abstract class ImageLayer extends Layer
{
	public double opacity = 1;
	public double sharpness = 0;
	public double gamma = 1;
	public double contrast = 0;
	@Nullable protected Lut lut = null;
	public boolean redChannel = true;
	public boolean greenChannel = true;
	public boolean blueChannel = true;
	public boolean invertedLut = false;
	protected boolean coronaVisible = true;
	public boolean animateCameraToFacePlane;

	protected LocalDateTime start;
	protected LocalDateTime end;
	
	protected int cadence;
	
	@Nullable public Lut getLUT()
	{
		return lut;
	}

	public void setLUT(@Nullable Lut _lut)
	{
		lut = _lut;
		MainFrame.SINGLETON.FILTER_PANEL.update();
	}

	public void toggleCoronaVisibility()
	{
		coronaVisible=!coronaVisible;
	}
	
	public boolean isCoronaVisible()
	{
		return coronaVisible;
	}
	
	private static int shaderprogram = -1;
	
	protected static ArrayList<Texture> textures= new ArrayList<>();
	
	public abstract @Nullable MetaData getMetaData(@Nonnull LocalDateTime currentDateTime);

	public abstract @Nullable Document getMetaDataDocument(@Nonnull LocalDateTime _currentDateTime);

	public abstract void storeConfiguration(JSONObject jsonLayer);
	
	public abstract @Nullable Match getMovie(LocalDateTime _currentDateTime);

	/**
	 * This method should be called whenever we can throw all cached textures away, and the
	 * likelihood that cached textures would be used again is low.
	 */
	public static void newRenderPassStarted()
	{
		FramerateStatusPanel.notifyRenderingNewFrame();
		
		for(Texture t:textures)
			if(t.usedByCurrentRenderPass)
			{
				Telemetry.trackException(new RuntimeException("Had to clear texture usage flag?!?"));
				t.usedByCurrentRenderPass=false;
			}
	}
	
	public static void ensureAppropriateTextureCacheSize()
	{
		int cnt=0;
		for (Layer l : Layers.getLayers())
			if (l instanceof ImageLayer)
				//need at least two textures per layer (overview + main)
				cnt+=2;
		
		//we should have space for at least 10 textures (2k x 2k * 8bit * 10 = 40 mb) 
		if(cnt<10)
			cnt=10;
		
		//there's no reason to deallocate already existing textures
		if(cnt<textures.size())
			cnt=textures.size();
		
		while(textures.size()<cnt)
			textures.add(new Texture());
	}
	
	@SuppressWarnings("null")
	public RenderResult renderLayer(GL2 gl, MainPanel mainPanel, PreparedImage _preparedImageData)
	{
		LocalDateTime currentDateTime = TimeLine.SINGLETON.getCurrentDateTime();
		MetaData md=getMetaData(currentDateTime);
		if(md==null || md.localDateTime==null)
			return RenderResult.RETRY_LATER;
		
		//create camera animation to rotate the camera to face the image plane of
		//the current layer. this is used when layers are newly added to make sure
		//they are shown nicely.
		if(animateCameraToFacePlane)
		{
			animateCameraToFacePlane=false;
			
			MainFrame.SINGLETON.MAIN_PANEL.addCameraAnimation(new CameraRotationAnimation(
					MainFrame.SINGLETON.MAIN_PANEL,
					MainFrame.SINGLETON.MAIN_PANEL.getRotationEnd().inversed().rotate(md.rotation)
				));
		}
		
		_preparedImageData.texture.usedByCurrentRenderPass=false;
		
		float xSunOffset =  (float) ((md.sunPixelPosition.x - md.resolution.x / 2.0) / (float)md.resolution.x);
		float ySunOffset = -(float) ((md.sunPixelPosition.y - md.resolution.y / 2.0) / (float)md.resolution.y);

		Vector3d currentPos = mainPanel.getRotationCurrent().toMatrix().multiply(new Vector3d(0, 0, 1));
		Vector3d startPos = md.rotation.toMatrix().multiply(new Vector3d(0, 0, 1));

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
		gl.glBindTexture(GL2.GL_TEXTURE_2D, _preparedImageData.texture.openGLTextureId);

		gl.glEnable(GL2.GL_VERTEX_PROGRAM_ARB);
		gl.glEnable(GL2.GL_FRAGMENT_PROGRAM_ARB);

		gl.glUseProgram(shaderprogram);

		gl.glActiveTexture(GL.GL_TEXTURE1);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, org.helioviewer.jhv.layers.LUT.getTextureId());

		gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "texture"), 0);
		gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "lut"), 1);
		gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "fov"), (float) Math.toRadians(MainPanel.FOV / 2.0));
		gl.glUniform2f(gl.glGetUniformLocation(shaderprogram, "physicalImageSize"), (float)md.getPhysicalImageWidth(), (float)md.getPhysicalImageHeight());
		gl.glUniform2f(gl.glGetUniformLocation(shaderprogram, "sunOffset"), xSunOffset, ySunOffset);
		
		gl.glUniform4f(gl.glGetUniformLocation(shaderprogram, "imageOffset"),
				(float)_preparedImageData.texture.getImageRegion().areaOfSourceImage.getX(),
				(float)_preparedImageData.texture.getImageRegion().areaOfSourceImage.getY(),
				(float)_preparedImageData.texture.getImageRegion().areaOfSourceImage.getWidth()/(_preparedImageData.texture.textureWidth-1f/_preparedImageData.texture.width),
				(float)_preparedImageData.texture.getImageRegion().areaOfSourceImage.getHeight()/(_preparedImageData.texture.textureHeight-1f/_preparedImageData.texture.height)
			);
		
		gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "opacity"), (float) opacity);
		gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "gamma"), (float) gamma);
		gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "sharpen"), (float) sharpness);
		gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "lutPosition"), getLUT().ordinal());
		gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "lutInverted"), invertedLut ? 1:0);
		gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "redChannel"), redChannel ? 1:0);
		gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "greenChannel"), greenChannel ? 1:0);
		gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "blueChannel"), blueChannel ? 1:0);
		gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "opacityCorona"), opacityCorona);
		gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "cameraMode"), CameraMode.mode == MODE.MODE_3D ? 1 : 0);
		gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "contrast"), (float) contrast);

		float clipNear = (float) Math.max(mainPanel.getTranslationCurrent().z - 4 * Constants.SUN_RADIUS, MainPanel.CLIP_NEAR);
		gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "near"), clipNear);
		gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "far"), (float) (mainPanel.getTranslationCurrent().z + 4 * Constants.SUN_RADIUS));
		float[] transformation = mainPanel.getTransformation().toFloatArray();
		gl.glUniformMatrix4fv(gl.glGetUniformLocation(shaderprogram, "transformation"), 1, true, transformation, 0);

		float[] layerTransformation = md.rotation.toMatrix().toFloatArray();
		gl.glUniformMatrix4fv(gl.glGetUniformLocation(shaderprogram, "layerTransformation"), 1, true, layerTransformation, 0);
		
		float[] layerInv = md.rotation.inversed().toMatrix().toFloatArray();
		gl.glUniformMatrix4fv(gl.glGetUniformLocation(shaderprogram, "layerInv"), 1, true, layerInv, 0);

		gl.glUniform2f(
				gl.glGetUniformLocation(shaderprogram, "imageResolution"),
				_preparedImageData.texture.width,
				_preparedImageData.texture.height);

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
		gl.glActiveTexture(GL.GL_TEXTURE0);
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

		String vertexShaderSrc = Globals.loadFile("/shader/MainVertex.glsl");
		String fragmentShaderSrc = Globals.loadFile("/shader/MainFragment.glsl");

		gl.glShaderSource(vertexShader, 1, new String[] { vertexShaderSrc }, null, 0);
		gl.glCompileShader(vertexShader);

		gl.glShaderSource(fragmentShader, 1, new String[] { fragmentShaderSrc }, null, 0);
		gl.glCompileShader(fragmentShader);

		IntBuffer intBuffer = IntBuffer.allocate(1);
		gl.glGetShaderiv(fragmentShader, GL2.GL_COMPILE_STATUS, intBuffer);
		if(intBuffer.get(0) == GL2.GL_FALSE)
		{
			gl.glGetShaderiv(fragmentShader, GL2.GL_INFO_LOG_LENGTH, intBuffer);
			int size = intBuffer.get(0);
			System.err.println("Program compile error: ");
			if (size > 0)
			{
				ByteBuffer byteBuffer = ByteBuffer.allocate(size);
				gl.glGetShaderInfoLog(fragmentShader, size, intBuffer, byteBuffer);
				for (byte b : byteBuffer.array())
					System.err.print((char) b);
			}
			else
			{
				System.err.println("Unknown error during shader compilation.");
			}
			throw new RuntimeException("Could not compile shader.");
		}
		
		

		shaderprogram = gl.glCreateProgram();
		gl.glAttachShader(shaderprogram, vertexShader);
		gl.glAttachShader(shaderprogram, fragmentShader);
		gl.glLinkProgram(shaderprogram);
		gl.glValidateProgram(shaderprogram);

		gl.glGetProgramiv(shaderprogram, GL2.GL_LINK_STATUS, intBuffer);
		if (intBuffer.get(0) == GL2.GL_FALSE)
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
	public @Nullable String getFullName()
	{
		MetaData md=getMetaData(TimeLine.SINGLETON.getCurrentDateTime());
		if(md!=null)
			return md.displayName;
		else
			return null;
	}
	
	public void dispose()
	{
	}

	@Override
	public void retry()
	{
	}

	public LocalDateTime getFirstLocalDateTime()
	{
		return start;
	}

	public LocalDateTime getLastLocalDateTime()
	{
		return end;
	}
	
	public static class PreparedImage
	{
		public final Texture texture;
		
		public final @Nullable ImageRegion imageRegion;
		
		public PreparedImage(Texture _texture)
		{
			texture=_texture;
			imageRegion=null;
		}
		
		public PreparedImage(Texture _texture, ImageRegion _imageRegion)
		{
			texture=_texture;
			imageRegion=_imageRegion;
		}
	}

	public abstract Future<PreparedImage> prepareImageData(final MainPanel mainPanel, DecodeQualityLevel _quality, final Dimension size, final GLContext _gl);

	private static final int MAX_X_POINTS = 11;
	private static final int MAX_Y_POINTS = 11;

	/**
	 * Calculates the required region
	 * 
	 * @return The ImageRegion or NULL if nothing is visible
	 */
	public @Nullable ImageRegion calculateRegion(MainPanel _mainPanel, DecodeQualityLevel _quality, MetaData _metaData, Dimension _size)
	{
		RayTrace rayTrace = new RayTrace(_metaData.rotation.toMatrix());

		double partOfWidth = _mainPanel.getWidth() / (double) (MAX_X_POINTS - 1);
		double partOfHeight = _mainPanel.getHeight() / (double) (MAX_Y_POINTS - 1);

		double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
		
		int hitPoints=0;
		for (int i = 0; i < MAX_X_POINTS; i++)
			for (int j = 0; j < MAX_Y_POINTS; j++)
			{
				Vector2d imagePoint = rayTrace.castTexturepos((int) (i * partOfWidth), (int) (j * partOfHeight), _metaData, _mainPanel);

				if (imagePoint != null)
				{
					hitPoints++;
					
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
		// frame.repaint();
		
		if(hitPoints<3)
			return null;
		
		ImageRegion ir = new ImageRegion(
				new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY),
				_quality,
				_mainPanel.getTranslationCurrent().z, _metaData, _size);
		
		if(ir.texels.width==0 || ir.texels.height==0)
			return null;
		
		return ir;
		
		// frame.repaint();
		// frame.setVisible(true);
	}

	public int getCadence()
	{
		return cadence;
	}
}
