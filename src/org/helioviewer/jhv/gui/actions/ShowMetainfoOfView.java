package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

/**
 * Action to close the active layer.
 * 
 * @author Markus Langenberg
 */
public class ShowMetainfoOfView extends AbstractAction {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public ShowMetainfoOfView() {
        super("Show metainfo...", IconBank.getIcon(JHVIcon.INFO));
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
        //LayersModel.getSingletonInstance().showMetaInfo(view);
    }

}
