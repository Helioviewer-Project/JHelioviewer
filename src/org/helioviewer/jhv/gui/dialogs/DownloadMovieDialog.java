package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;

import javax.swing.JDialog;
import javax.swing.JProgressBar;

import org.helioviewer.jhv.gui.MainFrame;

public class DownloadMovieDialog extends JDialog{

	private JProgressBar progressBar;
	
	public DownloadMovieDialog() {
		super(MainFrame.SINGLETON, "Download movie");
		setLocationRelativeTo(MainFrame.SINGLETON);
		setAlwaysOnTop(true);
		setLayout(new BorderLayout());
		setResizable(false);
		initGUI();
		pack();
	}
	
	private void initGUI(){
		progressBar = new JProgressBar();
	}
	
	
	
}
