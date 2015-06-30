package org.helioviewer.jhv.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.JHVException.MetaDataException;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.actions.filefilters.ExtensionFileFilter;
import org.helioviewer.jhv.gui.actions.filefilters.JPGFilter;
import org.helioviewer.jhv.gui.actions.filefilters.PNGFilter;
import org.helioviewer.jhv.layers.LayerInterface;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine;

/**
 * Action to save a screenshot in desired image format at desired location.
 * 
 * <p>
 * Therefore, opens a save dialog to choose format, name and location.
 * 
 * @author Markus Langenberg
 */
public class SaveScreenshotAsAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private static final String SETTING_SCREENSHOT_IMG_WIDTH = "export.screenshot.image.width";
    private static final String SETTING_SCREENSHOT_IMG_HEIGHT = "export.screenshot.image.height";
    private static final String SETTING_SCREENSHOT_TEXT = "export.screenshot.text";
    private static final String SETTING_SCREENSHOT_EXPORT_LAST_DIRECTORY = "export.screenshot.last.directory";

    private int imageWidth;
    private int imageHeight;

	private boolean textEnabled;
    /**
     * Default constructor.
     */
    public SaveScreenshotAsAction() {
        super("Save screenshot as...");
        putValue(SHORT_DESCRIPTION, "Save screenshots to a file");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.SHIFT_DOWN_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
        this.loadSettings();
    	final JFileChooser fileChooser = JHVGlobals.getJFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(false);
        JPGFilter firstFilter = new JPGFilter();
        fileChooser.addChoosableFileFilter(firstFilter);
        fileChooser.addChoosableFileFilter(new PNGFilter());
        fileChooser.setFileFilter(firstFilter);
        String val;
        try {
            val = Settings.getProperty(SETTING_SCREENSHOT_EXPORT_LAST_DIRECTORY);
            if (val != null && !(val.length() == 0)) {
                fileChooser.setCurrentDirectory(new File(val));
            }
        } catch (Throwable t) {
            System.err.println(t);
        }
        
        fileChooser.setSelectedFile(new File(fileChooser.getCurrentDirectory() + "/" + this.getDefaultFileName()));
        int retVal = fileChooser.showSaveDialog(MainFrame.SINGLETON);
        
        if (retVal == JFileChooser.APPROVE_OPTION) {
        	Settings.setProperty(SETTING_SCREENSHOT_EXPORT_LAST_DIRECTORY, fileChooser.getCurrentDirectory().getPath() + "/");
        	File selectedFile = fileChooser.getSelectedFile();

            ExtensionFileFilter fileFilter = (ExtensionFileFilter) fileChooser.getFileFilter();

            if (!fileFilter.accept(selectedFile)) {
                selectedFile = new File(selectedFile.getPath() + "." + fileFilter.getDefaultExtension());
            }

            ArrayList<String> descriptions = null;
			if (textEnabled) {
				descriptions = new ArrayList<String>();
				for (LayerInterface layer : Layers.getLayers()) {
					if (layer.isVisible()) {
						try {
							LocalDateTime currentDateTime = TimeLine.SINGLETON.getCurrentDateTime();
							descriptions.add(layer.getMetaData(currentDateTime).getFullName()
									+ " - "
									+ layer.getTime().format(JHVGlobals.DATE_TIME_FORMATTER));
						} catch (MetaDataException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}
			}

			try {
				ImageIO.write(MainFrame.MAIN_PANEL.getBufferedImage(this.imageWidth, this.imageHeight, descriptions),
						fileFilter.getDefaultExtension(), selectedFile);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        }
    }
    
    private void loadSettings(){
        String val;          
		try {
			val = Settings.getProperty(SETTING_SCREENSHOT_TEXT);
			if (val != null && !(val.length() == 0)) {
				this.textEnabled = Boolean.parseBoolean(val);
			}
		} catch (Throwable t) {
			System.err.println(t);
		}

        try {
            val = Settings.getProperty(SETTING_SCREENSHOT_IMG_HEIGHT);
            if (val != null && !(val.length() == 0)) {
                this.imageHeight = Integer.parseInt(val);
            }
        } catch (Throwable t) {
            System.err.println(t);
        }
        
        try {
            val = Settings.getProperty(SETTING_SCREENSHOT_IMG_WIDTH);
            if (val != null && !(val.length() == 0)) {
            	this.imageWidth = Integer.parseInt(val);
            }
        } catch (Throwable t) {
            System.err.println(t);
        }
        

        if(imageWidth==0)
          imageWidth=1280;

        if(imageHeight==0)
          imageHeight=720;
	}
    
    /**
     * Returns the default name for a screenshot. The name consists of
     * "JHV_screenshot_created" plus the current system date and time.
     * 
     * @return Default name for a screenshot.
     */
    private String getDefaultFileName() {
        String output = new String("JHV_screenshot_created_");

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
        output += dateFormat.format(new Date());

        return output;
    }
}
