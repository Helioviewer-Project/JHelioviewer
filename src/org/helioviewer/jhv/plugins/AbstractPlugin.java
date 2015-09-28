package org.helioviewer.jhv.plugins;

import java.awt.event.MouseEvent;
import java.time.LocalDateTime;

import org.helioviewer.jhv.base.math.Vector3d;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public abstract class AbstractPlugin
{
	protected final String pluginName;
	protected final boolean LOAD_ON_STARTUP = true;
	
	public enum RenderMode {
		MAIN_PANEL, OVERVIEW_PANEL, ALL_PANEL;
	};
	protected RenderMode renderMode = RenderMode.ALL_PANEL;
	
	public AbstractPlugin(String name) {
		pluginName = name;
	}
	
	public void timeStampChanged(LocalDateTime current, LocalDateTime last){
		
	}
	
	
	public void dateTimesChanged(int framecount) {
		
		
	}
	
	public void render(GL2 gl){
		
	}
	
	public void mouseDragged(MouseEvent e, Vector3d point) {
	}

	public void mouseMoved(MouseEvent e, Vector3d point) {
	}

	public void mouseClicked(MouseEvent e, Vector3d point) {
	}

	public void mousePressed(MouseEvent e, Vector3d point) {
	}

	public void mouseReleased(MouseEvent e, Vector3d point) {
	}

	public void mouseEntered(MouseEvent e, Vector3d point) {
	}

	public void mouseExited(MouseEvent e, Vector3d point) {
	}
	
	public RenderMode getRenderMode(){
		return this.renderMode;
	}
	
	abstract public void restoreConfiguration(JSONObject jsonObject);
	abstract public void storeConfiguration(JSONObject jsonObject);

	public abstract void setVisible(boolean visible);
	public abstract boolean isVisible();
	
	public String getName()
	{
		return pluginName;
	}

	abstract public void load();
	abstract public void remove();

	@Override
	public String toString()
	{
		return pluginName;
	}
	
	abstract public boolean retryNeeded();
	abstract public void retry();
}
