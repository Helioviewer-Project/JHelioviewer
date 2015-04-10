package org.helioviewer.jhv.gui.components.layerTable.newLayerTable;

import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.layers.LayerInterface;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.layers.NewLayerListener;

/**
 * A TableModel representing the state of visible Layers, internally using the
 * LayersModel
 * 
 * @author Malte Nuhn
 * 
 */
public class NewLayerTableModel extends AbstractTableModel implements NewLayerListener {

    private static final long serialVersionUID = 1167923521718778146L;

    public static final int COLUMN_VISIBILITY = 0;
    public static final int COLUMN_TITLE = 1;
    public static final int COLUMN_TIMESTAMP = 2;
    public static final int COLUMN_BUTTON_REMOVE = 3;

    /** The sole instance of this class. */
    private static final NewLayerTableModel SINGLETON = new NewLayerTableModel();

    /**
     * Returns the only instance of this class.
     * 
     * @return the only instance of this class.
     * */
    public static NewLayerTableModel getSingletonInstance() {
        return SINGLETON;
    }

    private NewLayerTableModel() {
    	GuiState3DWCS.layers.addNewLayerListener(this);
    }

    /**
     * {@inheritDoc}
     */
    public int getRowCount() {
        return GuiState3DWCS.layers.getLayerCount();
    }

    /**
     * {@inheritDoc} Hardcoded value of columns. This value is dependent on the
     * actual design of the LayerTable
     */
    public int getColumnCount() {
        return 4;
    }

    /**
     * Return the LayerDescriptor for the given row of the table, regardless
     * which column is requested.
     */
    public Object getValueAt(int row, int col) {

        int idx = row;

        return LayersModel.getSingletonInstance().getDescriptor(idx);

    }

	@Override
	public void newlayerAdded() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                fireTableRowsInserted(GuiState3DWCS.layers.getLayerCount(),GuiState3DWCS.layers.getLayerCount());
            }
        });
	}

	@Override
	public void newlayerRemoved(final int idx) {
	    SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                fireTableRowsDeleted(idx, idx);
            }
        });
   	}

	@Override
	public void newtimestampChanged() {
		SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	fireTableDataChanged();
            }
        });
	}

	@Override
	public void activeLayerChanged(LayerInterface layer) {
		// TODO Auto-generated method stub
		
	}
}
