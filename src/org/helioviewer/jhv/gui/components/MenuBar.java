package org.helioviewer.jhv.gui.components;

import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

import org.helioviewer.jhv.base.Globals;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.MoviePanel;
import org.helioviewer.jhv.gui.actions.ExitProgramAction;
import org.helioviewer.jhv.gui.actions.ExportMovieAction;
import org.helioviewer.jhv.gui.actions.LoadStateAction;
import org.helioviewer.jhv.gui.actions.OpenLocalFileAction;
import org.helioviewer.jhv.gui.actions.OpenURLinBrowserAction;
import org.helioviewer.jhv.gui.actions.SaveScreenshotAsAction;
import org.helioviewer.jhv.gui.actions.SaveStateAction;
import org.helioviewer.jhv.gui.actions.ShowDialogAction;
import org.helioviewer.jhv.gui.actions.ToggleFullscreenAction;
import org.helioviewer.jhv.gui.actions.Zoom1To1Action;
import org.helioviewer.jhv.gui.actions.ZoomFitAction;
import org.helioviewer.jhv.gui.actions.ZoomInAction;
import org.helioviewer.jhv.gui.actions.ZoomOutAction;
import org.helioviewer.jhv.gui.dialogs.AboutDialog;
import org.helioviewer.jhv.gui.dialogs.PreferencesDialog;
import org.helioviewer.jhv.gui.dialogs.ShortcutsDialog;

/**
 * Menu bar of the main window.
 * 
 * <p>
 * Basically, contains all actions from {@link org.helioviewer.jhv.gui.actions}.
 */
public class MenuBar extends JMenuBar
{
	public MenuBar()
	{
		super();

		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		fileMenu.add(new OpenLocalFileAction());
		/*
		 * fileMenu.add(new ShowDialogAction("Open remote image...",
		 * OpenRemoteFileDialog.class));
		 */
		fileMenu.addSeparator();

		JMenu exampleMenu = new JMenu("Load examples");
		exampleMenu.add(new LoadStateAction("SDO/AIA Prominence Eruption (2010/12/06)",
						MenuBar.class.getResource("/examples/SDO_AIA_Prominence_Eruption_2010_12_06.jhv")));
		exampleMenu.add(new LoadStateAction("SDO/AIA Flare (2010/11/11)",
						MenuBar.class.getResource("/examples/SDO_AIA_Flare_Nov2010.jhv")));
		exampleMenu.add(new LoadStateAction("SOHO Halloween Storms (2003)",
						MenuBar.class.getResource("/examples/SOHO_Halloween_Storms_2003.jhv")));
		exampleMenu.add(new LoadStateAction("SOHO Comet Neat (February 2003)",
						MenuBar.class.getResource("/examples/SOHO_Comet_Neat_Feb2003.jhv")));
		exampleMenu.add(new LoadStateAction("SOHO Bastille Day flare (July 2000)",
						MenuBar.class.getResource("/examples/SOHO_Bastille_Day_Flare_July2000.jhv")));
		exampleMenu.add(new LoadStateAction("SOHO Lightbulb CME (February 2000)",
						MenuBar.class.getResource("/examples/SOHO_Lightbulb_CME_Feb2000.jhv")));

		// exampleMenu.add(new LoadStateAction("SOHO CMEs (May 1998)",
		// FileUtils.getResourceUrl("/examples/SOHO_CMEs_May1998.jhv")));

		fileMenu.add(exampleMenu);
		fileMenu.add(new LoadStateAction());
		fileMenu.add(new SaveStateAction());
		fileMenu.addSeparator();
		fileMenu.add(new SaveScreenshotAsAction());

		fileMenu.add(new ExportMovieAction());

		if (!Globals.isOSX())
		{
			fileMenu.addSeparator();
			fileMenu.add(new ExitProgramAction());
		}
		
		add(fileMenu);

		JMenu viewMenu = new JMenu("View");
		viewMenu.setMnemonic(KeyEvent.VK_V);
		// viewMenu.add(new GL3DCenterImageAction());
		viewMenu.add(new ToggleFullscreenAction());
		viewMenu.addSeparator();
		viewMenu.add(new ZoomInAction(true));
		viewMenu.add(new ZoomOutAction(true));
		viewMenu.add(new ZoomFitAction(true));
		viewMenu.add(new Zoom1To1Action(true));
		add(viewMenu);

		JMenu movieMenu = new JMenu("Movie");
		movieMenu.setMnemonic(KeyEvent.VK_A);

		movieMenu.add(new MoviePanel.StaticPlayPauseAction());
		movieMenu.add(new MoviePanel.StaticPreviousFrameAction());
		movieMenu.add(new MoviePanel.StaticNextFrameAction());

		add(movieMenu);

		if(!Globals.isOSX())
		{
			JMenu optionsMenu = new JMenu("Options");
			optionsMenu.setMnemonic(KeyEvent.VK_O);
			optionsMenu.add(new ShowDialogAction("Preferences...", IconBank.getIcon(JHVIcon.SETTINGS, 16, 16), PreferencesDialog.class));
			add(optionsMenu);
		}

		JMenu helpMenu = new JMenu("Help");
		helpMenu.setMnemonic(KeyEvent.VK_H);
		helpMenu.add(new OpenURLinBrowserAction("Open manual", "http://www.jhelioviewer.org/doc.html"));
		helpMenu.add(new ShowDialogAction("List of shortcuts...", ShortcutsDialog.class));
		helpMenu.addSeparator();
		helpMenu.add(new OpenURLinBrowserAction("Report a bug", "https://github.com/Helioviewer-Project/JHelioViewer/issues"));
		helpMenu.add(new OpenURLinBrowserAction("Submit a feature request", "https://github.com/Helioviewer-Project/JHelioViewer/issues"));
		helpMenu.addSeparator();
		helpMenu.add(new ShowDialogAction("About JHelioviewer...", AboutDialog.class));
		add(helpMenu);
	}
}
