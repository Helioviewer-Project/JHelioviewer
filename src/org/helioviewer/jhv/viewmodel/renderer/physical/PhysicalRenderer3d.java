package org.helioviewer.jhv.viewmodel.renderer.physical;

import org.helioviewer.jhv.viewmodel.view.View;

/**
 * Interface be enable a class to draw to the screen.
 * 
 * <p>
 * By implementing this interface, a class is able to draw geometric figures and
 * images onto the main image. Therefore, it also has to register itself with
 * the component responsible for drawing the main image (e.g. by calling
 * {@link org.helioviewer.jhv.viewmodel.view.OverlayView#setRenderer(PhysicalRenderer3d)}
 * When drawing the image, the managing component will call this interface,
 * providing a PhysicalRenderGraphics object, to allow the implementing class to
 * draw its contents to the screen.
 * 
 * <p>
 * Note, that for drawing, a physical coordinate system is used. As base unit,
 * it uses kilometers for both axis.
 * 
 * @author Stephan Pagel
 * @author Markus Langenberg
 * 
 */
public abstract class PhysicalRenderer3d {

    /**
     * Callback function that will be called during rendering the image.
     * 
     * <p>
     * The implementing class should put everything it wants to render within
     * this function.
     * 
     * @param g
     *            render object provided by caller
     */
    public void render(GLPhysicalRenderGraphics g){
    	
    }
    
    public void viewChanged(View view){
    	
    }

}
