package org.helioviewer.jhv.layers;

import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.base.Globals;
import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.base.math.Matrix4d;
import org.helioviewer.jhv.base.math.Quaternion;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.math.Vector4d;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.base.physics.DifferentialRotation;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.MainPanel;
import org.helioviewer.jhv.gui.statusLabels.FramerateStatusPanel;
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

import com.google.common.util.concurrent.ListenableFuture;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;

public abstract class ImageLayer extends Layer
{
	public boolean animateCameraToFacePlane;

	protected LocalDateTime start;
	protected LocalDateTime end;
	
	private int groupForOpacity;
	
	protected int cadence;
	private boolean metadataInitialized=false;
	
	public void initializeMetadata(MetaData _md)
	{
		if(metadataInitialized)
			return;
		
		metadataInitialized=true;
		
		setLUT(_md.getDefaultLUT());
		groupForOpacity = _md.groupForOpacity;
	}
	
	public boolean isMetadataInitialized()
	{
		return metadataInitialized;
	}
	
	public int getGroupForOpacity()
	{
		return groupForOpacity;
	}
	
	private static int shaderCorona = -1;
	private static int shaderSphere = -1;
	
	protected static ArrayList<Texture> textures= new ArrayList<>();
	
	public abstract @Nullable MetaData getMetaData(@Nonnull LocalDateTime currentDateTime);

	public abstract @Nullable Document getMetaDataDocument(@Nonnull LocalDateTime _currentDateTime);

	public abstract void storeConfiguration(JSONObject jsonLayer);
	
	public abstract @Nullable Match findBestFrame(LocalDateTime _currentDateTime);

	
	public boolean supportsFilterContrastGamma()
	{
		return true;
	}
	
	public boolean supportsFilterSharpness()
	{
		return true;
	}
	
	public boolean supportsFilterRGB()
	{
		return true;
	}
	
	public boolean supportsFilterOpacity()
	{
		return true;
	}
	
	public boolean supportsFilterLUT()
	{
		return true;
	}
	
	public boolean supportsFilterCorona()
	{
		return true;
	}
	
	
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
	
	public static void ensureAppropriateTextureCacheSize(GL2 gl)
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
			textures.add(new Texture(gl));
	}
	
	public RenderResult renderLayer(GL2 gl, MainPanel mainPanel, PreparedImage _preparedImageData, float _opacityScaling)
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
					MainFrame.SINGLETON.MAIN_PANEL.getRotationEnd().inversed().rotated(md.rotation.inversed())
				));
		}
		
		_preparedImageData.texture.usedByCurrentRenderPass=false;
		if(_preparedImageData.texture.needsUpload)
			_preparedImageData.texture.uploadByteBuffer(gl, this, md.localDateTime, _preparedImageData.imageRegion);
		
		Matrix4d transformation = calcTransformation(mainPanel, md);
		
		float xSunOffset =  (float) ((md.sunPixelPosition.x - md.resolution.x / 2.0) / (float)md.resolution.x);
		float ySunOffset = -(float) ((md.sunPixelPosition.y - md.resolution.y / 2.0) / (float)md.resolution.y);

		float opacityCorona = 0;
		if (coronaVisible)
		{
			double dot = MathUtils.clip(transformation.multiply(new Vector4d(0,0,1,0)).dot(new Vector4d(0, 0, 1, 0)), -1, 1);
			double angle = Math.toDegrees(Math.acos(dot));
			double maxAngle = 60;
			double minAngle = 30;
			opacityCorona = (float) ((Math.abs(90 - angle) - minAngle) / (maxAngle - minAngle));
			opacityCorona = opacityCorona > 1 ? 1f : opacityCorona;
		}
		gl.glColor4f(1, 1, 1, 1);
		gl.glEnable(GL2.GL_BLEND);
		gl.glEnable(GL2.GL_TEXTURE_2D);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE);
		gl.glActiveTexture(GL.GL_TEXTURE0);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, _preparedImageData.texture.openGLTextureId);

		gl.glEnable(GL2.GL_VERTEX_PROGRAM_ARB);
		gl.glEnable(GL2.GL_FRAGMENT_PROGRAM_ARB);

		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glDepthFunc(GL2.GL_ALWAYS);
		gl.glDepthMask(true);
		renderWithShader(gl, mainPanel, shaderSphere, _preparedImageData, md, _opacityScaling, opacityCorona, xSunOffset, ySunOffset, transformation);
		
		gl.glDepthFunc(GL2.GL_LEQUAL);
		gl.glDepthMask(false);
		if(opacityCorona>0)
			renderWithShader(gl, mainPanel, shaderCorona, _preparedImageData, md, _opacityScaling, opacityCorona, xSunOffset, ySunOffset, transformation);
		
		gl.glUseProgram(0);
		gl.glActiveTexture(GL.GL_TEXTURE0);
		gl.glDisable(GL2.GL_BLEND);
		gl.glDisable(GL2.GL_TEXTURE_2D);
		gl.glDisable(GL2.GL_FRAGMENT_PROGRAM_ARB);
		gl.glDisable(GL2.GL_VERTEX_PROGRAM_ARB);
		return RenderResult.OK;
	}
	
	private void renderWithShader(GL2 gl, MainPanel mainPanel, int shaderprogram, PreparedImage _preparedImageData, MetaData md,
			float _opacityScaling,
			float opacityCorona,
			float xSunOffset, float ySunOffset, Matrix4d _transformation)
	{
		gl.glUseProgram(shaderprogram);

		gl.glActiveTexture(GL.GL_TEXTURE1);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, org.helioviewer.jhv.layers.LUT.getTextureId());

		//TODO: sharpness fade von 100% auf 200% skalierung
		
		gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "texture"), 0);
		gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "lut"), 1);
		gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "tanFOV"), (float) Math.tan(Math.toRadians(MainPanel.FOV / 2.0)));
		gl.glUniform2f(gl.glGetUniformLocation(shaderprogram, "physicalImageSize"), (float)md.getPhysicalImageWidth(), (float)md.getPhysicalImageHeight());
		gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "innerRadius"), (float)md.getInnerPhysicalRadius());
		gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "outerRadius"), (float)md.getOuterPhysicalRadius());
		gl.glUniform2f(gl.glGetUniformLocation(shaderprogram, "sunOffset"), xSunOffset, ySunOffset);
		
		Rectangle2D sourceArea = _preparedImageData.texture.getImageRegion().areaOfSourceImage;
		gl.glUniform4f(gl.glGetUniformLocation(shaderprogram, "imageOffset"),
				(float)sourceArea.getX(),
				(float)sourceArea.getY(),
				(float)sourceArea.getWidth()/(_preparedImageData.texture.textureWidth),
				(float)sourceArea.getHeight()/(_preparedImageData.texture.textureHeight)
			);
		
		gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "opacity"), (float) opacity * (float)_opacityScaling);
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

		gl.glUniformMatrix4fv(gl.glGetUniformLocation(shaderprogram, "transformation"), 1, true, _transformation.transposed().toFloatArray(), 0);

		gl.glUniform2f(
				gl.glGetUniformLocation(shaderprogram, "subtexSize"),
				_preparedImageData.texture.textureWidth,
				_preparedImageData.texture.textureHeight);
		
		gl.glUniform2f(
				gl.glGetUniformLocation(shaderprogram, "imageResolution"),
				_preparedImageData.texture.width,
				_preparedImageData.texture.height);

		gl.glBegin(GL2.GL_QUADS);
			gl.glTexCoord2f(-1.0f, 1.0f);
			gl.glVertex2d(-1, -1);
			gl.glTexCoord2f(1.0f, 1.0f);
			gl.glVertex2d(1, -1);
			gl.glTexCoord2f(1.0f, -1.0f);
			gl.glVertex2d(1, 1);
			gl.glTexCoord2f(-1.0f, -1.0f);
			gl.glVertex2d(-1, 1);
		gl.glEnd();
	}

	private Matrix4d calcTransformation(MainPanel mainPanel, MetaData md)
	{
		//see http://jgiesen.de/sunrot/index.html and http://www.petermeadows.com/stonyhurst/sdisk6in7.gif
		double diffRotattion = DifferentialRotation.calculateRotationInRadians(0,md.localDateTime.until(TimeLine.SINGLETON.getCurrentDateTime(), ChronoUnit.MILLIS)/1000f);
		Quaternion diffRotationQuat = Quaternion.createRotation(-diffRotattion, new Vector3d(0, 1, 0));
		
		return Matrix4d.createTranslationMatrix(mainPanel.getTranslationCurrent())
				.multiplied(diffRotationQuat.toMatrix())
				.multiplied(md.rotation.toMatrix())
				.multiplied(mainPanel.getRotationCurrent().toMatrix())
				;
	}
	
	private static int buildShader(GL2 gl, String _fnVertex, String _fnFragment)
	{
		int vertexShader = gl.glCreateShader(GL2.GL_VERTEX_SHADER);
		gl.glShaderSource(vertexShader, 1, new String[] { Globals.loadFile(_fnVertex) }, null, 0);
		gl.glCompileShader(vertexShader);
		IntBuffer intBuffer = IntBuffer.allocate(1);
		gl.glGetShaderiv(vertexShader, GL2.GL_COMPILE_STATUS, intBuffer);
		if(intBuffer.get(0) == GL2.GL_FALSE)
		{
			StringBuffer err=new StringBuffer();
			gl.glGetShaderiv(vertexShader, GL2.GL_INFO_LOG_LENGTH, intBuffer);
			int size = intBuffer.get(0);
			if (size > 0)
			{
				ByteBuffer byteBuffer = ByteBuffer.allocate(size);
				gl.glGetShaderInfoLog(vertexShader, size, intBuffer, byteBuffer);
				for (byte b : byteBuffer.array())
					err.append((char) b);
			}
			else
				err.append("Unknown");
			
			throw new RuntimeException("Could not compile vertex shader "+_fnVertex+": "+err.toString());
		}

		int fragmentShader = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);
		gl.glShaderSource(fragmentShader, 1, new String[] { Globals.loadFile(_fnFragment) }, null, 0);
		gl.glCompileShader(fragmentShader);
		gl.glGetShaderiv(fragmentShader, GL2.GL_COMPILE_STATUS, intBuffer);
		if(intBuffer.get(0) == GL2.GL_FALSE)
		{
			StringBuffer err=new StringBuffer();
			gl.glGetShaderiv(fragmentShader, GL2.GL_INFO_LOG_LENGTH, intBuffer);
			int size = intBuffer.get(0);
			if (size > 0)
			{
				ByteBuffer byteBuffer = ByteBuffer.allocate(size);
				gl.glGetShaderInfoLog(fragmentShader, size, intBuffer, byteBuffer);
				for (byte b : byteBuffer.array())
					err.append((char) b);
			}
			else
				err.append("Unknown");
			
			throw new RuntimeException("Could not compile fragment shader "+_fnFragment+": "+err.toString());
		}
		
		int program = gl.glCreateProgram();
		gl.glAttachShader(program, vertexShader);
		gl.glAttachShader(program, fragmentShader);
		gl.glLinkProgram(program);
		gl.glValidateProgram(program);

		gl.glGetProgramiv(program, GL2.GL_LINK_STATUS, intBuffer);
		if (intBuffer.get(0) == GL2.GL_FALSE)
		{
			StringBuffer err=new StringBuffer();
			gl.glGetProgramiv(program, GL2.GL_INFO_LOG_LENGTH, intBuffer);
			int size = intBuffer.get(0);
			if (size > 0)
			{
				ByteBuffer byteBuffer = ByteBuffer.allocate(size);
				gl.glGetProgramInfoLog(program, size, intBuffer, byteBuffer);
				for (byte b : byteBuffer.array())
					err.append((char) b);
			}
			else
				err.append("Unknown");
			
			throw new RuntimeException("Could not link shader: " + err.toString());
		}
		
		gl.glUseProgram(0);
		return program;
	}

	public static void init(GL2 gl)
	{
		shaderSphere = buildShader(gl, "/shader/MainVertex.glsl", "/shader/SphereFragment.glsl");
		shaderCorona = buildShader(gl, "/shader/MainVertex.glsl", "/shader/CoronaFragment.glsl");
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
		public final ImageLayer layer;
		
		public final Texture texture;
		
		public final @Nullable ImageRegion imageRegion;
		
		public PreparedImage(ImageLayer _layer, Texture _texture)
		{
			layer=_layer;
			texture=_texture;
			imageRegion=null;
		}
		
		public PreparedImage(ImageLayer _layer, Texture _texture, ImageRegion _imageRegion)
		{
			layer=_layer;
			texture=_texture;
			imageRegion=_imageRegion;
		}
	}

	public abstract ListenableFuture<PreparedImage> prepareImageData(final MainPanel mainPanel, DecodeQualityLevel _quality, final Dimension size, final GLContext _gl);

	private static final int MAX_X_POINTS = 11;
	private static final int MAX_Y_POINTS = 11;

	/**
	 * Calculates the required region
	 * 
	 * @return The ImageRegion or NULL if nothing is visible
	 */
	public @Nullable ImageRegion calculateRegion(MainPanel _mainPanel, DecodeQualityLevel _quality, MetaData _metaData, Dimension _size)
	{
		RayTrace rayTrace = new RayTrace();

		double partOfWidth = _mainPanel.getWidth() / (double) (MAX_X_POINTS - 1);
		double partOfHeight = _mainPanel.getHeight() / (double) (MAX_Y_POINTS - 1);

		double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
		
		Matrix4d transformation=calcTransformation(_mainPanel, _metaData);
		
		/*BufferedImage bi=new BufferedImage(frame.getContentPane().getWidth(),frame.getContentPane().getHeight(),BufferedImage.TYPE_INT_BGR);
		Graphics2D g=bi.createGraphics();
		g.setColor(Color.WHITE);*/
		
		int hitPoints=0;
		for (int i = 0; i < MAX_X_POINTS; i++)
			for (int j = 0; j < MAX_Y_POINTS; j++)
			{
				for(Vector2d imagePoint : rayTrace.castTexturepos((int) (i * partOfWidth), (int) (j * partOfHeight), _mainPanel, _metaData, transformation))
				{
					hitPoints++;
					
					minX = Math.min(minX, imagePoint.x);
					maxX = Math.max(maxX, imagePoint.x);
					minY = Math.min(minY, imagePoint.y);
					maxY = Math.max(maxY, imagePoint.y);

					/*g.fillRect(
							(int) (imagePoint.x * bi.getWidth()) - 3,
							(int) (imagePoint.y * bi.getHeight()) - 3,
							5, 5);*/
				}
		}
		
		/*g.dispose();
		if (_mainPanel.getClass()==MainPanel.class)
		{
			frame.getContentPane().removeAll();
			
			frame.getContentPane().add(new JLabel(new ImageIcon(bi)));
			frame.validate();
			frame.repaint();
		}*/
		
		if(hitPoints<3)
			return null;
		
		ImageRegion ir = new ImageRegion(
				new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY),
				_quality,
				_mainPanel.getTranslationCurrent().z, _metaData, _size);
		
		if(ir.texels.width==0 || ir.texels.height==0)
			return null;
		
		return ir;
	}
	
	/*JFrame frame = new JFrame();
	public ImageLayer()
	{
		frame.setSize(400, 400);
		frame.setVisible(true);
		frame.validate();
	}*/
	
	public int getCadence()
	{
		return cadence;
	}

	public boolean isDataAvailableOnServer(LocalDateTime _ldt)
	{
		return true;
	}
}
