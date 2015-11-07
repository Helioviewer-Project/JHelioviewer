package org.helioviewer.jhv.gui.dialogs;

import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.gui.MainFrame;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

class TextDialog extends JDialog implements ActionListener
{
    private JButton closeButton;

    public TextDialog(String title, URL textFile)
    {
        super(MainFrame.SINGLETON, title, true);
        
    	Telemetry.trackEvent("Dialog", "Type", getClass().getSimpleName(), "Title", title);

        setResizable(false);

        StringBuilder text = new StringBuilder();
        try(BufferedReader br = new BufferedReader(new InputStreamReader(textFile.openStream(),StandardCharsets.UTF_8)))
        {
            String line;
            while ((line=br.readLine())!=null)
            {
                text.append(line);
                text.append('\n');
            }
        }
        catch (Exception e)
        {
        	Telemetry.trackException(e);
        }
        
        JTextArea textArea = new JTextArea(text.toString());
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(scrollPane.getPreferredSize().width + 50, 500));
        add(scrollPane, BorderLayout.NORTH);

        closeButton = new JButton("Close");
        closeButton.addActionListener(this);
        
        JPanel closeButtonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        closeButtonContainer.add(closeButton);
        closeButtonContainer.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        add(closeButtonContainer, BorderLayout.SOUTH);
        
        pack();
        setSize(getPreferredSize());
        setLocationRelativeTo(MainFrame.SINGLETON);
        
        DialogTools.setDefaultButtons(closeButton,closeButton);
        
        setVisible(true);
    }

    public void actionPerformed(@Nullable ActionEvent e)
    {
        dispose();
    }
}
