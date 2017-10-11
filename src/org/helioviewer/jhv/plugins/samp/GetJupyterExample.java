package org.helioviewer.jhv.plugins.samp;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;

import javax.swing.AbstractAction;
import javax.swing.Icon;

import org.helioviewer.jhv.base.Globals;
import org.helioviewer.jhv.base.Settings;
import org.helioviewer.jhv.base.Globals.DialogType;
import org.helioviewer.jhv.base.Settings.StringKey;
import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.gui.PredefinedFileFilter;

public class GetJupyterExample extends AbstractAction
{
	private static final PredefinedFileFilter JUPYTER_NOTEBOOK = new PredefinedFileFilter("Jupyter Notebook (*.jpynb)", new String[]{"*.ipynb"});
	private final String notebookName;
	public GetJupyterExample(String _notebookName, String _name)
	{
		super(_name);
		notebookName = _notebookName;
	}

	@Override
	public void actionPerformed(ActionEvent _e)
	{
		File selectedFile  = Globals.showFileDialog(
				DialogType.SAVE_FILE, 
				"Save SAMP example for Jupyter notebook", 
				Settings.getString(StringKey.STATE_DIRECTORY), 
				false, 
				notebookName, 
				JUPYTER_NOTEBOOK);
		
		if (selectedFile != null)
		{
			try
			{
				Files.copy(GetJupyterExample.class.getResourceAsStream("/SAMP/" + notebookName), selectedFile.toPath());
			}
			catch (IOException _err)
			{
				Telemetry.trackException(_err);
			}
		}
	}

}
