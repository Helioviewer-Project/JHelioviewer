package org.helioviewer.jhv.plugins.samp;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.astrogrid.samp.Message;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.client.HubConnector;
import org.astrogrid.samp.client.SampException;
import org.astrogrid.samp.hub.Hub;
import org.astrogrid.samp.hub.HubServiceMode;
import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

import javafx.util.converter.LocalDateTimeStringConverter;

public class SampClient extends HubConnector
{
	private final static String MTYPE_VIEW_DATA = "jhv.vso.load"; 
	// TODO: Merge with other TimeFormatter (Metadata)
	private static final DateTimeFormatter SOHO_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd'T'HH:mm:ss.SSS");

	public SampClient(ClientProfile _profile)
	{
		super(_profile);
		Metadata meta = new Metadata();

		
		Hub[] runningHubs = Hub.getRunningHubs();
		if(runningHubs.length == 0)
		{
			try
			{
				Hub.checkExternalHubAvailability();
				Hub.runExternalHub(HubServiceMode.CLIENT_GUI);
			}
			catch (IOException _e1)
			{
				// TODO Auto-generated catch block
				_e1.printStackTrace();
			}
		}
		
		// TODO: name /  description
		meta.setName("JHelioviewer");
		meta.setDescriptionText("JHelioviewer");		
		declareMetadata(meta);
		declareSubscriptions(computeSubscriptions());
		
		setAutoconnect(10);
	}
	
	public void notifyRequestData() {
		// TODO: different layers may need different data to be sent
		if (Layers.anyImageLayers())
		{
			ImageLayer layer = Layers.getActiveImageLayer();
			MetaData metadata = layer.getCurrentMetaData();
			
			notifyRequestData(	
					MathUtils.toLDT(metadata.timeMS),
					metadata.instrument, 
					metadata.measurement);
		}
	}

	public void notifyRequestData(LocalDateTime timestamp, String instrument, String wave) {
		Message msg = new Message(MTYPE_VIEW_DATA);
		msg.addParam("timestamp", SOHO_DATE_TIME_FORMATTER.format(timestamp));
		msg.addParam("instrument", instrument);
		msg.addParam("wave", wave);
		
		try
		{
			// TODO: handle no HUBs available error!
			this.getConnection().notifyAll(msg);
		}
		catch (SampException _e)
		{
			// TODO Auto-generated catch block
			_e.printStackTrace();
		}
	}

}
