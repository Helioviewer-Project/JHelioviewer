package org.helioviewer.jhv.gui.dialogs;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

class DialogTools
{
    public static void setDefaultButtons(final JButton _acceptBtn,final JButton _cancelBtn)
    {
        JRootPane root=SwingUtilities.getRootPane(_acceptBtn);
        root.setDefaultButton(_acceptBtn);
        
        String CANCEL_ACTION_KEY = "CANCEL_ACTION_KEY";
        KeyStroke escapeKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKey, CANCEL_ACTION_KEY);
        root.getActionMap().put(CANCEL_ACTION_KEY, new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                _cancelBtn.doClick();
            }
        });
    }

}
