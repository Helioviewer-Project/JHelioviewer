package org.helioviewer.jhv.gui;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * Simple helper for accessing the user's clipboard.
 * 
 * @author Malte Nuhn
 * 
 */
public final class ClipBoardCopier implements ClipboardOwner {

    private final static ClipBoardCopier SINGLETON = new ClipBoardCopier();

    /**
     * Returns the only instance of this class.
     * 
     * @return the only instance of this class.
     * */
    public final static ClipBoardCopier getSingletonInstance() {
        return SINGLETON;
    }

    private ClipBoardCopier() {
    }

    public void lostOwnership(Clipboard aClipboard, Transferable aContents) {
        // do nothing
    }

    /**
     * Set the content of the clipboard
     * 
     * @param data
     *            content to write to the clipboard
     */
    public void setString(String data) {
        StringSelection stringSelection = new StringSelection(data);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, this);
    }

    /**
     * Read the current content from the clipboard.
     * 
     * @return clipboard content
     */
    public String getString() {
        String result = "";
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        boolean hasTransferableText = (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
        if (hasTransferableText) {
            try {
                result = (String) contents.getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}