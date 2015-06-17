package org.helioviewer.jhv.plugins.sdocutoutplugin;

import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.SwingConstants;

public class SDOCutOutToggleButton extends JButton implements
		PropertyChangeListener {

	private static final long serialVersionUID = 1L;

	private static final String PATH = "/images/";
	public SDOCutOutToggleButton() {
		super(new SDOCutOutAction());
		initButton();
	}

	private void initButton() {
		setSelected(false);
		setIcon(SDOCutOutToggleButton
				.getIcon(SDOCutOutSettings.ICON_FILENAME,24, 24));
		setToolTipText("Connect to SDO Cut-Out Service");
		setEnabled(true);
		setVerticalTextPosition(SwingConstants.BOTTOM);
		setHorizontalAlignment(SwingConstants.CENTER);
		setHorizontalTextPosition(SwingConstants.CENTER);
	}
	
	
	

    public static ImageIcon getIcon(String fileName, int width, int height){
        URL imgURL = SDOCutOutPlugin3D.getResourceUrl(PATH + fileName);
        System.out.println(imgURL);
        ImageIcon imageIcon = new ImageIcon(imgURL);
        Image image = imageIcon.getImage();
        image = image.getScaledInstance(width, height, Image.SCALE_AREA_AVERAGING);
        imageIcon.setImage(image);
        return imageIcon;
    	
    }

	@Override
	/*
	 * This method is called by the event firePropertyChange to add the plugin
	 * button in the TopToolBar
	 */
	public void propertyChange(PropertyChangeEvent evt) {
	}
}
