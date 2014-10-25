package org.helioviewer.jhv.gui.states;

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

    public void createViewChains();


    public ComponentView getMainComponentView();

    public ComponentView getOverviewComponentView();

    public ImagePanelInputController getDefaultInputController();

    public TopToolBar getTopToolBar();

    public void activate();

    public boolean isOverviewPanelInteractionEnabled();
}