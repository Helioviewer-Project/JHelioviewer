package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.gui.dialogs.ShortcutsDialog;

public class ShowDialogAction extends AbstractAction
{
	private @Nullable JDialog previousInstance;
    private Class<JDialog> dialogToShow;

    /**
     * Default constructor.
     * 
     * @param name
     *            name of the action that shall be displayed on a button
     * @param dialog
     *            Dialog to open on click
     */
    @SuppressWarnings("unchecked")
    public <T extends JDialog> ShowDialogAction(String name, Class<T> dialog)
    {
        super(name);
        
        dialogToShow = (Class<JDialog>) dialog;

        if (dialog.isAssignableFrom(ShortcutsDialog.class))
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
    }
    
    /**
     * Default constructor.
     * 
     * @param name
     *            name of the action that shall be displayed on a button
     * @param icon
     *            icon of the action that shall be displayed on a button
     * @param dialog
     *            Dialog to open on click
     */
    @SuppressWarnings("unchecked")
    public <T extends JDialog> ShowDialogAction(String name, ImageIcon icon, Class<T> dialog)
    {
        super(name, icon);

        dialogToShow = (Class<JDialog>) dialog;

        if (dialog.isAssignableFrom(ShortcutsDialog.class))
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
    }

    public void actionPerformed(@Nullable ActionEvent e)
    {
        try
        {
        	if(previousInstance!=null && previousInstance.isVisible())
        		previousInstance.requestFocus();
        	else
        		previousInstance = dialogToShow.newInstance();
        }
        catch (Exception e1)
        {
        	Telemetry.trackException(e1);
        }
    }
}
