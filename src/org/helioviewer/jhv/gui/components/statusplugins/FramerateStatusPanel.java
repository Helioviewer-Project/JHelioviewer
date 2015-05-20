package org.helioviewer.jhv.gui.components.statusplugins;

import java.awt.Dimension;
import java.time.LocalDateTime;

import javax.swing.BorderFactory;

import org.helioviewer.jhv.viewmodel.timeline.TimeLine;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine.TimeLineListener;

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
public class FramerateStatusPanel extends ViewStatusPanelPlugin implements TimeLineListener {

    private static final long serialVersionUID = 1L;
    private int counter = 0;
    private long last = -1;
    private long currentMillis = 0;
    /**
     * Default constructor.
     */
    public FramerateStatusPanel(){
        setBorder(BorderFactory.createEtchedBorder());

        setPreferredSize(new Dimension(70, 20));
        setText("fps:");

        setVisible(true);
        currentMillis = System.currentTimeMillis();
        TimeLine.SINGLETON.addListener(this);

    }

    private void updateFramerate() {
            setVisible(true);
            setText("fps: " + counter);
            counter = 0;
            System.out.println("fps : " + (System.currentTimeMillis() - currentMillis));
            currentMillis = System.currentTimeMillis();
    }

    public void activeLayerChanged(int idx) {
    }

	@Override
	public void timeStampChanged(LocalDateTime localDateTime) {
		if ((System.currentTimeMillis() - currentMillis) >= 1000)
           updateFramerate();
		counter++;
	}

	@Override
	public void dateTimesChanged(int framecount) {
	}
}
