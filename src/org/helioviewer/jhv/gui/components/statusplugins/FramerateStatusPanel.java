package org.helioviewer.jhv.gui.components.statusplugins;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Timer;

import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.viewmodel.view.LinkedMovieManager;

/**
 * Status panel for displaying the framerate for image series.
 * 
 * <p>
 * The information of this panel is always shown for the active layer.
 * 
 * <p>
 * This panel is not visible, if the active layer is not an image series.
 * 
 * @author Markus Langenberg
 */
public class FramerateStatusPanel extends ViewStatusPanelPlugin {

    private static final long serialVersionUID = 1L;
    private Timer timer;
    private int counter = 0;
    private long last = -1;
    /**
     * Default constructor.
     */
    public FramerateStatusPanel(){
        setBorder(BorderFactory.createEtchedBorder());

        setPreferredSize(new Dimension(70, 20));
        setText("fps:");

        setVisible(true);
        LayersModel.getSingletonInstance().addLayersListener(this);
        
        timer = new Timer(1000, new ActionListener() {
    		
    		@Override
    		public void actionPerformed(ActionEvent e) {
    			updateFramerate();
    			counter = 0;
    			
    		}
    	});
        timer.start();
        
    }

    private void updateFramerate() {
            setVisible(true);
            setText("fps: " + counter);
    }
    
    private void count(){
    	if (LinkedMovieManager.getActiveInstance() != null && LinkedMovieManager.getActiveInstance().getMasterMovie() != null){
    		long current = LinkedMovieManager.getActiveInstance().getMasterMovie().getCurrentFrameDateTime().getMillis();
    		if (last >= 0 && last != current) counter++;
    		last = current;
    	}
    }

    public void activeLayerChanged(int idx) {
    }

    public void subImageDataChanged() {
        count();
    }
    
    public void timestampChanged(){
    	count();
    }
}
