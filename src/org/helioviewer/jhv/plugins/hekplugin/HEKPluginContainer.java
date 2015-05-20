package org.helioviewer.jhv.plugins.hekplugin;

import java.util.Date;

import org.helioviewer.jhv.base.math.Interval;
import org.helioviewer.jhv.plugins.viewmodelplugin.overlay.OverlayContainer;

/**
 * Overlay plug-in to display different kinds of solar events retrieved from the
 * HEK API.
 * <p>
 * This plug-in provides a Treeview, categorizing the different Events available
 * on different tree levels:
 * <li>Catalogue (Default=HEK)</li>
 * <li>Event Type (Coronal Holes, ...)</li>
 * <li>Feature Recognition Method</li>
 * <li>Event</li>
 * 
 * It is necessary to initially request the Tree structure for each Interval
 * currently looked at. Once the structure is loaded, the user can select
 * Different categories and download all the events "inside" these categories.
 * 
 * The basic architecture is heavily inspired by the event catalogue developed
 * by Stephan Pagel.
 * 
 * @author Malte Nuhn
 */
public class HEKPluginContainer extends OverlayContainer {

    // TODO: Malte Nuhn - Does storing the panel connected with this plugin fit
    // the architecture?

    private HEKPluginPanel hekPanel;

    public HEKPluginContainer() {
    }

    /**
     * {@inheritDoc}
     */
    /*@Override
    public Class<? extends PhysicalRenderer3d> getOverlayClass() {
        return HEKPlugin3dRenderer.class;
    }*/

    /**
     * {@inheritDoc}
     */
    /*@Override
    protected void installOverlayImpl(OverlayView overlayView, OverlayControlComponentManager controlList) {
        
    	hekPanel = new HEKPluginPanel(HEKCache.getSingletonInstance());
        OverlayPluginContainer overlayPluginContainer = new OverlayPluginContainer();
        overlayPluginContainer.setRenderer3d(new HEKPlugin3dRenderer());
        
        overlayView.addOverlay(overlayPluginContainer);
        controlList.add(new OverlayControlComponent(hekPanel, getName()));
        hekPanel.reload();
    }*/

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return "HEK Events";
    }

    /**
     * @see HEKPanel#setCurInterval()
     */
    public void setCurInterval(Interval<Date> newInterval) {
        hekPanel.setCurInterval(newInterval);
    }

    public void setEnabled(boolean b) {
        hekPanel.setEnabled(b);
    }

}
