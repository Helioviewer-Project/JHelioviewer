package org.helioviewer.jhv.plugins.samp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.astrogrid.samp.Message;
import org.astrogrid.samp.SampUtils;
import org.astrogrid.samp.client.AbstractMessageHandler;
import org.astrogrid.samp.client.HubConnection;
import org.helioviewer.jhv.base.Observatories;
import org.helioviewer.jhv.base.Observatories.Filter;
import org.helioviewer.jhv.base.Observatories.Observatory;
import org.helioviewer.jhv.base.coordinates.HeliocentricCartesianCoordinate;
import org.helioviewer.jhv.base.coordinates.HelioprojectiveCartesianCoordinate;
import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.jp2view.newjpx.KakaduLayer;

public class ShowLayerMessageHandler extends AbstractMessageHandler
{
	public final static String MTYPE_SHOW_LAYER = "jhv.layers.show";
	protected static final DateTimeFormatter LAYER_DATE_FORMATTER = DateTimeFormatter
			.ofPattern("yyyy-MM-dd");
	protected static final DateTimeFormatter LAYER_TIME_FORMATTER = DateTimeFormatter
			.ofPattern("HH:mm:ss");
	
	
	private final static String DEFAULT_OBSERVATORY = "SDO";
	private final static String DEFAULT_INSTRUMENT  = "AIA";
	private final static String DEFAULT_MEASUREMENT = "171";

	protected ShowLayerMessageHandler()
	{
		super(createSubscription());
		// TODO Auto-generated constructor stub
	}

	private static Map createSubscription()
	{
		Map<String, Map<String, String>> layersSubs = new HashMap<String, Map<String, String>>();
		Map<String, String> layersSubsConfig = new HashMap<String, String>();
		// allow web samp to also send add layers messages
		layersSubsConfig.put("x-samp.mostly-harmless", "1");
		layersSubs.put(MTYPE_SHOW_LAYER, layersSubsConfig);
		
		return layersSubs;
	}

	@Override
	public Map processCall(HubConnection c, String senderId, Message msg) throws Exception
	{
		Layers.removeAllImageLayers();
		
		DataContainer requestInfo = DataContainer.createFromMesssage(msg);
		
		ImageLayer newLayer = AddLayer(requestInfo);
		if (newLayer != null)
		{
			SetCameraPosition(requestInfo, newLayer);	
		}
		
		return null;
	}
	
	private void SetCameraPosition(DataContainer _requestInfo, ImageLayer _newLayer)
	{
		double xTheta = _requestInfo.xPos / (double)Constants.ARCSEC_FACTOR;
		double yTheta = _requestInfo.yPos / (double)Constants.ARCSEC_FACTOR;
		
		double xThetaRad = Math.toRadians(xTheta);
		double thetaX = Math.atan(xThetaRad);
		double thetaY = Math.atan(Math.toRadians(yTheta/Math.sqrt(1 + xThetaRad*xThetaRad)));
		
		HelioprojectiveCartesianCoordinate hpcc = new HelioprojectiveCartesianCoordinate(thetaX, thetaY);		
		HeliocentricCartesianCoordinate cart = hpcc.toHeliocentricCartesianCoordinate();
		
		// ToDo: investigate factor 216
		MainFrame.SINGLETON.MAIN_PANEL.setTranslationCurrent(new Vector3d(-216*cart.x, 216*cart.y, cart.z*2)  );
	}

	private ImageLayer AddLayer(DataContainer _requestInfo)
	{
		final String observatory = _requestInfo.observatory != null ? _requestInfo.observatory : DEFAULT_OBSERVATORY;
		final String instrument  = _requestInfo.instrument  != null ? _requestInfo.instrument  : DEFAULT_INSTRUMENT;
		final String measurement = _requestInfo.measurement != null ? _requestInfo.measurement : DEFAULT_MEASUREMENT; 
		
		for(Observatory o:Observatories.getObservatories())
			if(observatory.equals(o.toString()))
				for(Filter i:o.getInstruments())
					if(instrument.equals(i.toString()))
						for(Filter f:i.getFilters())
							if(measurement.equals(f.toString()))
							{
								ImageLayer layer = new KakaduLayer(f.sourceId,
										MathUtils.fromLDT(_requestInfo.start),
										MathUtils.fromLDT(_requestInfo.end),
										12*1000, f.getNickname());
								Layers.addLayer(layer);
								return layer;
							}
		
		return null;
	}

	private static class DataContainer
	{
		protected final LocalDateTime start;
		protected final LocalDateTime end;
		protected final LocalDateTime peak;
		
		protected final int xPos;
		protected final int yPos;
		
		protected final String observatory;
		protected final String instrument;
		protected final String detector;
		protected final String measurement;
		
		
		public DataContainer(LocalDateTime _start, LocalDateTime _end, LocalDateTime _peak, int _xPos, int _yPos,
				String _observatory, String _instrument, String _detector, String _measurement)
		{
			super();
			start = _start;
			end = _end;
			peak = _peak;
			xPos = _xPos;
			yPos = _yPos;
			observatory = _observatory;
			instrument = _instrument;
			detector = _detector;
			measurement = _measurement;
		}



		public static DataContainer createFromMesssage(Message _msg)
		{
			LocalDate date = LocalDate.from(LAYER_DATE_FORMATTER.parse((String)_msg.getRequiredParam("date")));
			LocalTime start = LocalTime.from(LAYER_TIME_FORMATTER.parse((String)_msg.getRequiredParam("start")));
			LocalTime peak = LocalTime.from(LAYER_TIME_FORMATTER.parse((String)_msg.getRequiredParam("end")));
			LocalTime end = LocalTime.from(LAYER_TIME_FORMATTER.parse((String)_msg.getRequiredParam("peak")));
			int xPos = SampUtils.decodeInt((String)_msg.getRequiredParam("xPos"));
			int yPos = SampUtils.decodeInt((String)_msg.getRequiredParam("yPos"));
			
			return new DataContainer(
					date.atTime(start),
					peak.isAfter(start) ? date.atTime(peak) : date.plusDays(1).atTime(peak),
					end.isAfter(start)  ? date.atTime(end)  : date.plusDays(1).atTime(end),
					xPos,
					yPos,
					(String)_msg.getParam("observatory"),
					(String)_msg.getParam("instrument"),
					(String)_msg.getParam("detector"),
					(String)_msg.getParam("measurement"));
		}



		@Override
		public String toString()
		{
			return "DataContainer [start=" + start + ", end=" + end + ", peak=" + peak + ", xPos=" + xPos + ", yPos="
					+ yPos + "]";
		}
		
	}
}
