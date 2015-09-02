package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.ShowableDialog;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.MainFrame;


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

    /**
     * Default constructor.
     */
    public AboutDialog() {
        super(MainFrame.SINGLETON, "About JHelioviewer", true);
        setResizable(false);

        JPanel contentPane = new JPanel(new BorderLayout());
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel logo = new JLabel(IconBank.getIcon(JHVIcon.HVLOGO_SMALL));
        logo.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        headerPanel.add(logo, BorderLayout.WEST);

        Font font = this.getFont();

        JEditorPane content = new JEditorPane("text/html", "<html><center><font style=\"font-family: '" + font.getFamily() + "'; font-size: " + font.getSize() + ";\">" + "<b>"
        + "JHelioviewer " + JHVGlobals.VERSION
        + "</b><br>" + '\u00A9' + "2015 ESA JHelioviewer Team<br>" + "Part of the ESA/NASA Helioviewer project<br><br>" + "JHelioviewer is released under the <br>" + "<a href=JHelioviewer.txt>Mozilla Public License Version 2.0</a><br><br>" + "<a href='http://www.jhelioviewer.org'>www.jhelioviewer.org</a><br><br>" + "Contact: <a href='mailto:support@jhelioviewer.org'>Daniel.Mueller@esa.int</a>" + "</font></center></html>");
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.setEditable(false);
        content.setFocusable(false);
        content.setOpaque(false);
        content.addHyperlinkListener(this);
        headerPanel.add(content, BorderLayout.CENTER);
        contentPane.add(headerPanel, BorderLayout.NORTH);
        
        JLabel install4j = new JLabel(new ImageIcon(IconBank.getImage(JHVIcon.INSTALL4J)));
        install4j.setCursor(new Cursor(Cursor.HAND_CURSOR));
        install4j.addMouseListener(new MouseAdapter()
        {
			@Override
			public void mouseClicked(MouseEvent e) {
				JHVGlobals.openURL("http://www.ej-technologies.com/products/install4j/overview.html");
			}
		});
        JLabel raygunIo = new JLabel(IconBank.getIcon(JHVIcon.RAYGUN_IO));
        raygunIo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        raygunIo.addMouseListener(new MouseAdapter()
        {
			@Override
			public void mouseClicked(MouseEvent e) {
				JHVGlobals.openURL("https://raygun.io");
			}
		});
        
        
        JPanel libraries = new JPanel(new FlowLayout(FlowLayout.CENTER,30,30));
        libraries.add(install4j);
        libraries.add(raygunIo);
        
        String text = "<html><font style=\"font-family: '" + font.getFamily() + "'; font-size: " + font.getSize() + ";\">" +

        "This software uses the following libraries:<br/>"+
        "<ul>"+
        "<li><a href=\"http://www.kakadusoftware.com\">Kakadu JPEG2000 Toolkit</a>, \u00A92009 NewSouth Innovations Ltd (NSI) <a href=Kakadu.txt>(License)</a></li>" +
        "<li><a href=\"http://jogamp.org/jogl/www/\">Java" + '\u2122' + " Binding for the OpenGL\u00AE API (JOGL)</a> <a href=\"JOGL.txt\">(License)</a></li>" +
        "<li><a href=\"http://jogamp.org/gluegen/www/\">GlueGen Toolkit</a> <a href=\"GlueGen.txt\">(License)</a></li>" +
        "<li><a href=\"http://logging.apache.org/log4j/index.html\">log4j</a>, \u00A92010 Apache Software Foundation <a href=\"log4j.txt\">(License)</a></li>" +
        "<li><a href=\"http://ffmpeg.org/\">FFmpeg</a> <a href=\"FFmpeg.txt\">(License)</a></li>" +
        "<li>Crystal icons, licensed under LGPL 2.1</li>" +
        "<li><a href=\"http://www.davekoelle.com/alphanum.html\">Alphanum Algorithm</a>, licensed under LGPL 2.1</li>"+
        "</ul>";

        JEditorPane license = new JEditorPane("text/html", text);
        license.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        license.setEditable(false);
        license.setFocusable(false);
        license.setOpaque(false);
        license.addHyperlinkListener(this);
        
        JPanel boxPanel = new JPanel();
        boxPanel.setLayout(new BoxLayout(boxPanel, BoxLayout.PAGE_AXIS));
        boxPanel.add(libraries);
        boxPanel.add(license);

        contentPane.add(boxPanel, BorderLayout.SOUTH);

        JPanel closeButtonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        closeButton.addActionListener(this);
        closeButtonContainer.add(closeButton);
        closeButtonContainer.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        
        /*scrollPane = new JScrollPane(contentPane);
        scrollPane.getVerticalScrollBar().setBlockIncrement(100);
        scrollPane.getVerticalScrollBar().setUnitIncrement(100);*/
        
        setLayout(new BorderLayout());
        add(contentPane);
        add(closeButtonContainer,BorderLayout.SOUTH);
        
        //setPreferredSize(new Dimension(getPreferredSize().width, 500));
        this.setFocusable(true);
    }

    /**
     * {@inheritDoc}
     */
    public void showDialog() {
        pack();
        setSize(getPreferredSize());
        setLocationRelativeTo(MainFrame.SINGLETON);

        DialogTools.setDefaultButtons(closeButton,closeButton);
        
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
                TextDialog textDialog = new TextDialog("License - " + e.getDescription().substring(0, e.getDescription().indexOf('.')), MainFrame.class.getResource("/licenses/" + e.getDescription()));
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
