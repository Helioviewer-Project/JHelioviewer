package org.helioviewer.jhv.plugins.samp;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.astrogrid.samp.DataException;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.SampUtils;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.client.HubConnector;
import org.astrogrid.samp.client.SampException;
import org.astrogrid.samp.hub.Hub;
import org.astrogrid.samp.hub.HubServiceMode;
import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.TimeLine.DecodeQualityLevel;
import org.helioviewer.jhv.viewmodel.jp2view.newjpx.KakaduLayer;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

public class SampClient extends HubConnector
{
	private final static String MTYPE_VIEW_DATA = "jhv.vso.load";
	// TODO: Merge with other TimeFormatter (Metadata)
	protected static final DateTimeFormatter SAMP_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	public SampClient(ClientProfile _profile)
	{
		super(_profile);
		Metadata meta = new Metadata();

		Hub[] runningHubs = Hub.getRunningHubs();
		if (runningHubs.length == 0)
		{
			try
			{
				Hub.checkExternalHubAvailability();
				Hub.runExternalHub(HubServiceMode.MESSAGE_GUI);
			}
			catch (IOException _e1)
			{
				Telemetry.trackException(_e1);
			}
		}

		// TODO: name / description
		meta.setName("JHelioviewer");
		meta.setDescriptionText("JHelioviewer");
		
		declareMetadata(meta);
		//addMessageHandler(new FlareMessageHandler());
		declareSubscriptions(computeSubscriptions());

		setAutoconnect(10);
	}



	public void notifyRequestData()
	{
		if (Layers.anyImageLayers())
		{
			ImageLayer activeLayer = Layers.getActiveImageLayer();
			List<KakaduLayer> layers = new ArrayList<>();

			for (Layer layer : Layers.getLayers())
			{
				if (layer.isVisible() && layer instanceof KakaduLayer)
				{
					layers.add((KakaduLayer) layer);
				}
			}

			if (layers.size() == 0)
			{
				return;
			}

			if (!activeLayer.isVisible())
			{
				activeLayer = layers.get(0);
			}

			Message msg = createMessage(activeLayer, layers);

			notifyRequestData(msg);
		}
	}

	private Message createMessage(ImageLayer _activeLayer, List<KakaduLayer> layers)
	{
		LocalDateTime timestamp = MathUtils.toLDT(_activeLayer.getCurrentTimeMS());
		LocalDateTime start = MathUtils.toLDT(_activeLayer.getStartTimeMS());
		LocalDateTime end = MathUtils.toLDT(_activeLayer.getEndTimeMS());
		Rectangle2D cutout = getCutoutInfo(_activeLayer);

		Message msg = new Message(MTYPE_VIEW_DATA);
		msg.addParam("timestamp", SAMP_DATE_TIME_FORMATTER.format(timestamp));
		msg.addParam("start", SAMP_DATE_TIME_FORMATTER.format(start));
		msg.addParam("end", SAMP_DATE_TIME_FORMATTER.format(end));
		msg.addParam("cadence", SampUtils.encodeLong(_activeLayer.getCadenceMS()));
		msg.addParam("cutout.set", SampUtils.encodeBoolean(cutout != null));
		if(cutout != null) {
			msg.addParam("cutout.x0", SampUtils.encodeFloat(cutout.getX()));
			msg.addParam("cutout.y0", SampUtils.encodeFloat(cutout.getY()));
			msg.addParam("cutout.w", SampUtils.encodeFloat(cutout.getWidth()));
			msg.addParam("cutout.h", SampUtils.encodeFloat(cutout.getHeight()));
		} 

		List<Map<String, String>> layersData = new ArrayList<>();

		for (KakaduLayer layer : layers)
		{
			MetaData data = layer.getCurrentMetaData();
			if (data != null)
			{
				LocalDateTime layerTimeStamp = MathUtils.toLDT(layer.getCurrentTimeMS());
				Map<String, String> layerMsg = new HashMap<String, String>();
				layerMsg.put("observatory", encodeString(data.observatory));
				layerMsg.put("instrument", encodeString(getInstrumentNameOnly(data.instrument)));
				layerMsg.put("detector", encodeString(data.detector));
				layerMsg.put("measurement", encodeString(data.measurement));
				layerMsg.put("timestamp", SAMP_DATE_TIME_FORMATTER.format(layerTimeStamp));
				layersData.add(layerMsg);
			}
			else
			{
				Telemetry.trackEvent("SAMP: Layer has no metadata");
			}
		}

		msg.addParam("layers", layersData);

		return msg;
	}

	private Rectangle2D getCutoutInfo(ImageLayer _activeLayer)
	{
		// TODO: this is the same code as in SDOCutOutAction.java
		// TODO: merge both instance
		MetaData metaData = _activeLayer.getCurrentMetaData();
		ImageRegion ir = _activeLayer.calculateRegion(MainFrame.SINGLETON.MAIN_PANEL, DecodeQualityLevel.QUALITY,
				metaData, MainFrame.SINGLETON.MAIN_PANEL.getCanavasSize());
		if (ir == null)
		{
			return null;
		}

		Rectangle2D sourceRegion = ir.areaOfSourceImage;
		Vector2i resolution = metaData.resolution;
		Vector2d arcsecFactor = metaData.arcsecPerPixel;
		Vector2d sunPosArcSec = metaData.sunPixelPosition.scaled(arcsecFactor);
		Vector2d sizeArcSec = new Vector2d(sourceRegion.getWidth() * resolution.x,
				sourceRegion.getHeight() * resolution.y).scaled(arcsecFactor);
		Vector2d offsetArcSec = new Vector2d(sourceRegion.getCenterX() * resolution.x,
				sourceRegion.getCenterY() * resolution.y).scaled(arcsecFactor);

		return new Rectangle2D.Double((offsetArcSec.x - sunPosArcSec.x), (-offsetArcSec.y + sunPosArcSec.y),
				sizeArcSec.x, sizeArcSec.y);
	}

	private String getInstrumentNameOnly(String instrument)
	{
		if (instrument == null)
		{
			return null;
		}

		return instrument.split("_")[0];
	}

	private String encodeString(String value)
	{
		if (value != null)
		{
			SampUtils.checkString(value);
			return value;
		}
		else
		{
			return "";
		}
	}

	public void notifyRequestData(Message msg)
	{
		try
		{
			// TODO: handle no HUBs available error!
			this.getConnection().notifyAll(msg);
		}
		catch (SampException _e)
		{
			Telemetry.trackException(_e);
		}
		catch (DataException _e)
		{
			Telemetry.trackException(_e);
		}
	}

}
