package org.helioviewer.jhv.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.KeyStroke;

import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.actions.filefilters.ExtensionFileFilter;
import org.helioviewer.jhv.gui.actions.filefilters.JPGFilter;
import org.helioviewer.jhv.gui.actions.filefilters.PNGFilter;

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
    private static final String SETTING_SCREENSHOT_USE_CURRENT_OPENGL_SIZE = "export.screenshot.use.current.opengl.size";
    private static final String SETTING_SCREENSHOT_EXPORT_LAST_DIRECTORY = "export.screenshot.last.directory";

    private boolean useCurrentOpenGlSize;
    private int imageWidth;
    private int imageHeigth;
    /**
     * Default constructor.
     */
    public SaveScreenshotAsAction() {
        super("Save Screenshot As...");
        putValue(SHORT_DESCRIPTION, "Save Screenshot to Chosen Folder");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.SHIFT_DOWN_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
        this.loadSettings();
    	final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.addChoosableFileFilter(new JPGFilter());
        fileChooser.addChoosableFileFilter(new PNGFilter());

        Settings settings = Settings.getSingletonInstance();
        String val;
        try {
            val = settings.getProperty(SETTING_SCREENSHOT_EXPORT_LAST_DIRECTORY);
            if (val != null && !(val.length() == 0)) {
                fileChooser.setCurrentDirectory(new File(val));
            }
        } catch (Throwable t) {
            Log.error(t);
        }
        
        fileChooser.setSelectedFile(new File(fileChooser.getCurrentDirectory() + "/" + SaveScreenshotAction.getDefaultFileName()));
        int retVal = fileChooser.showSaveDialog(ImageViewerGui.getMainFrame());

        if (retVal == JFileChooser.APPROVE_OPTION) {
        	settings.setProperty(SETTING_SCREENSHOT_EXPORT_LAST_DIRECTORY, fileChooser.getCurrentDirectory().getPath() + "/");
        	settings.save();
            File selectedFile = fileChooser.getSelectedFile();

            ExtensionFileFilter fileFilter = (ExtensionFileFilter) fileChooser.getFileFilter();

            if (!fileFilter.accept(selectedFile)) {
                selectedFile = new File(selectedFile.getPath() + "." + fileFilter.getDefaultExtension());
            }

            try {
            	ImageViewerGui.getSingletonInstance().getMainView().stop();
            	if (this.useCurrentOpenGlSize) ImageViewerGui.getSingletonInstance().getMainView().saveScreenshot(fileFilter.getDefaultExtension(), selectedFile);        	
            	else ImageViewerGui.getSingletonInstance().getMainView().saveScreenshot(fileFilter.getDefaultExtension(), selectedFile, this.imageWidth, this.imageHeigth);
            	ImageViewerGui.getSingletonInstance().getMainView().start();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }
    
    private void loadSettings(){
		Settings settings = Settings.getSingletonInstance();
        String val;  
        
        try {
            val = settings.getProperty(SETTING_SCREENSHOT_USE_CURRENT_OPENGL_SIZE);
            if (val != null && !(val.length() == 0)) {
                useCurrentOpenGlSize = Boolean.parseBoolean(val);
            }
        } catch (Throwable t) {
            Log.error(t);
        }

        
        try {
            val = settings.getProperty(SETTING_SCREENSHOT_IMG_HEIGHT);
            if (val != null && !(val.length() == 0)) {
                this.imageHeigth = Integer.parseInt(val);
            }
        } catch (Throwable t) {
            Log.error(t);
        }
        
        try {
            val = settings.getProperty(SETTING_SCREENSHOT_IMG_WIDTH);
            if (val != null && !(val.length() == 0)) {
            	this.imageWidth = Integer.parseInt(val);
            }
        } catch (Throwable t) {
            Log.error(t);
        }
	}
}
