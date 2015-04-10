package org.helioviewer.jhv.gui.dialogs.observation;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.Message;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.dialogs.DialogTools;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;

/**
 * The Observation Dialog provides the main interface to get data from the
 * Helioviewer or other servers.
 * 
 * In order to select data a panel has to be added to the dialog. The Panel has
 * to be derived from ObservationDialogPanel.
 * 
 * @author Stephan Pagel
 * */
public class ObservationDialog extends JDialog implements ActionListener, ShowableDialog {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;

    private static final ObservationDialog SINGLETON = new ObservationDialog();

    private final HashMap<String, ObservationDialogPanel> uiMap = new HashMap<String, ObservationDialogPanel>();

    private final JPanel contentPane = new JPanel();
    private final JPanel uiSelectionPane = new JPanel();
    private final JComboBox<String> uiSelectionComboBox = new JComboBox<String>();
    private final JPanel buttonPane = new JPanel();
    private final JButton btnAddLayer = new JButton("Add Layer");
    private final JButton btnCancel = new JButton("Cancel");

    private ObservationDialogPanel selectedPane = null;

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    /**
     * Default constructor.
     * */
    private ObservationDialog() {
        super(ImageViewerGui.getMainFrame(), true);

        initVisualComponents();
    }

    /**
     * Returns the singleton instance of the dialog.
     * */
    public static ObservationDialog getSingletonInstance() {
        return SINGLETON;
    }

    /**
     * Sets up the visual sub components and the component itself.
     * */
    private void initVisualComponents() {
        // set dialog settings
        setTitle("Add Layer");
        setContentPane(contentPane);

        // set basic layout
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
        contentPane.setBorder(BorderFactory.createEmptyBorder(3, 9, 1, 9));
        contentPane.setFocusable(true);

        // set up components
        uiSelectionPane.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
        uiSelectionPane.setBorder(BorderFactory.createTitledBorder(" Choose type of data to add "));
        uiSelectionPane.add(new JLabel("Data type"));
        uiSelectionPane.add(uiSelectionComboBox);

        uiSelectionComboBox.addActionListener(this);

        buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 3, 3));
        
        if(JHVGlobals.isWindows())
        {
            buttonPane.add(btnAddLayer);
            buttonPane.add(btnCancel);
        }
        else
        {
            buttonPane.add(btnCancel);
            buttonPane.add(btnAddLayer);
        }

        btnAddLayer.setIcon(IconBank.getIcon(JHVIcon.ADD));
        btnAddLayer.setToolTipText("Request the selected image data and display it");

        btnCancel.setIcon(IconBank.getIcon(JHVIcon.REMOVE_LAYER));
        btnCancel.setToolTipText("Close this Dialog");

        final int btnWidth = Math.max(btnCancel.getPreferredSize().getSize().width, btnAddLayer.getPreferredSize().getSize().width);

        btnAddLayer.setPreferredSize(new Dimension(btnWidth, 25));
        btnAddLayer.addActionListener(this);

        btnCancel.setPreferredSize(new Dimension(btnWidth, 25));
        btnCancel.addActionListener(this);
    }

    /**
     * Deactivates and activates the old and new displayed panel.
     * */
    private void setUIContainerPane(final String name) {
        if (selectedPane != null) {
            selectedPane.deselected();
            selectedPane = null;
        }

        if (name != null) {
            selectedPane = uiMap.get(name);

            if (selectedPane != null) {
                selectedPane.selected();
            }
        }

        resetContentPane();
    }

    /**
     * Removes previous panel from the UI and sets the new selected panel to the
     * UI.
     * */
    private void resetContentPane() {
        contentPane.removeAll();

        if (uiMap.size() > 1) {
            contentPane.add(uiSelectionPane);
        }

        if (selectedPane != null) {
            contentPane.add(selectedPane);
        }

        contentPane.add(buttonPane);

        contentPane.revalidate();
        pack();
    }

    /**
     * Allows a component or plug-in to add its panel to the dialog in order to
     * select the corresponding data.
     * */
    public void addUserInterface(final String name, final ObservationDialogPanel userInterface) {
        uiMap.put(name, userInterface);
        uiSelectionComboBox.addItem(name);
    }

    /**
     * Allows a component or plug-in to remove its panel from the dialog.
     * */
    public void removeUserInterface(final String name, final ObservationDialogPanel userInterface) {
        uiMap.remove(name);
        uiSelectionComboBox.removeItem(name);
    }

    /**
     * Returns the panel which is connected with the given name.
     * */
    public ObservationDialogPanel getUserInterface(final String name) {
        return uiMap.get(name);
    }

    /**
     * Enables the load data button.
     * */
    public void setLoadButtonEnabled(final boolean enable) {
        btnAddLayer.setEnabled(enable);
    }

    /**
     * Shows up the dialog and initializes the UI with the panel of the given
     * name.
     * */
    public void showDialog(final String dataSourceName) {
        if (uiMap.size() <= 0) {
            Message.err("Error", "There are no data sources available!", false);
            return;
        }

        for (final ObservationDialogPanel pane : uiMap.values()) {
            pane.dialogOpened();
        }

        if (dataSourceName != null) {
            uiSelectionComboBox.setSelectedItem(dataSourceName);
        } else {
            uiSelectionComboBox.setSelectedIndex(0);
        }

        setLocationRelativeTo(ImageViewerGui.getMainFrame());

        pack();

        DialogTools.setDefaultButtons(btnAddLayer,btnCancel);
        setVisible(true);
    }

    /**
     * Closes the dialog.
     * */
    private void closeDialog() {
        setVisible(false);
        dispose();
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Showable Dialog
    // //////////////////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public void showDialog()
    {
        showDialog(null);
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Action Listener
    // //////////////////////////////////////////////////////////////////////////////

    /**
     * Reacts on user input.
     * */
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource().equals(uiSelectionComboBox)) {
            setUIContainerPane((String) uiSelectionComboBox.getSelectedItem());
        } else if (e.getSource().equals(btnAddLayer)) {
            boolean result = true;
            
            if (selectedPane != null) {
                result = selectedPane.loadButtonPressed();
            }

            if (result) {
                closeDialog();
            }
        } else if (e.getSource().equals(btnCancel)) {
            selectedPane.cancelButtonPressed();
            closeDialog();
        }
    }

	@Override
	public void init() {
	}
}
