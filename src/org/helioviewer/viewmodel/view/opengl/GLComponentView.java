package org.helioviewer.viewmodel.view.opengl;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.AbstractList;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.media.opengl.DebugGL2;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason.LayerChangeType;
import org.helioviewer.viewmodel.changeevent.RegionChangedReason;
import org.helioviewer.viewmodel.changeevent.ViewChainChangedReason;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.renderer.screen.GLScreenRenderGraphics;
import org.helioviewer.viewmodel.renderer.screen.ScreenRenderer;
import org.helioviewer.viewmodel.view.AbstractComponentView;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.ViewportView;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderView;
import org.helioviewer.viewmodel.view.opengl.shader.GLMinimalFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLMinimalVertexShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderHelper;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderView;
import org.helioviewer.viewmodel.viewport.StaticViewport;
import org.helioviewer.viewmodel.viewport.Viewport;
import org.helioviewer.viewmodel.viewportimagesize.ViewportImageSize;

import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.TileRenderer;
import com.jogamp.opengl.util.awt.ImageUtil;


/**
 * Implementation of ComponentView for rendering in OpenGL mode.
 * 
 * <p>
 * This class starts the tree walk through all the GLViews to draw the final
 * scene. Therefore the class owns a GLCanvas. Note that GLCanvas is a
 * heavyweight component.
 * 
 * <p>
 * For further information about the use of OpenGL within this application, see
 * {@link GLView}.
 * 
 * <p>
 * For further information about the role of the ComponentView within the view
 * chain, see {@link org.helioviewer.viewmodel.view.ComponentView}
 * 
 * @author Markus Langenberg
 */
public class GLComponentView extends AbstractComponentView implements ViewListener, GLEventListener {

    public static final String SETTING_TILE_WIDTH = "gl.screenshot.tile.width";
    public static final String SETTING_TILE_HEIGHT = "gl.screenshot.tile.height";

    private static final boolean DEBUG = true;

    // general
    private GLCanvas canvas;
    private RegionView regionView;

    // render options
    private Color backgroundColor = Color.BLACK;
    private Color outsideViewportColor = Color.DARK_GRAY;
    private float xOffset = 0.0f;
    private float yOffset = 0.0f;
    private AbstractList<ScreenRenderer> postRenderers = new LinkedList<ScreenRenderer>();

    // Helper
    private boolean rebuildShadersRequest = false;
    private GLTextureHelper textureHelper = new GLTextureHelper();
    private GLShaderHelper shaderHelper = new GLShaderHelper();

    // screenshot
    private boolean saveBufferedImage = false;
    private BufferedImage screenshot;
    private Buffer screenshotBuffer;
    private TileRenderer tileRenderer;
    private FPSAnimator animator;

    private int[] frameBufferObject;
    private int[] renderBufferDepth;
    private int[] renderBufferColor;
    private static int defaultTileWidth = 2048;
    private static int defaultTileHeight = 2048;
    private int tileWidth;
    private int tileHeight;

    private Viewport viewport;
    private Viewport defaultViewport;
    private Region defaultRegion;
    
    /**
     * Default constructor.
     * 
     * Also initializes all OpenGL Helper classes.
     */
    public GLComponentView() {
        //canvas = new GLCanvas(null, null, GLSharedContext.getSharedContext(), null);
        canvas = new GLCanvas();
    	canvas.setMinimumSize(new Dimension());
        
        animator = new FPSAnimator(canvas, 30);
        animator.start();

        canvas.addGLEventListener(this);
    }

    public void dispose() {
        if (screenshot != null) {
            screenshot.flush();
            screenshot = null;
        }
        screenshotBuffer = null;
        tileRenderer = null;
    }

    public void dispose(GLAutoDrawable drawable) {
        final GL2 gl = drawable.getGL().getGL2();
        gl.glDeleteFramebuffers(1, frameBufferObject, 0);
        gl.glDeleteRenderbuffers(1, renderBufferDepth, 0);
        gl.glDeleteRenderbuffers(1, renderBufferColor, 0);
        dispose();
    }

    /**
     * Stop the animation of the canvas. Useful if one wants to display frame
     * per frame (such as in movie export).
     */
    public void stopAnimation() {
        if (animator != null && animator.isAnimating())
            animator.stop();
    }

    /**
     * (Re-)start the animation of the canvas.
     */
    public void startAnimation() {
        if (animator == null)
            animator = new FPSAnimator(canvas, 30);
        if (!animator.isAnimating())
            animator.start();
    }

    /**
     * Save the next rendered frame in a buffered image and return this image.
     * 
     * WARNING: The returned image is a reference to the internal buffered image
     * of this class. A subsequent call to this function might change the data
     * of this returned buffered image. If this is not desired, one has to make
     * a copy of the returned image, before calling getBufferedImage() again.
     * This choice was made with the movie export application in mind in order
     * to save main memory.
     * 
     * @return BufferedImage of the next rendered frame
     */
    public BufferedImage getBufferedImage() {
    	saveBufferedImage = true;
        this.canvas.display();
        return screenshot;
    }
    
    public BufferedImage getBufferedImage(int width, int height) {
    	viewport = StaticViewport.createAdaptedViewport(width, height);
    	saveBufferedImage = true;
        this.canvas.display();
        viewport = null;
        BufferedImage screenshot = this.screenshot;
        this.start();
        this.getBufferedImage();
        this.stop();
        if (screenshot.getHeight() != height || screenshot.getWidth() != width) screenshot = getBufferedImage(width,height);
        return screenshot;
    }

    public static void setTileSize(int width, int height) {
        defaultTileWidth = width;
        defaultTileHeight = height;
    }

    /**
     * {@inheritDoc}
     */
    public Component getComponent() {
        return canvas;
    }

    /**
     * {@inheritDoc}
     * 
     * Since the screenshot is saved after the next rendering cycle, the result
     * is not available directly after calling this function. It only places a
     * request to save the screenshot.
     */
    public void saveScreenshot(String imageFormat, File outputFile) throws IOException {
    	ImageIO.write(this.getBufferedImage(), imageFormat, outputFile);
    }

    public void saveScreenshot(String imageFormat, File outputFile, int width, int height){
    	try {
			ImageIO.write(this.getBufferedImage(width, height), imageFormat, outputFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    /**
     * {@inheritDoc}
     */
    public void setOffset(Vector2dInt offset) {
        xOffset = offset.getX();
        yOffset = offset.getY();
    }

    /**
     * {@inheritDoc}
     */
    public void addPostRenderer(ScreenRenderer postRenderer) {
        if (postRenderer != null) {
            synchronized (postRenderers) {
                postRenderers.add(postRenderer);
                if (postRenderer instanceof ViewListener) {
                    addViewListener((ViewListener) postRenderer);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removePostRenderer(ScreenRenderer postRenderer) {
        if (postRenderer != null) {
            synchronized (postRenderers) {
                do {
                    postRenderers.remove(postRenderer);
                    if (postRenderer instanceof ViewListener) {
                        removeViewListener((ViewListener) postRenderer);
                    }
                } while (postRenderers.contains(postRenderer));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public AbstractList<ScreenRenderer> getAllPostRenderer() {
        return postRenderers;
    }

    /**
     * {@inheritDoc}
     * 
     * In this case, the canvas is repainted.
     */
    protected synchronized void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {
        if (newView != null) {
            regionView = newView.getAdapter(RegionView.class);
        }

        canvas.repaint();
    }

    /**
     * {@inheritDoc}
     */
    public void viewChanged(View sender, ChangeEvent aEvent) {

        if (aEvent.reasonOccurred(ViewChainChangedReason.class)) {
            regionView = view.getAdapter(RegionView.class);
        }

        // rebuild shaders, if necessary
        if (aEvent.reasonOccurred(ViewChainChangedReason.class) || (aEvent.reasonOccurred(LayerChangedReason.class) && aEvent.getLastChangedReasonByType(LayerChangedReason.class).getLayerChangeType() == LayerChangeType.LAYER_ADDED)) {
            rebuildShadersRequest = true;
        }

        // inform all listener of the latest change reason
        // frameUpdated++;
        notifyViewListeners(aEvent);
    }

    public void setBackgroundColor(Color color) {
        backgroundColor = color;
    }

    /**
     * {@inheritDoc}
     */
    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }

    /**
     * Initializes OpenGL2.
     * 
     * This function is called when the canvas is visible the first time. It
     * initializes OpenGL by setting some system properties, such as switching
     * on some OpenGL features. Apart from that, the function also calls
     * {@link GLTextureHelper#initHelper(GL)}.
     * 
     * <p>
     * Note, that this function should not be called by any user defined
     * function. It is part of the GLEventListener and invoked by the OpenGL
     * thread.
     * 
     * @param drawable
     *            GLAutoDrawable passed by the OpenGL thread
     */
    public void init(GLAutoDrawable drawable) {
        GLSharedContext.setSharedContext(drawable.getContext());
        GLDrawableFactory.getFactory(GLProfile.getDefault()).createExternalGLContext();
        final GL2 gl = drawable.getGL().getGL2();

        frameBufferObject = new int[1];
        gl.glGenFramebuffers(1, frameBufferObject, 0);
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBufferObject[0]);
        generateNewRenderBuffers(gl);
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);

        textureHelper.delAllTextures(gl);
        GLTextureHelper.initHelper(gl);

        shaderHelper.delAllShaderIDs(gl);

        gl.glShadeModel(GL2.GL_FLAT);
        gl.glEnable(GL2.GL_TEXTURE_1D);
        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glEnable(GL2.GL_BLEND);
        gl.glEnable(GL2.GL_POINT_SMOOTH);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

        gl.glColor3f(1.0f, 1.0f, 1.0f);
    }

    private void generateNewRenderBuffers(GL gl) {
        tileWidth = defaultTileWidth;
        tileHeight = defaultTileHeight;
        if (renderBufferDepth != null) {
            gl.glDeleteRenderbuffers(1, renderBufferDepth, 0);
        }
        renderBufferDepth = new int[1];
        gl.glGenRenderbuffers(1, renderBufferDepth, 0);
        gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, renderBufferDepth[0]);
        gl.glRenderbufferStorage(GL2.GL_RENDERBUFFER, GL2.GL_DEPTH_COMPONENT, tileWidth, tileHeight);
        gl.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER, GL2.GL_DEPTH_ATTACHMENT, GL2.GL_RENDERBUFFER, renderBufferDepth[0]);

        if (renderBufferColor != null) {
            gl.glDeleteRenderbuffers(1, renderBufferColor, 0);
        }
        renderBufferColor = new int[1];
        gl.glGenRenderbuffers(1, renderBufferColor, 0);
        gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, renderBufferColor[0]);
        gl.glRenderbufferStorage(GL2.GL_RENDERBUFFER, GL2.GL_RGBA8, tileWidth, tileHeight);
        gl.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0, GL2.GL_RENDERBUFFER, renderBufferColor[0]);
    }

    /**
     * Reshapes the viewport.
     * 
     * This function is called, whenever the canvas is resized. It ensures, that
     * the perspective never gets corrupted.
     * 
     * <p>
     * Note, that this function should not be called by any user defined
     * function. It is part of the GLEventListener and invoked by the OpenGL
     * thread.
     * 
     * @param drawable
     *            GLAutoDrawable passed by the OpenGL thread
     * @param x
     *            New x-offset on the screen
     * @param y
     *            New y-offset on the screen
     * @param width
     *            New width of the canvas
     * @param height
     *            New height of the canvas
     */
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        final GL2 gl = drawable.getGL().getGL2();

        gl.setSwapInterval(1);

        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();

        gl.glOrtho(0, width, 0, height, -1, 1);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    /**
     * This function does the actual rendering of the scene on the screen.
     * 
     * This is the most important function of this class, it is responsible for
     * rendering the entire scene to the screen. Therefore, it starts the tree
     * walk. After that, all post renderes are called.
     * 
     * @param gl
     *            current GL object
     * @param xOffsetFinal
     *            x-offset in pixels
     * @param yOffsetFinal
     *            y-offset in pixels
     */
    protected void displayBody(GL2 gl, float xOffsetFinal, float yOffsetFinal) {
        // Set up screen
    	gl.glClearColor(outsideViewportColor.getRed() / 255.0f, outsideViewportColor.getGreen() / 255.0f, outsideViewportColor.getBlue() / 255.0f, outsideViewportColor.getAlpha() / 255.0f);

    	gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
        gl.glLoadIdentity();

        Viewport viewport = view.getAdapter(ViewportView.class).getViewport();
        ViewportImageSize viewportImageSize = ViewHelper.calculateViewportImageSize(view);

        if (viewportImageSize != null) {
            // Draw image

            gl.glPushMatrix();

            Region region = regionView.getRegion();
            gl.glTranslatef(xOffsetFinal, yOffsetFinal, 0.0f);
            gl.glScalef(viewportImageSize.getWidth() / (float) region.getWidth(), viewportImageSize.getHeight() / (float) region.getHeight(), 1.0f);
            gl.glTranslated(-region.getCornerX(), -region.getCornerY(), 0.0);

            // clear viewport
            gl.glColor4f(outsideViewportColor.getRed() / 255.0f, outsideViewportColor.getGreen() / 255.0f, outsideViewportColor.getBlue() / 255.0f, outsideViewportColor.getAlpha() / 255.0f);
            gl.glRectd(region.getCornerX(), region.getCornerY(), region.getUpperRightCorner().getX(), region.getUpperRightCorner().getY());

            //if (view instanceof GLView) {
                ((GLView) view).renderGL(gl, true);
            //} else {
                //textureHelper.renderImageDataToScreen(gl, view.getAdapter(RegionView.class).getRegion(), view.getAdapter(SubimageDataView.class).getSubimageData());
            //}
            gl.glPopMatrix();
        }

        // Draw post renderer

        gl.glTranslatef(0.0f, viewport.getHeight(), 0.0f);
        gl.glScalef(1.0f, -1.0f, 1.0f);

        GLScreenRenderGraphics glRenderer = new GLScreenRenderGraphics(gl);
        synchronized (postRenderers) {
            for (ScreenRenderer r : postRenderers) {
                r.render(glRenderer);
            }
        }
    }

    /**
     * Displays the scene on the screen.
     * 
     * This is the most important function of this class, it is responsible for
     * rendering the entire scene to the screen. Therefore, it starts the tree
     * walk. After that, all post renderes are called.
     * 
     * <p>
     * Note, that this function should not be called by any user defined
     * function. It is part of the GLEventListener and invoked by the OpenGL
     * thread.
     * 
     * @param drawable
     *            GLAutoDrawable passed by the OpenGL thread
     * @see ComponentView#addPostRenderer(ScreenRenderer)
     */
    public synchronized void display(GLAutoDrawable drawable) {

        if (view == null) {
            return;
        }
        if (!saveBufferedImage && (canvas.getSize().width <= 0 || canvas.getSize().height <= 0)) {
            return;
        }

        final GL2 gl;
        if (DEBUG) {
            gl = new DebugGL2(drawable.getGL().getGL2());
        } else {
            gl = drawable.getGL().getGL2();
        }

        // Rebuild all shaders, if necessary
        if (rebuildShadersRequest) {
            rebuildShaders(gl);
        }

        // Save Screenshot, if requested
        if (saveBufferedImage) {
        	gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBufferObject[0]);
            if (tileWidth != defaultTileWidth || tileHeight != defaultTileHeight) {
                generateNewRenderBuffers(gl);
            }
            gl.glPushAttrib(GL2.GL_VIEWPORT_BIT);
            gl.glViewport(0, 0, tileWidth, tileHeight);

            Log.trace(">> GLComponentView.display() > Start taking screenshot");
            
            Viewport v;
            
            if (viewport != null) {
            	v = viewport;
            	this.getAdapter(ViewportView.class).setViewport(viewport, new ChangeEvent());
            }
            else v = this.getAdapter(ViewportView.class).getViewport();
            
            if (screenshot == null || screenshot.getWidth() != v.getWidth() || screenshot.getHeight() != v.getHeight()) {
                screenshot = new BufferedImage(v.getWidth(), v.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
                //screenshot = new BufferedImage(tileWidth, tileHeight, BufferedImage.TYPE_3BYTE_BGR);
                screenshotBuffer = ByteBuffer.wrap(((DataBufferByte) screenshot.getRaster().getDataBuffer()).getData());
            }
            Log.trace(">> GLComponentView.display() > Initialize tile renderer - Tile size: " + tileWidth + "x" + tileHeight + " - Image size: " + v.getWidth() + "x" + v.getHeight());
            if (tileRenderer == null) {
                tileRenderer = new TileRenderer();
            }
            tileRenderer.setTileSize(tileWidth, tileHeight, 0);
            //tileRenderer.setImageSize(tileWidth, tileHeight);
            tileRenderer.setImageSize(v.getWidth(), v.getHeight());
            //tileRenderer.setImageBuffer(GL2.GL_BGR, GL2.GL_UNSIGNED_BYTE, screenshotBuffer);
            //tileRenderer.trOrtho(0, v.getWidth(), 0, v.getHeight(), -1, 1);
            //tileRenderer.trOrtho(0, tileWidth, 0, tileHeight, -1, 1);
            int tileNum = 0;
            /*do {
                ++tileNum;
                tileRenderer.beginTile(gl);
                gl.glClearColor(backgroundColor.getRed() / 255.0f, backgroundColor.getGreen() / 255.0f, backgroundColor.getBlue() / 255.0f, backgroundColor.getAlpha() / 255.0f);
                displayBody(gl, 0, 0);
            } while (tileRenderer.endTile(gl));*/
            Log.trace(">> GLComponentView.display() > Rendered " + tileNum + " tiles.");

            gl.glPopAttrib();
            
            ImageUtil.flipImageVertically(screenshot);

            gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
            
            
            saveBufferedImage = false;
            
            if (!animator.isStarted()) {
            	gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
            	gl.glClearColor(outsideViewportColor.getRed() / 255.0f, outsideViewportColor.getGreen() / 255.0f, outsideViewportColor.getBlue() / 255.0f, outsideViewportColor.getAlpha() / 255.0f);
                this.getAdapter(ViewportView.class).setViewport(defaultViewport, new ChangeEvent());
                view.getAdapter(RegionView.class).setRegion(defaultRegion, new ChangeEvent(new RegionChangedReason(this, defaultRegion)));
        		
                float xOffsetFinal = xOffset;
                float yOffsetFinal = yOffset;
                gl.glClearColor(outsideViewportColor.getRed() / 255.0f, outsideViewportColor.getGreen() / 255.0f, outsideViewportColor.getBlue() / 255.0f, outsideViewportColor.getAlpha() / 255.0f);

                /*ViewportImageSize viewportImageSize = ViewHelper.calculateViewportImageSize(view);
                if (viewportImageSize != null && canvas != null) {
                    if (viewportImageSize.getWidth() < canvas.getWidth()) {
                        xOffsetFinal += (canvas.getWidth() - viewportImageSize.getWidth()) / 2;
                    }
                    if (viewportImageSize.getHeight() < canvas.getHeight()) {
                        yOffsetFinal += canvas.getHeight() - viewportImageSize.getHeight() - yOffsetFinal - (canvas.getHeight() - viewportImageSize.getHeight()) / 2;
                    }
                }*/
                //this.rebuildShaders(gl);
                this.viewport = null;
                //this.canvas.display();

                //displayBody(gl, 0, 0);
            }

        } else {
            float xOffsetFinal = xOffset;
            float yOffsetFinal = yOffset;
        	gl.glClearColor(outsideViewportColor.getRed() / 255.0f, outsideViewportColor.getGreen() / 255.0f, outsideViewportColor.getBlue() / 255.0f, outsideViewportColor.getAlpha() / 255.0f);

           /* ViewportImageSize viewportImageSize = ViewHelper.calculateViewportImageSize(view);
            if (viewportImageSize != null && canvas != null) {
                if (viewportImageSize.getWidth() < canvas.getWidth()) {
                    xOffsetFinal += (canvas.getWidth() - viewportImageSize.getWidth()) / 2;
                }
                if (viewportImageSize.getHeight() < canvas.getHeight()) {
                    yOffsetFinal += canvas.getHeight() - viewportImageSize.getHeight() - yOffsetFinal - (canvas.getHeight() - viewportImageSize.getHeight()) / 2;
                }
            }*/
            displayBody(gl, 0, 0);
        }

        // // fps counter;
        // frame++;
        // long time = System.currentTimeMillis();
        //
        // if (time - timebase > 1000) {
        // float factor = 1000.0f/(time-timebase);
        // float fps = frame*factor;
        // float fps2 = frameUpdated*factor;
        // timebase = time;
        // frame = 0;
        // frameUpdated = 0;
        // System.out.println(fps2 + ", " + fps);
        // }

        // check for errors
        int errorCode = gl.glGetError();
        if (errorCode != GL2.GL_NO_ERROR) {
            GLU glu = new GLU();
            Log.error("OpenGL Error (" + errorCode + ") : " + glu.gluErrorString(errorCode));
        }
    }

    /**
     * Force rebuilding the shaders during the next rendering iteration.
     */
    public void requestRebuildShaders() {
        rebuildShadersRequest = true;
    }

    /**
     * Start rebuilding all shaders.
     * 
     * This function is called, whenever the shader structure of the whole view
     * chain may have changed, e.g. when new views are added.
     * 
     * @param gl
     *            Valid reference to the current gl object
     */
    private void rebuildShaders(GL2 gl) {

        rebuildShadersRequest = false;
        shaderHelper.delAllShaderIDs(gl);

        GLFragmentShaderView fragmentView = view.getAdapter(GLFragmentShaderView.class);
        if (fragmentView != null) {
            // create new shader builder
            GLShaderBuilder newShaderBuilder = new GLShaderBuilder(gl, GL2.GL_FRAGMENT_PROGRAM_ARB);

            // fill with standard values
            GLMinimalFragmentShaderProgram minimalProgram = new GLMinimalFragmentShaderProgram();
            minimalProgram.build(newShaderBuilder);

            // fill with other filters and compile
            fragmentView.buildFragmentShader(newShaderBuilder).compile();
        }

        GLVertexShaderView vertexView = view.getAdapter(GLVertexShaderView.class);
        if (vertexView != null) {
            // create new shader builder
            GLShaderBuilder newShaderBuilder = new GLShaderBuilder(gl, GL2.GL_VERTEX_PROGRAM_ARB);

            // fill with standard values
            GLMinimalVertexShaderProgram minimalProgram = new GLMinimalVertexShaderProgram();
            minimalProgram.build(newShaderBuilder);

            // fill with other filters and compile
            vertexView.buildVertexShader(newShaderBuilder).compile();
        }
    }

    public void activate() {
        if (this.animator != null) {
            this.animator.start();
        }
    }

    public void deactivate() {
        if (this.animator != null) {
            this.animator.stop();
        }
    }

	public Dimension getCanavasSize() {
		return new Dimension(canvas.getWidth(), canvas.getHeight());
	}

	@Override
	public void stop() {
		animator.stop();
	    defaultRegion = view.getAdapter(RegionView.class).getRegion();
    	defaultViewport = this.getAdapter(ViewportView.class).getViewport();
	}

	@Override
	public void start() {
		animator.start();
	    view.getAdapter(RegionView.class).setRegion(defaultRegion, new ChangeEvent());
		//this.getAdapter(ViewportView.class).setViewport(defaultViewport, new ChangeEvent());
		
	}

	
    
    }
