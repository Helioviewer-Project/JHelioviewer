package org.helioviewer.jhv.gui.components.layerTable.newLayerTable;

import java.awt.CardLayout;
import java.awt.Component;

import javax.swing.JPanel;

import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.layers.LayerInterface;
import org.helioviewer.jhv.layers.NewLayerListener;

/**
 * GUI Component showing either the LayerTable, or a notice that currently no
 * layers exists.
 * 
 * The class updates/decides which component to show based on callback methods
 * called by the LayersModel, by implementing the LayersListener interface
 * 
 * @author Malte Nuhn
 */
public class NewLayerTableContainer extends JPanel implements NewLayerListener {

    private static final long serialVersionUID = 4954283312509735677L;

    CardLayout cl = new CardLayout();

    /**
     * Construct a new LayerTableContainer
     * 
     * @param table
     *            - the Component to show if at least one layer exists
     * @param empty
     *            - the Component to show if no layers exist
     */
    public NewLayerTableContainer(Component table, Component empty) {
        super();
        this.setLayout(cl);
        this.add(empty, "empty");
        this.add(table, "table");

        GuiState3DWCS.layers.addNewLayerListener(this);
        update();

    }

    /**
     * Sync the state of this component with the state of the LayersModel
     */
    public void update() {
        if (GuiState3DWCS.layers.getLayerCount() == 0) {
            cl.show(this, "empty");
        } else {
            cl.show(this, "table");
        }
    }

	@Override
	public void newlayerAdded() {
		update();
	}

	@Override
	public void newlayerRemoved(int idx) {
		update();
	}

	@Override
	public void newtimestampChanged() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void activeLayerChanged(LayerInterface layer) {
		// TODO Auto-generated method stub
		
	}

}
