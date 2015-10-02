package org.helioviewer.jhv.gui;

import java.awt.Toolkit;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import org.helioviewer.jhv.Telemetry;

public class Clipboard implements ClipboardOwner
{
    private final static Clipboard SINGLETON = new Clipboard();

    private Clipboard()
    {
    }

    @Override
    public void lostOwnership(java.awt.datatransfer.Clipboard aClipboard, Transferable aContents)
    {
    }

    public static void setString(String data)
    {
        StringSelection stringSelection = new StringSelection(data);
        java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
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
            	Telemetry.trackException(e);
            }

        return "";
    }
}