package org.helioviewer.jhv.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.viewmodel.view.LayeredView;

/**
 * Action to terminate the application.
 * 
 * @author Markus Langenberg
 */
public class ExitProgramAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public ExitProgramAction() {
        super("Quit");
        putValue(SHORT_DESCRIPTION, "Quit program");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {

        if (GuiState3DWCS.mainComponentView != null) {
            if (GuiState3DWCS.mainComponentView.getAdapter(LayeredView.class).getNumberOfVisibleLayer() > 0) {
                int option = JOptionPane.showConfirmDialog(ImageViewerGui.getMainFrame(), "Are you sure you want to quit?", "Confirm", JOptionPane.OK_CANCEL_OPTION);
                if (option == JOptionPane.CANCEL_OPTION) {
                    return;
                }
            }
        }

        System.exit(0);
    }

}
