package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;

public class TextDialog extends JDialog implements ActionListener, ShowableDialog {

    private static final long serialVersionUID = 1L;
    private JButton closeButton;

    public TextDialog(String title, URL textFile) {
        super(MainFrame.SINGLETON, title, true);
        setResizable(false);

        StringBuffer text = new StringBuffer();
        try(BufferedReader br = new BufferedReader(new InputStreamReader(textFile.openStream(),StandardCharsets.UTF_8)))
        {
            String line;
            while ((line=br.readLine())!=null)
            {
                text.append(line);
                text.append('\n');
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        init(text.toString());
    }

    private void init(String text) {
        JTextArea textArea = new JTextArea(text);
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
    }

    public void actionPerformed(ActionEvent e) {
        this.dispose();
    }

    public void showDialog()
    {
        pack();
        setSize(getPreferredSize());
        setLocationRelativeTo(MainFrame.SINGLETON);
        
        DialogTools.setDefaultButtons(closeButton,closeButton);
        
        setVisible(true);
    }

	@Override
	public void init() {
	}
}
