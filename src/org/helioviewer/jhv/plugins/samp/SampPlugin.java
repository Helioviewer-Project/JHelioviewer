package org.helioviewer.jhv.plugins.samp;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.time.LocalDateTime;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JMenu;

import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.client.DefaultClientProfile;
import org.astrogrid.samp.client.HubConnector;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.layers.PluginLayer;
import org.helioviewer.jhv.plugins.Plugin;
import org.helioviewer.jhv.plugins.Plugins;
import org.json.JSONException;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class SampPlugin extends Plugin
{
	private SampClient sampHub;

	public SampPlugin()
	{
		super("SAMP", "SAMP", RenderMode.OVERVIEW_PANEL);
		
		ClientProfile profile = DefaultClientProfile.getProfile();	
		sampHub = new SampClient(profile);
		
		JButton notifySamp = new JButton(new NotifySamp(false, false)
		{
			@Override
			public void actionPerformed(ActionEvent _e)
			{
				sampHub.notifyRequestData();
			}
		});
		
		JMenu sampMenu = new JMenu("Samp");
		sampMenu.setMnemonic('S');
		sampMenu.add(new NotifySamp(true, true)
		{
			@Override
			public void actionPerformed(ActionEvent _e)
			{
				sampHub.notifyRequestData();
			}
		});
		sampMenu.add(new GetJupyterExample());
		
		Plugins.addButtonToToolbar(notifySamp);
		Plugins.addMenuEntry(sampMenu);
	}



	@Override
	public void restoreConfiguration(JSONObject _jsonObject) throws JSONException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void storeConfiguration(JSONObject _jsonObject) throws JSONException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public boolean retryNeeded()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void retry()
	{
		// TODO Auto-generated method stub

	}

}
