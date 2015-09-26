package org.helioviewer.jhv.gui;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class ClipBoard implements ClipboardOwner
{
    private final static ClipBoard SINGLETON = new ClipBoard();

    private ClipBoard()
    {
    }

    public void lostOwnership(Clipboard aClipboard, Transferable aContents)
    {
    }

    public static void setString(String data)
    {
        StringSelection stringSelection = new StringSelection(data);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, SINGLETON);
    }

    public static String getString()
    {
        Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        
        if ((contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor))
            try
            {
                return (String) contents.getTransferData(DataFlavor.stringFlavor);
            }
            catch (UnsupportedFlavorException | IOException e)
            {
                e.printStackTrace();
            }

        return "";
    }
}