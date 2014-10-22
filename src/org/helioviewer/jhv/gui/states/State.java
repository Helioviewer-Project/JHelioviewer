package org.helioviewer.jhv.gui.states;

import org.helioviewer.jhv.gui.GL3DViewchainFactory;
import org.helioviewer.jhv.gui.ViewchainFactory;
import org.helioviewer.jhv.gui.components.SideContentPane;
import org.helioviewer.jhv.gui.components.TopToolBar;
import org.helioviewer.jhv.gui.interfaces.ImagePanelInputController;
import org.helioviewer.viewmodel.view.ComponentView;

/**
 * State Interface
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public interface State {

    public void addStateSpecificComponents(SideContentPane sideContentPane);

    public void removeStateSpecificComponents(SideContentPane sideContentPane);

    public boolean createViewChains();

    /**
     * The ViewchainFactory is dependent on the state, as a different Viewchain
     * is required for 3D and 2D Modes.
     * 
     * @return viewchainFactory to use
     */
    public GL3DViewchainFactory getViewchainFactory();


    public ComponentView getMainComponentView();

    public ComponentView getOverviewComponentView();

    public ImagePanelInputController getDefaultInputController();

    public TopToolBar getTopToolBar();

    public void activate();

    public void deactivate();

    public boolean isOverviewPanelInteractionEnabled();
}