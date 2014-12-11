package org.helioviewer.jhv.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.actions.filefilters.ExtensionFileFilter;
import org.helioviewer.jhv.gui.actions.filefilters.JPGFilter;
import org.helioviewer.jhv.gui.actions.filefilters.PNGFilter;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.opengl.model.GL3DImageLayer;
import org.helioviewer.jhv.opengl.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.jhv.viewmodel.view.opengl.GL3DSceneGraphView;

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
    	final JFileChooser fileChooser = new JFileChooser();
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
        int retVal = fileChooser.showSaveDialog(ImageViewerGui.getMainFrame());
        
        if (retVal == JFileChooser.APPROVE_OPTION) {
        	Settings.setProperty(SETTING_SCREENSHOT_EXPORT_LAST_DIRECTORY, fileChooser.getCurrentDirectory().getPath() + "/");
        	File selectedFile = fileChooser.getSelectedFile();

            ExtensionFileFilter fileFilter = (ExtensionFileFilter) fileChooser.getFileFilter();

            if (!fileFilter.accept(selectedFile)) {
                selectedFile = new File(selectedFile.getPath() + "." + fileFilter.getDefaultExtension());
            }

            ArrayList<String> descriptions = null;
			if (textEnabled) {
				GL3DSceneGraphView scenegraphView = GuiState3DWCS.mainComponentView.getAdapter(GL3DSceneGraphView.class);
				descriptions = new ArrayList<String>();
				int counter = 0;
				for (GL3DImageLayer layer : scenegraphView.getLayers().getLayers()){
					if (!layer.isDrawBitOn(Bit.Hidden)){
						descriptions.add(LayersModel.getSingletonInstance()
								.getDescriptor(counter).title
								+ " - "
								+ LayersModel.getSingletonInstance().getDescriptor(
										counter).timestamp.replaceAll(" ", " - "));
					}
					counter++;
				}
			}
            GuiState3DWCS.mainComponentView.saveScreenshot(fileFilter.getDefaultExtension(), selectedFile, this.imageWidth, this.imageHeight, descriptions);
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
