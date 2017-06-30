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

public class ShowLayerMessageHandler extends AbstractMessageHandler
{
	public final static String MTYPE_SHOW_LAYER = "jhv.layers.show";
	protected static final DateTimeFormatter LAYER_DATE_FORMATTER = DateTimeFormatter
			.ofPattern("yyyy-MM-dd'T'");
	protected static final DateTimeFormatter LAYER_TIME_FORMATTER = DateTimeFormatter
			.ofPattern("HH:mm:ss");

	protected ShowLayerMessageHandler()
	{
		super(createSubscription());
		// TODO Auto-generated constructor stub
	}

	private static Map createSubscription()
	{
		Map<String, Map<String, String>> flareSubs = new HashMap<String, Map<String, String>>();
		Map<String, String> flareSubsConfig = new HashMap<String, String>();
		flareSubsConfig.put("x-samp.mostly-harmless", "1");
		flareSubs.put(MTYPE_SHOW_LAYER, flareSubsConfig);
		
		return flareSubs;
	}

	@Override
	public Map processCall(HubConnection c, String senderId, Message msg) throws Exception
	{
		DataContainer flareInfo = DataContainer.createFromMesssage(msg);
		
		System.out.println(c);
		System.out.println(senderId);
		System.out.println(flareInfo);
		return null;
	}
	
	private static class DataContainer
	{
		protected final LocalDateTime start;
		protected final LocalDateTime end;
		protected final LocalDateTime peak;
		
		protected final int xPos;
		protected final int yPos;

		private DataContainer(LocalDateTime _start, LocalDateTime _end, LocalDateTime _peak, int _xPos, int _yPos)
		{
			super();
			start = _start;
			end = _end;
			peak = _peak;
			xPos = _xPos;
			yPos = _yPos;
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
					yPos
					);
		}



		@Override
		public String toString()
		{
			return "DataContainer [start=" + start + ", end=" + end + ", peak=" + peak + ", xPos=" + xPos + ", yPos="
					+ yPos + "]";
		}
		
	}
}
