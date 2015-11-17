package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collections;

import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.gui.MainFrame;

/**
 * Dialog that is used to display information about the developers
 */
public final class CreditsDialog extends JDialog
{
    public CreditsDialog(Component _parent)
    {
        super(MainFrame.SINGLETON, "Credits", true);
        
    	Telemetry.trackEvent("Dialog", "Type", getClass().getSimpleName());
    	
        setResizable(false);

        setLayout(new BorderLayout());

        JEditorPane content = new JEditorPane("text/html", "<html><font style=\"font-family: '" + getFont().getFamily() + "'; font-size: " + getFont().getSize() + "px;\">"
    	        + "Many people participated in the development of JHelioviewer. The JHelioviewer<br/>"
    	        + " team would like to acknowledge the following contributors:</font></html>");
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.setEditable(false);
        content.setFocusable(false);
        content.setOpaque(false);
        
        
        add(new JLabel("Many people participated in the development of JHelioviewer. The JHelioviewer"
    	        + " team would like to acknowledge the following contributors:"), BorderLayout.NORTH);
        
        add(content, BorderLayout.NORTH);
        
        
		String[] names = new String[] { "Alen Alexanderian", "Andre Dau", "Benjamin W. Caplins", "Benjamin Wamsler",
				"Bram Bourgoignie", "Daniel Müller", "Desmond Amadigwe", "Freek Verstringe", "Jonas Schwammberger",
				"Juan Pablo Garcia Ortiz", "Ludwig Schmidt", "Malte Nuhn", "Markus Langenberg", "Simon Felix",
				"Simon Spörri", "Stefan Meier", "Stephan Pagel", "Helge Dietert", "Andreas Hoelzl" };
        
        Collections.shuffle(Arrays.asList(names));
        
		JPanel nameGrid=new JPanel(new GridLayout(0, 3));
		for(String name:names)
		{
			JLabel l=new JLabel(name);
			l.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
			nameGrid.add(l);
		}
        add(nameGrid);
        
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener()
        {
			@Override
			public void actionPerformed(@Nullable ActionEvent _arg0)
			{
				dispose();
			}});
        
        JPanel closeButtonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        closeButtonContainer.add(closeButton);
        closeButtonContainer.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        add(closeButtonContainer,BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(_parent);

        DialogTools.setDefaultButtons(closeButton,closeButton);
        
        setVisible(true);
    }
}
