package org.helioviewer.jhv.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.Message;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.actions.filefilters.AllSupportedImageTypesFilter;
import org.helioviewer.jhv.gui.actions.filefilters.FitsFilter;
import org.helioviewer.jhv.gui.actions.filefilters.JP2Filter;
import org.helioviewer.jhv.gui.actions.filefilters.JPGFilter;
import org.helioviewer.jhv.gui.actions.filefilters.PNGFilter;
import org.helioviewer.jhv.io.APIRequestManager;

/**
 * Action to open a local file.
 * 
 * <p>
 * Opens a file chooser dialog, opens the selected file. Currently supports the
 * following file extensions: "jpg", "jpeg", "png", "fts", "fits", "jp2" and
 * "jpx"
 * 
 * @author Markus Langenberg
 */
public class OpenLocalFileAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public OpenLocalFileAction() {
        super("Open...");
        putValue(SHORT_DESCRIPTION, "Open image");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
        final JFileChooser fileChooser = JHVGlobals.getJFileChooser(Settings.getProperty("default.local.path"));
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.addChoosableFileFilter(new JP2Filter());
        fileChooser.addChoosableFileFilter(new FitsFilter());
        fileChooser.addChoosableFileFilter(new PNGFilter());
        fileChooser.addChoosableFileFilter(new JPGFilter());
        fileChooser.setFileFilter(new AllSupportedImageTypesFilter());
        fileChooser.setMultiSelectionEnabled(false);
        
        int retVal = fileChooser.showOpenDialog(ImageViewerGui.getMainFrame());
        if (retVal == JFileChooser.APPROVE_OPTION)
        {
            File selectedFile = fileChooser.getSelectedFile();

            if (selectedFile.exists() && selectedFile.isFile()) {

                // remember the current directory for future
                Settings.setProperty("default.local.path", fileChooser.getSelectedFile().getParent());

                ImageViewerGui.getSingletonInstance().getMainImagePanel().setLoading(true);

                // Load image in new thread
                Thread thread = new Thread(new Runnable() {

                    public void run() {
                        try {
                            APIRequestManager.newLoad(fileChooser.getSelectedFile().toURI(), true);
                        } catch (IOException e) {
                            Message.err("An error occured while opening the file!", e.getMessage(), false);
                        } finally {
                            ImageViewerGui.getSingletonInstance().getMainImagePanel().setLoading(false);
                        }
                    }
                }, "OpenLocalFile");
                thread.setDaemon(true);
                thread.start();
            }
        }
    }
}
