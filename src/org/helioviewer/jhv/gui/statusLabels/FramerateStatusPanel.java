package org.helioviewer.jhv.gui.statusLabels;

import java.awt.Dimension;
import java.time.LocalDateTime;

import javax.swing.BorderFactory;

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
public class FramerateStatusPanel extends StatusLabel{

    private static final long serialVersionUID = 1L;
    private int counter = 0;
    private long currentMillis = 0;
    private static final String TITLE = "fps:";
    
    /**
     * Default constructor.
     */
    public FramerateStatusPanel(){
    	super();
        setBorder(BorderFactory.createEtchedBorder());

        setPreferredSize(new Dimension(70, 20));
        setText(TITLE);

        currentMillis = System.currentTimeMillis();

    }

    private void updateFramerate() {
            setVisible(true);
            setText(TITLE + counter);
            counter = 0;
            currentMillis = System.currentTimeMillis();
    }

	public void timeStampChanged(LocalDateTime current, LocalDateTime last) {
		if ((System.currentTimeMillis() - currentMillis) >= 1000)
           updateFramerate();
		counter++;
	}	
}
