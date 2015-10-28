package org.helioviewer.jhv.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.layers.Layers;

public class ExitProgramAction extends AbstractAction
{
	private static final ArrayList<Runnable> onShutdown=new ArrayList<>();
	
	public ExitProgramAction()
	{
		super("Quit");
		putValue(SHORT_DESCRIPTION, "Quit program");
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit
				.getDefaultToolkit().getMenuShortcutKeyMask()));
	}
	
	public static synchronized void addShutdownHook(Runnable _r)
	{
		onShutdown.add(_r);
	}

	public void actionPerformed(@Nullable ActionEvent e)
	{
		if (Layers.anyImageLayers())
		{
			int option = JOptionPane.showConfirmDialog(
					MainFrame.SINGLETON,
					"Are you sure you want to quit?", "Confirm",
					JOptionPane.OK_CANCEL_OPTION);
			
			if (option == JOptionPane.CANCEL_OPTION)
				return;
		}
		
		System.out.println("Running shutdown hooks");
		for(Runnable r:onShutdown)
			r.run();
		
		System.out.println("Quitting application");
		System.exit(0);
	}
}
