package org.helioviewer.jhv.gui.components.newComponents;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.text.ParseException;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.layerTable.LayerTable;
import org.helioviewer.jhv.gui.components.layerTable.newLayerTable.NewLayerTable;
import org.helioviewer.jhv.gui.components.layerTable.newLayerTable.NewLayerTableContainer;
import org.helioviewer.jhv.gui.dialogs.observation.ImageDataPanel;
import org.helioviewer.jhv.layers.LayerInterface;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.layers.NewLayerListener;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.jp2view.ImmutableDateTime;
import org.helioviewer.jhv.viewmodel.view.jp2view.JHVJPXView;

public class NewImageSelectorPanel extends JPanel implements NewLayerListener {

    private static final long serialVersionUID = 1L;

    private final ImageDataPanel observationImagePane = new ImageDataPanel();

    /**
     * Observation dialog to actually add new data
     */
    private final AddLayerPanel addLayerDialog = new AddLayerPanel();
    //private final ObservationDialog observationDialog = ObservationDialog.getSingletonInstance();

    /**
     * Action to add a new layer. If there is a current active layer which much
     * different time, the dates will be updated.
     */
    private Action addLayerAction = new AbstractAction("Add Layer", IconBank.getIcon(JHVIcon.ADD_NEW, 16, 16)) {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;
        {
            putValue(SHORT_DESCRIPTION, "Add a new Layer");
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent arg0) {
            // Check the dates if possible
            final View activeView = LayersModel.getSingletonInstance().getActiveView();

            /*
             * TODO: Code Simplification - Cleanup Date selection when clicking
             * on "add images", e.g. use LayersModel.getLatestDate(...), ...
             * 
             * Here are some more comments by Helge:
             * 
             * If it is a local file, the timestamps are read from the parsed
             * JPX movie, i.e. a call will pause until the whole movie has
             * finished loading.
             * 
             * If it has been reading through the API the frame time stamps
             * already have been returned and it is not bad. For the time being
             * it will only update if its already loaded.
             * 
             * I think there should be a better solution? Maybe a wait dialog?
             * etc.?
             */

            if (activeView != null) {
                JHVJPXView tmv = activeView.getAdapter(JHVJPXView.class);
                if (tmv != null && tmv.getMaximumAccessibleFrameNumber() == tmv.getMaximumFrameNumber()) {
                    final ImmutableDateTime start = LayersModel.getSingletonInstance().getStartDate(activeView);
                    final ImmutableDateTime end = LayersModel.getSingletonInstance().getEndDate(activeView);
                    if (start != null && end != null) {
                        try {
                            Date startDate = start.getTime();
                            Date endDate = end.getTime();
                            Date obsStartDate;
                            Date obsEndDate;
                            synchronized(ImageDataPanel.API_DATE_FORMAT)
                            {
                                obsStartDate = ImageDataPanel.API_DATE_FORMAT.parse(observationImagePane.getStartTime());
                                obsEndDate = ImageDataPanel.API_DATE_FORMAT.parse(observationImagePane.getEndTime());
                            }
                            
                            // only updates if its really necessary with a
                            // tolerance of an hour
                            final int tolerance = 60 * 60 * 1000;
                            if (Math.abs(startDate.getTime() - obsStartDate.getTime()) > tolerance || Math.abs(endDate.getTime() - obsEndDate.getTime()) > tolerance) {
                                observationImagePane.setStartDate(startDate);
                                observationImagePane.setEndDate(endDate);
                            }
                        } catch (ParseException e) {
                            // Should not happen
                            System.err.println("Cannot update observation dialog");
                            e.printStackTrace();
                        }
                    }
                }
            }
            // Show dialog
            addLayerDialog.setVisible(true);
        }
    };
    /**
     * Button to add new layers
     */
    private JButton addLayerButton = new JButton(addLayerAction);

    /**
     * Action to download the current layer. If there is no active layer, the
     * action will do nothing.
     * <p>
     * Should be activated accordingly in the class
     */
    private Action downloadLayerAction = new AbstractAction() {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;
        {
            putValue(SHORT_DESCRIPTION, "Download the currently selected Layer");
            putValue(SMALL_ICON, IconBank.getIcon(JHVIcon.DOWNLOAD_NEW, 16, 16));
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent arg0) {
            if (LayersModel.getSingletonInstance().getActiveView() != null) {
                LayersModel.getSingletonInstance().downloadLayer(LayersModel.getSingletonInstance().getActiveView());
            }
        }
    };
    /**
     * Button to show {@link #downloadLayerAction}
     */
    private JButton downloadLayerButton = new JButton(downloadLayerAction);

    private NewLayerTable layerTable;
  
    /**
     * Action to show the meta data. If there is no active layer, the action
     * will do nothing.
     * <p>
     * Should be activated accordingly in the class
     */
    private Action showMetaAction = new AbstractAction() {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;
        {
            putValue(SHORT_DESCRIPTION, "Show the Metainformation of the currently selected Layer");
            putValue(SMALL_ICON, IconBank.getIcon(JHVIcon.INFO_NEW, 16, 16));
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent arg0) {
        	if (LayersModel.getSingletonInstance().getActiveView() != null) {
                LayersModel.getSingletonInstance().showMetaInfo(LayersModel.getSingletonInstance().getActiveView());
            }
        }
    };
    /**
     * Button to show {@link #showMetaAction}
     */
    private JButton showMetaButton = new JButton(showMetaAction);

    /**
     * Default constructor.
     */
    public NewImageSelectorPanel() {
        // set up observation dialog
        //observationDialog.addUserInterface("Image data", observationImagePane);

        // add components
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        addLayerButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
        southPanel.add(showMetaButton);
        southPanel.add(downloadLayerButton);
        southPanel.add(addLayerButton);

        layerTable = new NewLayerTable();

        // Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(layerTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JLabel emptyLabel = new JLabel("No layers added yet", JLabel.CENTER);
        emptyLabel.setFont(emptyLabel.getFont().deriveFont(Font.ITALIC));
        emptyLabel.setHorizontalTextPosition(JLabel.CENTER);
        emptyLabel.setOpaque(true);
        emptyLabel.setBackground(Color.WHITE);

        // Create the scroll pane and add the table to it.
        JScrollPane emptyScrollPane = new JScrollPane(emptyLabel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        emptyScrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        NewLayerTableContainer layerTableContainer = new NewLayerTableContainer(scrollPane, emptyScrollPane);

        // Add the scroll pane to this panel.
        layerTableContainer.setPreferredSize(new Dimension(ImageViewerGui.SIDE_PANEL_WIDTH, LayerTable.ROW_HEIGHT * 4 + 2));

        this.setLayout(new BorderLayout());

        this.add(layerTableContainer, BorderLayout.CENTER);
        this.add(southPanel, BorderLayout.SOUTH);

		GuiState3DWCS.layers.addNewLayerListener(this);
        activateActions();
    }

    /**
     * Checks if there is a current active layer and activates the buttons
     * accordingly.
     * <p>
     * Since the events can come from different threads it takes care that this
     * runs in the EventQueue.
     */
    private void activateActions() {
    	downloadLayerAction.setEnabled(true);
        showMetaAction.setEnabled(true);
    }

    /**
     * {@inheritDoc}
     */
    public void activeLayerChanged(int index) {
    }

    /**
     * {@inheritDoc}
     */
    public void layerAdded(int newIndex) {
        activateActions();
    }

    /**
     * {@inheritDoc}
     */
    public void layerChanged(int index) {
    }

    /**
     * {@inheritDoc}
     */
    public void layerRemoved(View oldView, int oldIndex) {
        activateActions();
    }

	@Override
	public void newlayerAdded() {
		activateActions();
	}

	@Override
	public void newlayerRemoved(int idx) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void newtimestampChanged() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void activeLayerChanged(LayerInterface layer) {
		
	}
}
