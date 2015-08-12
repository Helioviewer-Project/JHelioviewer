package org.helioviewer.jhv.plugins.newhek;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.helioviewer.jhv.opengl.events.RenderableEvent;
import org.json.JSONException;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class HEKEvent extends RenderableEvent{

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private static final String JSON_ID = "kb_archivid";
    private static final String JSON_START_DATE = "event_starttime";
    private static final String JSON_END_DATE = "event_endtime";
    
	private HEKEvent(String id, LocalDateTime startDateTime,
			LocalDateTime endDateTime) {
		super(id, startDateTime, endDateTime);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void render(GL2 gl) {
		// TODO Auto-generated method stub
		
	}
	
	public static HEKEvent createHEKEvent(JSONObject jsonObject) throws JSONException{
		
        String id = jsonObject.getString(JSON_ID);

        LocalDateTime startDateTime = LocalDateTime.parse(jsonObject.getString(JSON_START_DATE), DATE_TIME_FORMATTER);
        LocalDateTime endDateTime = LocalDateTime.parse(jsonObject.getString(JSON_END_DATE), DATE_TIME_FORMATTER);
		
		return new HEKEvent(id, startDateTime, endDateTime);
	}
	
}
