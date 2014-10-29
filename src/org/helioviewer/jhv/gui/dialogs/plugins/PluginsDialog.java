package org.helioviewer.jhv.gui.dialogs.plugins;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;
import org.helioviewer.jhv.internal_plugins.InternalPlugin;
import org.helioviewer.viewmodelplugin.controller.PluginContainer;
import org.helioviewer.viewmodelplugin.controller.PluginManager;

/**
 * The Plug-in Dialog allows to manage all available plug-ins. Plug-ins can be
 * added, removed or enabled / disabled.
 * 
 * @author Stephan Pagel
 * */
public class PluginsDialog extends JDialog implements ShowableDialog, ActionListener, WindowListener, ListEntryChangeListener {

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;

    private boolean changesMade = false;

    private static final Dimension DIALOG_SIZE_MINIMUM = new Dimension(400, 500);
    private static final Dimension DIALOG_SIZE_PREFERRED = new Dimension(400, 500);

    private final JPanel contentPane = new JPanel();

    private final JComboBox<String> filterComboBox = new JComboBox<String>(new String[] { "All", "Enabled", "Disabled" });

    private final JLabel emptyLabel = new JLabel("No Plug-ins available", JLabel.CENTER);
    private final List pluginList = new List();
    private final JScrollPane emptyScrollPane = new JScrollPane(emptyLabel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    private final JPanel listContainerPane = new JPanel();
    private final CardLayout listLayout = new CardLayout();

    private final JButton okButton = new JButton("Ok", IconBank.getIcon(JHVIcon.CHECK));

    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

    /**
     * Default constructor.
     * */
    public PluginsDialog() {
        super(ImageViewerGui.getMainFrame(), "Plug-in Manager", true);

        initVisualComponents();
    }

    /**
     * Initialize the visual parts of the component.
     */
    private void initVisualComponents() {
        // dialog
        setMinimumSize(DIALOG_SIZE_MINIMUM);
        setPreferredSize(DIALOG_SIZE_PREFERRED);
        setContentPane(contentPane);
        addWindowListener(this);

        // header
        final StringBuilder headerText = new StringBuilder();
        headerText.append("<html><font style=\"font-family: '" + getFont().getFamily() + "'; font-size: " + getFont().getSize() + ";\">");
        headerText.append("<b>JHelioviewer Plug-ins</b>");
        headerText.append("<p style=\"padding-left:10px\">");
        headerText.append("Manage available plug-ins of JHelioviewer.<br>");
        headerText.append("You can import enable or delete plug-ins.<br>"); // TODO
                                                                            // SP:
                                                                            // add
                                                                            // "download"
        headerText.append("Press the information buttons to get additional details.");
        headerText.append("</p></font></html>");

        final JEditorPane headerPane = new JEditorPane("text/html", headerText.toString());
        headerPane.setBorder(BorderFactory.createEmptyBorder(3, 3, 6, 3));
        headerPane.setEditable(false);
        headerPane.setOpaque(false);

        // center - installed plug-ins
        final JPanel installedFilterPane = new JPanel();
        installedFilterPane.setLayout(new FlowLayout(FlowLayout.LEFT));
        installedFilterPane.add(new JLabel("Filter"));
        installedFilterPane.add(filterComboBox);

        filterComboBox.addActionListener(this);

        // ////////
        pluginList.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        emptyScrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        emptyLabel.setFont(emptyLabel.getFont().deriveFont(Font.ITALIC));
        emptyLabel.setHorizontalTextPosition(JLabel.CENTER);
        emptyLabel.setOpaque(true);
        emptyLabel.setBackground(Color.WHITE);

        listContainerPane.setLayout(listLayout);
        listContainerPane.add(emptyScrollPane, "empty");
        listContainerPane.add(pluginList, "list");

        pluginList.addListEntryChangeListener(this);

        // ////////
        final JPanel installedButtonPane = new JPanel();
        installedButtonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
        // installedButtonPane(downloadButton); //TODO SP: add

        final JPanel installedPane = new JPanel();
        installedPane.setLayout(new BorderLayout());
        installedPane.setBorder(BorderFactory.createTitledBorder(" Installed Plug-ins "));
        installedPane.add(installedFilterPane, BorderLayout.PAGE_START);
        installedPane.add(listContainerPane, BorderLayout.CENTER);
        installedPane.add(installedButtonPane, BorderLayout.PAGE_END);

        // center - sequence arrangement
        final JPanel managePane = new JPanel();
        managePane.setLayout(new FlowLayout(FlowLayout.LEFT));
        managePane.setBorder(BorderFactory.createTitledBorder(" Filter and Overlays "));

        // center
        final JPanel centerPane = new JPanel();
        centerPane.setLayout(new BorderLayout());
        centerPane.add(installedPane, BorderLayout.CENTER);
        centerPane.add(managePane, BorderLayout.PAGE_END);

        // footer
        final JPanel footer = new JPanel();
        footer.setLayout(new FlowLayout(FlowLayout.RIGHT));
        footer.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
        footer.add(okButton);

        okButton.setToolTipText("Closes the dialog.");
        okButton.addActionListener(this);

        // content pane
        contentPane.setLayout(new BorderLayout());
        contentPane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        contentPane.add(headerPane, BorderLayout.PAGE_START);
        contentPane.add(centerPane, BorderLayout.CENTER);
        contentPane.add(footer, BorderLayout.PAGE_END);
    }

    /**
     * Updates visual components according to the current state.
     * */
    private void updateVisualComponents() {
        if (pluginList.getNumberOfItems() > 0) {
            listLayout.show(listContainerPane, "list");
        } else {
            switch (filterComboBox.getSelectedIndex()) {
            case 0:
                emptyLabel.setText("No Plug-ins available");
                break;
            case 1:
                emptyLabel.setText("No Plug-ins enabled");
                break;
            case 2:
                emptyLabel.setText("No Plug-ins disabled");
                break;
            }

            listLayout.show(listContainerPane, "empty");
        }
    }

    /**
     * This method will close the dialog and handles things which have to be
     * done before. This includes saving the settings and rebuild the viewchains
     * with the current activated plug ins.
     */
    private void closeDialog() {
        if (changesMade) {
            // rebuild the view chains
            //recreateViewChains();

            // save plug-in settings to XML file
            PluginManager.getSingeltonInstance().saveSettings();
        }

        // close dialog
        dispose();
    }


    /**
     * Removes all entries from the plug-in list and adds all available plug-ins
     * to the list again.
     * */
    private void updatePluginList() {
        final PluginContainer[] plugins = PluginManager.getSingeltonInstance().getAllPlugins();
        final int filterIndex = filterComboBox.getSelectedIndex();

        final PluginListEntry entry = (PluginListEntry) pluginList.getSelectedEntry();
        final String selectedPlugin = entry == null ? null : entry.getPluginContainer().getName();

        pluginList.removeAllEntries();

        for (final PluginContainer plugin : plugins) {
            if (!(plugin.getPlugin() instanceof InternalPlugin)) {
                if (filterIndex == 0 || (plugin.isActive() && filterIndex == 1) || (!plugin.isActive() && filterIndex == 2)) {
                    pluginList.addEntry(plugin.getName(), new PluginListEntry(plugin, pluginList));
                }
            }
        }

        pluginList.selectItem(selectedPlugin);

        updateVisualComponents();
    }

    // ////////////////////////////////////////////////////////////////
    // Showable Dialog
    // ////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     * */
    public void showDialog() {
        changesMade = false;

        updatePluginList();
        pluginList.selectFirstItem();

        pack();
        setSize(getPreferredSize());
        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        setVisible(true);
    }

    // ////////////////////////////////////////////////////////////////
    // Action Listener
    // ////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     * */
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource().equals(okButton)) {
            closeDialog();
        } else if (e.getSource().equals(filterComboBox)) {
            updatePluginList();
        }
    }

    // ////////////////////////////////////////////////////////////////
    // Window Listener
    // ////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     * */
    public void windowActivated(final WindowEvent e) {
    }

    /**
     * {@inheritDoc}
     * */
    public void windowClosed(final WindowEvent e) {
    }

    /**
     * {@inheritDoc}
     * */
    public void windowClosing(final WindowEvent e) {
        closeDialog();
    }

    /**
     * {@inheritDoc}
     * */
    public void windowDeactivated(final WindowEvent e) {
    }

    /**
     * {@inheritDoc}
     * */
    public void windowDeiconified(final WindowEvent e) {
    }

    /**
     * {@inheritDoc}
     * */
    public void windowIconified(final WindowEvent e) {
    }

    /**
     * {@inheritDoc}
     * */
    public void windowOpened(final WindowEvent e) {
    }

    // ////////////////////////////////////////////////////////////////
    // List Entry Change Listener
    // ////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public void itemChanged() {
        changesMade = true;

        updatePluginList();
    }

    /**
     * {@inheritDoc}
     */
    public void listChanged() {
        updateVisualComponents();
    }

    // ////////////////////////////////////////////////////////////////
    // JAR Filter
    // ////////////////////////////////////////////////////////////////

    @Override
	public void init() {
	}
}