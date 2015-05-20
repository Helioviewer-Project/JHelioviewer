package org.helioviewer.jhv.gui.components;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.util.AbstractList;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.interfaces.ImagePanelInputController;
import org.helioviewer.jhv.gui.interfaces.ImagePanelPlugin;
import org.helioviewer.jhv.viewmodel.view.opengl.MainPanel;

import com.jogamp.opengl.awt.GLCanvas;


/**
 * This class represents a basic image component that is used to display the
 * image of all images.
 * 
 * @author Stephan Pagel
 * 
 * */
public class BasicImagePanel extends JPanel implements ComponentListener{

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;

    protected MainPanel componentView;

    protected ImagePanelInputController inputController;

    protected AbstractList<ImagePanelPlugin> plugins;

    protected GLCanvas renderedImageComponent;


    protected boolean updateViewportView = true;

    protected Image backgroundImage;

    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

    /**
     * Default constructor.
     * */
    public BasicImagePanel() {
        super(new BorderLayout(0, 0));
        setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        // initialize list of plugins
        plugins = new LinkedList<ImagePanelPlugin>();

        // initialize container for post renderer

        // add component listener
        addComponentListener(this);
    }

    public void setUpdateViewportView(boolean doUpdate) {
        updateViewportView = doUpdate;
    }

    public void setBackgroundImage(Image img) {
        backgroundImage = img;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null)
            g.drawImage(backgroundImage, 0, 0, null);
    }

    /**
     * Adds an mouse listener to the component.
     */
    public void addMouseListener(MouseListener l) {
        if (renderedImageComponent != null)
            renderedImageComponent.addMouseListener(l);
    }

    /**
     * Adds an mouse motion listener to the component.
     */
    public void addMouseMotionListener(MouseMotionListener l) {
        if (renderedImageComponent != null)
            renderedImageComponent.addMouseMotionListener(l);
    }

    /**
     * Adds an mouse wheel listener to the component.
     */
    public void addMouseWheelListener(MouseWheelListener l) {
        if (renderedImageComponent != null)
            renderedImageComponent.addMouseWheelListener(l);
    }

    /**
     * Removes an mouse listener from the component.
     */
    public void removeMouseListener(MouseListener l) {
        if (renderedImageComponent != null)
            renderedImageComponent.removeMouseListener(l);
    }

    /**
     * Removes an mouse listener from the component.
     */
    public void removeMouseMotionListener(MouseMotionListener l) {
        if (renderedImageComponent != null)
            renderedImageComponent.removeMouseMotionListener(l);
    }

    /**
     * Removes an mouse listener from the component.
     */
    public void removeMouseWheelListener(MouseWheelListener l) {
        if (renderedImageComponent != null)
            renderedImageComponent.removeMouseWheelListener(l);
    }


    /**
     * Sets the component view which acts as the last view in the associated
     * view chain and provides the data for this component.
     * 
     * @param newView
     *            new component view.
     */
    
    /**
     * Returns the associated input controller.
     * 
     * @return input controller of this component.
     */
    public ImagePanelInputController getInputController() {
        return inputController;
    }


    // ////////////////////////////////////////////////////////////////
    // Component Listener
    // ////////////////////////////////////////////////////////////////

    /**
     * Method will be called when component was hidden.
     */
    public void componentHidden(ComponentEvent e) {
    }

    /**
     * Method will be called when component was moved.
     */
    public void componentMoved(ComponentEvent e) {
    }

    /**
     * Method will be called when component was resized. Resets the viewport and
     * region.
     */

    /**
     * Method will be called when component was shown.
     */
    public void componentShown(ComponentEvent e) {
    }


	@Override
	public void componentResized(ComponentEvent e) {
		// TODO Auto-generated method stub
		
	}
}