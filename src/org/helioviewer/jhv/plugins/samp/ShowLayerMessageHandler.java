package org.helioviewer.jhv.plugins.samp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

import org.astrogrid.samp.DataException;
import org.astrogrid.samp.ErrInfo;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.Response;
import org.astrogrid.samp.SampMap;
import org.astrogrid.samp.SampUtils;
import org.astrogrid.samp.client.AbstractMessageHandler;
import org.astrogrid.samp.client.HubConnection;
import org.helioviewer.jhv.base.Observatories;
import org.helioviewer.jhv.base.Observatories.Filter;
import org.helioviewer.jhv.base.Observatories.Observatory;
import org.helioviewer.jhv.base.coordinates.HeliocentricCartesianCoordinate;
import org.helioviewer.jhv.base.coordinates.HelioprojectiveCartesianCoordinate;
import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.base.math.Quaternion;
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
	
	private final static LocalDate SDO_FIRST_IMAGES = LocalDate.of(2010, 06, 02);
	
	private final static String DEFAULT_OBSERVATORY_PRE_SDO = "SOHO";
	private final static String DEFAULT_INSTRUMENT_PRE_SDO  = "EIT";
	private final static String DEFAULT_MEASUREMENT_PRE_SDO = "195";

	protected ShowLayerMessageHandler()
	{
		super(createSubscription());
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
		ErrInfo errorInfo = new ErrInfo();
		DataContainer requestInfo = DataContainer.createFromMesssage(msg, errorInfo);			
		
		if(!errorInfo.isEmpty())
		{
			return Response.createErrorResponse(errorInfo);
		}
		
		Layers.removeAllImageLayers();
		ImageLayer newLayer = AddLayer(requestInfo);
		if (newLayer != null && requestInfo.hasPos)
		{
			SetCameraPosition(requestInfo, newLayer);	
		}
		
		return Response.createSuccessResponse(SampMap.EMPTY);
	}
	
	
	
	@Override
	protected Response createResponse(Map _processOutput)
	{
		if (_processOutput instanceof Response)
		{
			return (Response) _processOutput;
		}
		else
		{
			return super.createResponse(_processOutput);
		}
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
		
		// TODO: investigate factor 216
		Vector3d translation = new Vector3d(-216*cart.x, 216*cart.y, cart.z*2);
		
		MainFrame.SINGLETON.MAIN_PANEL.abortAllAnimations();
		
		MainFrame.SINGLETON.MAIN_PANEL.setRotationEnd(Quaternion.IDENTITY);
		MainFrame.SINGLETON.MAIN_PANEL.setRotationCurrent(Quaternion.IDENTITY);
		MainFrame.SINGLETON.MAIN_PANEL.setTranslationEnd(translation);
		MainFrame.SINGLETON.MAIN_PANEL.setTranslationCurrent(translation);
	}

	private ImageLayer AddLayer(DataContainer _requestInfo)
	{
		final String observatory;
		final String instrument;
		final String measurement;
		if (SDO_FIRST_IMAGES.atStartOfDay().isAfter(_requestInfo.start))
		{
			observatory = _requestInfo.observatory != null ? _requestInfo.observatory : DEFAULT_OBSERVATORY_PRE_SDO;
			instrument  = _requestInfo.instrument  != null ? _requestInfo.instrument  : DEFAULT_INSTRUMENT_PRE_SDO;
			measurement = _requestInfo.measurement != null ? _requestInfo.measurement : DEFAULT_MEASUREMENT_PRE_SDO; 
		}
		else
		{
			observatory = _requestInfo.observatory != null ? _requestInfo.observatory : DEFAULT_OBSERVATORY;
			instrument  = _requestInfo.instrument  != null ? _requestInfo.instrument  : DEFAULT_INSTRUMENT;
			measurement = _requestInfo.measurement != null ? _requestInfo.measurement : DEFAULT_MEASUREMENT; 
		}
		
		
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
		
		protected final boolean hasPos;
		
		protected final String observatory;
		protected final String instrument;
		protected final String detector;
		protected final String measurement;
		
		
		public DataContainer(
				LocalDateTime _start, 
				LocalDateTime _end, 
				LocalDateTime _peak, 
				int _xPos, 
				int _yPos,
				
				String _observatory, 
				String _instrument, 
				String _detector, 
				String _measurement)
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
			hasPos = true;
		}
		
		public DataContainer(
				LocalDateTime _start, 
				LocalDateTime _end, 
				LocalDateTime _peak, 				
				String _observatory, 
				String _instrument, 
				String _detector, 
				String _measurement)
		{
			super();
			start = _start;
			end = _end;
			peak = _peak;
			observatory = _observatory;
			instrument = _instrument;
			detector = _detector;
			measurement = _measurement;
			
			xPos = 0;
			yPos = 0;
			hasPos = false;
		}

		private static LocalTime tryParseTime(String time, String fieldName, ErrInfo _errorInfo)
		{
			try
			{
				return LocalTime.from(LAYER_TIME_FORMATTER.parse(time));
			}
			catch (DateTimeParseException err)
			{
				_errorInfo.setErrortxt("The field '"+fieldName+"' is in the wrong format.");
				_errorInfo.setUsertxt(err.getMessage());
				return null;
			}
		}

		private static LocalDate tryParseDate(String date, String fieldName, ErrInfo _errorInfo)
		{
			try
			{
				return LocalDate.from(LAYER_DATE_FORMATTER.parse(date));
			}
			catch (DateTimeParseException err)
			{
				_errorInfo.setErrortxt("The field '"+fieldName+"' is in the wrong format.");
				_errorInfo.setUsertxt(err.getMessage());
				return null;
			}
		}

		public static DataContainer createFromMesssage(Message _msg, ErrInfo _errorInfo)
		{
			String dateStr;
			String startStr;
			String endStr;
			
			try
			{
				dateStr = (String)_msg.getRequiredParam("date");
				startStr = (String)_msg.getRequiredParam("start");
				endStr = (String)_msg.getRequiredParam("end");
			}
			catch (DataException err)
			{
				_errorInfo.setErrortxt(err.getMessage());
				return null;
			}
			
			LocalDate date = tryParseDate(dateStr, "date", _errorInfo);
			LocalTime start = tryParseTime(startStr, "start", _errorInfo);
			LocalTime end = tryParseTime(endStr, "end", _errorInfo);
			
			String peakStr = (String)_msg.getParam("peak");
			LocalDateTime peakDateTime = null;
			if (peakStr != null)
			{
				LocalTime peak = tryParseTime(peakStr, "peak", _errorInfo);
				if (_errorInfo.isEmpty())
				{
					peakDateTime = peak.isAfter(start) 
							? date.atTime(peak) 
									: date.plusDays(1).atTime(peak);
				}
			}
			
			String xPosString = (String)_msg.getParam("xPos");
			String yPosString = (String)_msg.getParam("yPos");
			
			// abort if there were any errors
			if (!_errorInfo.isEmpty())
			{
				return null;
			}
			else if (xPosString != null && yPosString != null)
			{
				int xPos = SampUtils.decodeInt(xPosString);
				int yPos = SampUtils.decodeInt(yPosString);

				return new DataContainer(
						date.atTime(start),
						end.isAfter(start)  ? date.atTime(end)  : date.plusDays(1).atTime(end),
						peakDateTime,
						xPos,
						yPos,
						(String)_msg.getParam("observatory"),
						(String)_msg.getParam("instrument"),
						(String)_msg.getParam("detector"),
						(String)_msg.getParam("measurement"));
			}
			else
			{
				return new DataContainer(
						date.atTime(start),
						end.isAfter(start)  ? date.atTime(end)  : date.plusDays(1).atTime(end),
						peakDateTime,
						(String)_msg.getParam("observatory"),
						(String)_msg.getParam("instrument"),
						(String)_msg.getParam("detector"),
						(String)_msg.getParam("measurement"));
			}
		}



		@Override
		public String toString()
		{
			return "DataContainer [start=" + start + ", end=" + end + ", peak=" + peak + ", xPos=" + xPos + ", yPos="
					+ yPos + "]";
		}
		
	}
}
