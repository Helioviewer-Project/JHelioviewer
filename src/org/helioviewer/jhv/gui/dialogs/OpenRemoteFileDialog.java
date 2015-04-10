package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.tree.TreePath;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.Message;
import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.DynamicModel;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;
import org.helioviewer.jhv.io.APIRequestManager;
import org.helioviewer.jhv.io.FileDownloader;

/**
 * Dialog that is used to open user defined JPIP images.
 * 
 * 
 * @author Stephan Pagel
 * @author Andreas Hoelzl
 */
public class OpenRemoteFileDialog extends JDialog implements ShowableDialog, ActionListener {

    // ///////////////////////////////////////////////////////////////////////////////
    // DEFINITIONS
    // ///////////////////////////////////////////////////////////////////////////////

    // weather the advanced or the normal options are currently displayed
    private boolean advancedOptions = false;
    private static final long serialVersionUID = 1L;
    private static JTextField inputAddress = new JTextField();
    private static JTextField imageAddress = new JTextField();
    private JLabel secondLabel = new JLabel("Remote Image Path: ");
    private JButton buttonOpen = new JButton(" Open ");
    private JButton buttonCancel = new JButton(" Cancel ");
    private JButton refresh = new JButton(" Connect ");
    private JButton buttonShow = new JButton(" Advanced Options ");
    private static DynamicModel treeModel;
    private static JTree tree;
    private static String chosenFile = "/";
    private JPanel connectPanel = new JPanel(new BorderLayout());
    private JScrollPane scrollPane = new JScrollPane();
    private static JCheckBox fromJPIP = new JCheckBox("Download From HTTP");

    // ///////////////////////////////////////////////////////////////////////////////
    // METHODS
    // ///////////////////////////////////////////////////////////////////////////////

    /**
     * The constructor that sets the fields and the dialog.
     */
    public OpenRemoteFileDialog() {

        super(ImageViewerGui.getMainFrame(), "Open Remote File", true);

        try {
            if (treeModel == null) {
                treeModel = new DynamicModel(Settings.getProperty("/"));
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        MouseListener mouseListener;

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        // the input text fields
        JPanel northContainer = new JPanel(new BorderLayout(7, 7));

        JPanel labelPanel = new JPanel(new GridLayout(0, 1, 8, 8));
        JPanel fieldPanel = new JPanel(new GridLayout(0, 1, 8, 8));

        // the buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        inputAddress.setPreferredSize(new Dimension(250, 25));

        inputAddress.addActionListener(this);
        imageAddress.addActionListener(this);
        buttonOpen.addActionListener(this);
        buttonCancel.addActionListener(this);
        buttonShow.addActionListener(this);
        imageAddress.setPreferredSize(new Dimension(250, 25));
        buttonOpen.setPreferredSize(new Dimension(90, 25));
        refresh.setPreferredSize(new Dimension(90, 25));
        buttonCancel.setPreferredSize(new Dimension(90, 25));

        try {

            mouseListener = new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    int selRow = tree.getRowForLocation(e.getX(), e.getY());
                    TreePath treePath = tree.getPathForLocation(e.getX(), e.getY());
                    if (selRow != -1) {

                        String path = treePath.toString();
                        if (!path.contains(",")) {
                            buttonOpen.doClick();
                            return;
                        }

                        String parsed = path.substring(path.indexOf(","), path.lastIndexOf(']'));
                        parsed = parsed.replace(",", "");
                        parsed = parsed.replace(" ", "");

                        chosenFile = parsed;
                        if (e.getClickCount() >= 2) {
                            if (parsed.toLowerCase().endsWith(".jp2") || parsed.toLowerCase().endsWith(".jpx")) {
                                buttonOpen.doClick(100);
                            } else {

                            }
                        }
                    }
                }
            };

            if (tree == null) {
                tree = new JTree(treeModel);
                tree.addMouseListener(mouseListener);

            }
            fromJPIP.addActionListener(this);
            refresh.addActionListener(this);
            connectPanel.add(refresh, BorderLayout.EAST);
            connectPanel.add(fromJPIP, BorderLayout.WEST);

            scrollPane = new JScrollPane(tree);

            labelPanel.add(new JLabel("JPIP Server Address: "));

            labelPanel.add(secondLabel);

            fieldPanel.add(inputAddress);
            fieldPanel.add(imageAddress);

            buttonPanel.add(buttonShow);
            buttonPanel.add(buttonCancel);
            buttonPanel.add(buttonOpen);

            northContainer.add(labelPanel, BorderLayout.WEST);
            northContainer.add(fieldPanel, BorderLayout.CENTER);
            northContainer.add(connectPanel, BorderLayout.SOUTH);

            connectPanel.setVisible(false);
            panel.add(northContainer, BorderLayout.NORTH);
            panel.add(scrollPane, BorderLayout.CENTER);
            panel.add(buttonPanel, BorderLayout.SOUTH);
            scrollPane.setVisible(false);
            panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            fromJPIP.setSelected(false);
            inputAddress.setEnabled(true);
            imageAddress.setText("");
            add(panel);
            this.setResizable(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
        inputAddress.setText(Settings.getProperty("default.remote.path"));

    }

    public void init() {
    }

    /**
     * @param arg
     *            the ActionEvent that occured what happens when the ActionEvent
     *            e is fired
     */
    public void actionPerformed(ActionEvent arg) {
        /*
         * when the user wants to have more options the JTree is expanded and
         * the user can choose the files fromt he server he put into the
         * http-field
         */

        if (arg.getSource() == buttonShow) {
            show(arg);
        }

        /*
         * when the the refresh ("Connect") button is pressed a new JTree is
         * loaded
         */
        if (arg.getSource() == refresh) {
            refresh();
        }

        /*
         * the Cancel button is pressed
         */
        if (arg.getSource() == buttonCancel) {
            dispose();
        }

        /*
         * the selected file is streamed from the server
         */
        if (arg.getSource() == buttonOpen) {
            open();
        }

        /*
         * changes the download source from http to jpip or vice versa
         */
        if (arg.getSource() == fromJPIP) {
            changeSource();
        }

    }

    /**
     * changes the download source from http to jpip or vice versa
     * 
     */
    public void changeSource() {
        if (fromJPIP.isSelected() == true) {
            inputAddress.setEnabled(false);
        } else {
            inputAddress.setEnabled(true);
        }
    }

    /**
     * when the user wants to have more options the JTree is expanded and the
     * user can choose the files fromt he server he put into the http-field
     * 
     * @param arg
     *            the actionEvent that has occured
     * 
     */
    public void show(ActionEvent arg) {
        this.setSize(this.getPreferredSize());
        if (advancedOptions == true) {

            connectPanel.setVisible(false);
            secondLabel.setText("Remote Image Path:");
            imageAddress.setText("");
            buttonShow.setText(" Advanced Options");
            inputAddress.setText(Settings.getProperty("default.remote.path"));
            inputAddress.setEnabled(true);
            inputAddress.setEnabled(true);
            scrollPane.setVisible(false);

            this.setSize(this.getPreferredSize());
        } else {
            fromJPIP.setSelected(false);
            connectPanel.setVisible(true);
            secondLabel.setText("Concurrent HTTP Server:   ");

            imageAddress.setText(Settings.getProperty("default.httpRemote.path"));
            inputAddress.setText(Settings.getProperty("default.remote.path"));
            buttonShow.setText(" Basic Options");
            scrollPane.setVisible(true);
            inputAddress.setEnabled(true);
            this.setSize(this.getPreferredSize());
        }
        advancedOptions = !advancedOptions;
    }

    /**
     * When the the refresh ("Connect") button is pressed a new JTree is loaded
     */
    public void refresh() {
        String http = imageAddress.getText();

        if (!http.endsWith("/"))
            http = http + "/";

        if (http.startsWith(" ")) {
            http = http.substring(1);
        }

        imageAddress.setText(http);

        /*
         * if the JPIP server and the HTTP Server are concurrent
         */
        try {
            URI urlHttpServer = new URI(imageAddress.getText());
            URI urlJpipServer = new URI(inputAddress.getText());

            if (urlHttpServer.getHost() == null) {
                Message.err("Invalid HTTP Server Address", "", false);
                return;
            }

            if (urlJpipServer.getHost() == null && fromJPIP.isSelected() == false) {
                Message.err("Invalid JPIP Server Address", "", false);
                return;
            }

            if (urlHttpServer.getHost() != null && urlJpipServer.getHost() != null && (!urlHttpServer.getHost().equals(urlJpipServer.getHost()))) {
                if (advancedOptions) {
                    Message.err("JPIP and HTTP address do not fit.", "", false);
                    return;
                }
            }

        } catch (URISyntaxException e) {
            Message.err("Invalid server address.", "", false);
            return;
        }

        try {

            String text = imageAddress.getText();
            if (!text.endsWith("/")) {
                text = text + "/";
            }

            treeModel = new DynamicModel(imageAddress.getText());

            tree.setModel(treeModel);

            tree.getParent().setSize(tree.getParent().getPreferredSize());

            Settings.setProperty("default.httpRemote.path", imageAddress.getText());

            Settings.setProperty("default.remote.path", inputAddress.getText());

            tree.getParent().getParent().repaint();
        } catch (BadLocationException i) {

            Message.err("No .jp2 or .jpx on the server.", "", false);

        } catch (IOException i) {
            Message.err("The requested URL was not found or you have no access to it.", "", false);
        }
    }

    /**
     * downloads the selected file via http, stores it in the emote folder of
     * JHelioViewer and loads it locally from there
     */
    public void downloadFromHTTP() {

        if (tree.getLastSelectedPathComponent() != null) {
            if (!tree.getModel().isLeaf(tree.getLastSelectedPathComponent())) {
                tree.expandPath(tree.getSelectionPath());
                return;
            }
        }

        String srv = Settings.getProperty("default.httpRemote.path");

        srv = srv.trim();

        if (srv.endsWith("/"))
            srv = srv.substring(0, srv.length() - 1);

        imageAddress.setText(srv);

        String img = chosenFile;

        img = img.trim();
        if (!img.startsWith("/"))
            img = "/" + img;

        setVisible(false);
        ImageViewerGui.getSingletonInstance().getMainImagePanel().setLoading(true);

        try {
            final URI uri = new URI(srv + img);
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    ImageViewerGui.getSingletonInstance().getMainImagePanel().setLoading(true);
                    FileDownloader filedownloader = new FileDownloader();
                    URI newUri = filedownloader.downloadFromHTTP(uri, true);
                    ImageViewerGui.getSingletonInstance().getMainImagePanel().setLoading(false);
                    try {
                        APIRequestManager.newLoad(newUri, uri, true);
                        dispose();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        ImageViewerGui.getSingletonInstance().getMainImagePanel().setLoading(false);
                        GuiState3DWCS.mainComponentView.getComponent().repaint();
                    }

                }

            });
            thread.setDaemon(true);
            thread.start();

        } catch (URISyntaxException e1) {
            e1.printStackTrace();
        }

    }

    /**
     * The selected file is streamed from the server.
     */
    public void open() {

        if (tree.getLastSelectedPathComponent() != null) {
            if (!tree.getModel().isLeaf(tree.getLastSelectedPathComponent())) {
                tree.expandPath(tree.getSelectionPath());
                return;
            }
        }
        if (fromJPIP.isSelected() == true) {
            downloadFromHTTP();
            return;
        }

        String srv = inputAddress.getText();
        if (advancedOptions) {
            srv = Settings.getProperty("default.remote.path");
        }
        srv = srv.trim();

        if (srv.endsWith("/"))
            srv = srv.substring(0, srv.length() - 1);

        inputAddress.setText(srv);
        String img = "";

        if (advancedOptions == true) {
            img = chosenFile;
        } else {
            img = imageAddress.getText();
        }
        img = img.trim();
        if (!img.startsWith("/"))
            img = "/" + img;

        final String httpPath;

        if (advancedOptions) {
            httpPath = Settings.getProperty("default.httpRemote.path") + img;
        } else {
            httpPath = srv + img;
        }

        ImageViewerGui.getSingletonInstance().getMainImagePanel().setLoading(true);

        try {
            final URI uri = new URI(srv + img);
            final OpenRemoteFileDialog parent = this;
            Thread thread = new Thread(new Runnable() {

                public void run() {
                    try {
                        APIRequestManager.newLoad(uri, new URI(httpPath), true);

                        if (advancedOptions == false) {
                            Settings.setProperty("default.remote.path", inputAddress.getText());
                        }
                    } catch (IOException e) {

                        JOptionPane.showMessageDialog(buttonShow, e.getMessage(), "File not found on streaming server!", JOptionPane.ERROR_MESSAGE);

                        ImageViewerGui.getSingletonInstance().getMainImagePanel().setLoading(false);
                        GuiState3DWCS.mainComponentView.getComponent().repaint();
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    } finally {
                        ImageViewerGui.getSingletonInstance().getMainImagePanel().setLoading(false);
                        parent.dispose();
                    }
                }
            });
            thread.setDaemon(true);
            thread.start();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            setVisible(true);
            ImageViewerGui.getSingletonInstance().getMainImagePanel().setLoading(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void showDialog() {
        pack();
        setSize(getPreferredSize());

        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        setVisible(true);
    }
}
