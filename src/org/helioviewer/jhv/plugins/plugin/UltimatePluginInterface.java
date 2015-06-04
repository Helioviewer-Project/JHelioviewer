package org.helioviewer.jhv.plugins.plugin;

import java.time.LocalDateTime;
import java.util.ArrayList;

import javax.swing.AbstractButton;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.components.newComponents.MainFrame;
import org.helioviewer.jhv.plugins.pfssplugin.PfssPlugin;
import org.helioviewer.jhv.plugins.sdocutoutplugin.SDOCutOutPlugin3D;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine.TimeLineListener;

import com.jogamp.opengl.GL2;

public class UltimatePluginInterface implements TimeLineListener{
	
	private ArrayList<NewPlugin> plugins;
	
	public static final UltimatePluginInterface SIGLETON = new UltimatePluginInterface();
	
	private UltimatePluginInterface() {
		plugins = new ArrayList<NewPlugin>();
		TimeLine.SINGLETON.addListener(this);
		plugins.add(new SDOCutOutPlugin3D());
		plugins.add(new PfssPlugin());
	}
	
	public void addPlugin(NewPlugin plugin){
		plugins.add(plugin);
	}
	
	public void removePlugin(NewPlugin plugin){
		plugins.remove(plugin);
	}
	
	public static void addButtonToToolbar(AbstractButton button){
		MainFrame.TOP_TOOL_BAR.addButton(button);
	}
	
	public static void addPanelToLeftControllPanel(String title, JPanel panel, boolean startExpanded){
		MainFrame.LEFT_PANE.add(title, panel, startExpanded);
	}

	@Override
	public void timeStampChanged(LocalDateTime current, LocalDateTime last) {
		for (NewPlugin plugin : plugins){
			plugin.timeStampChanged(current, last);
		}
	}

	@Override
	public void dateTimesChanged(int framecount) {
		for (NewPlugin plugin : plugins){
			plugin.dateTimesChanged(framecount);
		}		
	}
	
	public void renderPlugin(GL2 gl){
		for (NewPlugin plugin : plugins){
			plugin.render(gl);
		}
	}
	
	public String[] getAboutLicenseTexts(){
		String[] licenseTexts = new String[plugins.size()];
		int index = 0;
		for (NewPlugin plugin : plugins){
			String licenseText = plugin.getAboutLicenseText();
			licenseTexts[index++] = licenseText != null ? licenseText : "";
		}
		return licenseTexts;
	}

	public LocalDateTime getCurrentDateTime() {
		return TimeLine.SINGLETON.getCurrentDateTime();
	}
	
}
