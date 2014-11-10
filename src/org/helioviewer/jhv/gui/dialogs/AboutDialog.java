package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;
import org.helioviewer.jhv.internal_plugins.InternalPlugin;
import org.helioviewer.jhv.plugins.viewmodelplugin.controller.PluginContainer;
import org.helioviewer.jhv.plugins.viewmodelplugin.controller.PluginManager;
import org.helioviewer.jhv.plugins.viewmodelplugin.interfaces.Plugin;

/**
 * Dialog that is used to display information about the program.
 * 
 * <p>
 * This includes version and contact informations.
 * 
 * @author Markus Langenberg
 * @author Andre Dau
 */
public final class AboutDialog extends JDialog implements ActionListener, ShowableDialog, HyperlinkListener {

    private static final long serialVersionUID = 1L;

    private final JButton closeButton = new JButton("Close");
    private JScrollPane scrollPane;

    /**
     * Default constructor.
     */
    public AboutDialog() {
        super(ImageViewerGui.getMainFrame(), "About JHelioviewer", true);
        setLayout(new BorderLayout());
        setResizable(false);

        JPanel contentPane = new JPanel(new BorderLayout());

        JLabel logo = new JLabel(IconBank.getIcon(JHVIcon.HVLOGO_SMALL));
        logo.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPane.add(logo, BorderLayout.WEST);

        Font font = this.getFont();

        JEditorPane content = new JEditorPane("text/html", "<html><center><font style=\"font-family: '" + font.getFamily() + "'; font-size: " + font.getSize() + ";\">" + "<b>" + "JHelioviewer " + JHVGlobals.VERSION + "</b><br>" + '\u00A9' + "2014 ESA JHelioviewer Team<br>" + "Part of the ESA/NASA Helioviewer project<br><br>" + "JHelioviewer is released under the <br>" + "<a href=JHelioviewer.txt>Mozilla Public License Version 2.0</a><br><br>" + "<a href='http://www.jhelioviewer.org'>www.jhelioviewer.org</a><br><br>" + "Contact: <a href='mailto:Daniel.Mueller@esa.int'>Daniel.Mueller@esa.int</a>" + "</font></center></html>");
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.setEditable(false);
        content.setOpaque(false);
        content.addHyperlinkListener(this);
        contentPane.add(content, BorderLayout.CENTER);

        JPanel boxPanel = new JPanel();
        boxPanel.setLayout(new BoxLayout(boxPanel, BoxLayout.PAGE_AXIS));

        boxPanel.add(new JSeparator());
        String text = "<html><font style=\"font-family: '" + font.getFamily() + "'; font-size: " + font.getSize() + ";\">" +

        "This software uses the <a href=http://www.kakadusoftware.com>Kakadu JPEG2000 Toolkit</a>,<br> " + '\u00A9' + " 2009, NewSouth Innovations Ltd (NSI), <a href=Kakadu.txt>(License)</a><br>" +

        "<p>This software uses the <a href=http://jogamp.org/jogl/www/>Java" + '\u2122' + " Binding for the OpenGL" + '\u00AE' + " API (JOGL)</a>,<br> maintained by the JogAmp Community, <a href=JOGL2.txt>(License)</a><br>" +

        "<p>This software uses the <a href=http://jogamp.org/gluegen/www/>GlueGen Toolkit</a>,<br> maintained by the JogAmp Community, <a href=GlueGen.txt>(License)</a><br>" +

        "<p>This software uses the <a href=http://developer.nvidia.com/object/cg_toolkit.html>Cg Compiler</a>,<br>" + '\u00A9' + " 2009, NVIDIA Corp., <a href=Cg.txt>(License)</a><br>" +

        "<p>This software uses <a href=http://logging.apache.org/log4j/index.html>log4j from the Apache Logging Services Project</a>,<br>" + '\u00A9' + " 2010, Apache Software Foundation, <a href=log4j.txt>(License)</a><br>" +

        "<p>This software uses libraries from the <a href=http://ffmpeg.org/>FFmpeg project</a>,<br> licensed under the <a href=FFmpeg.txt>LGPLv2.1</a>.<br>" +

        "<p>This software uses the Chrystal Project, licensed under the LGPL.<br> Its source code can be downloaded <a href=\"http://everaldo.com/crystal/?action=downloads\">here</a>.<br>" +

        "<p>This software uses the <a href=\"http://www.davekoelle.com/alphanum.html\">Alphanum Algorithm</a>, licensed under the LGPLv2.1.<br> Its source code can be downloaded <a href=\"http://jhelioviewer.org/libjhv/external/AlphanumComparator.java\">here</a>.<br>";

        for (PluginContainer pluginContainer : PluginManager.getSingeltonInstance().getAllPlugins()) {
            Plugin plugin = pluginContainer.getPlugin();
            if (!(plugin instanceof InternalPlugin)) {
                String pluginAboutLicense = plugin.getAboutLicenseText();

                if (pluginAboutLicense != null && !pluginAboutLicense.equals("")) {
                    text += pluginAboutLicense;
                }

            }
        }

        JEditorPane license = new JEditorPane("text/html", text);

        license.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        license.setEditable(false);
        license.setOpaque(false);
        license.addHyperlinkListener(this);
        boxPanel.add(license);

        JPanel closeButtonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        closeButton.addActionListener(this);
        closeButtonContainer.add(closeButton);
        boxPanel.add(closeButtonContainer);

        contentPane.add(boxPanel, BorderLayout.SOUTH);

        scrollPane = new JScrollPane(contentPane);
        scrollPane.getVerticalScrollBar().setBlockIncrement(100);
        scrollPane.getVerticalScrollBar().setUnitIncrement(100);
        add(scrollPane);

        setPreferredSize(new Dimension(getPreferredSize().width + 50, 600));
    }

    /**
     * {@inheritDoc}
     */
    public void showDialog() {
        pack();
        setSize(getPreferredSize());
        setLocationRelativeTo(ImageViewerGui.getMainFrame());

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                scrollPane.getVerticalScrollBar().setValue(0);
            }
        });

        setVisible(true);
    }

    /**
     * Closes the dialog.
     */
    public void actionPerformed(ActionEvent a) {
        if (a.getSource() == this.closeButton) {
            this.dispose();
        }
    }

    /**
     * Opens a browser or email client after clicking on a hyperlink.
     */
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            if (e.getURL() == null) {
                TextDialog textDialog = new TextDialog("License - " + e.getDescription().substring(0, e.getDescription().indexOf('.')), ImageViewerGui.class.getResource("/licenses/" + e.getDescription()));
                textDialog.showDialog();
            } else {
                JHVGlobals.openURL(e.getURL().toString());
            }
        }
    }

    @Override
	public void init()
    {
	}
}
