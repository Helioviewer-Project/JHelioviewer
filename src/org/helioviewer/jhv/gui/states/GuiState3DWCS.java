package org.helioviewer.jhv.gui.states;

import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.gui.GL3DCameraMouseController;
import org.helioviewer.gl3d.gui.GL3DCameraSelectorModel;
import org.helioviewer.gl3d.gui.GL3DTopToolBar;
import org.helioviewer.gl3d.view.GL3DSceneGraphView;
import org.helioviewer.jhv.gui.GL3DViewchainFactory;
import org.helioviewer.jhv.gui.ViewListenerDistributor;
import org.helioviewer.jhv.gui.ViewchainFactory;
import org.helioviewer.jhv.gui.components.SideContentPane;
import org.helioviewer.jhv.gui.components.TopToolBar;
import org.helioviewer.jhv.gui.components.statusplugins.RenderModeStatusPanel;
import org.helioviewer.jhv.gui.interfaces.ImagePanelInputController;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.SynchronizeView;

public class GuiState3DWCS implements State {

    // private JPanel gl3dSettingsPanel;
    protected TopToolBar topToolBar;

    protected ComponentView mainComponentView;
    protected ComponentView overviewComponentView;

    protected RenderModeStatusPanel renderModeStatus;

    private GL3DViewchainFactory viewchainFactory;

    
    public GuiState3DWCS() {
        // Override the viewchainFactory with a specific 3D implementation
        this(new GL3DViewchainFactory());
    }

    public GuiState3DWCS(GL3DViewchainFactory viewchainFactory) {
        this.viewchainFactory = viewchainFactory;
    }
    

    public void addStateSpecificComponents(SideContentPane sideContentPane) {
        GL3DCameraSelectorModel.getInstance();
    }

    public void removeStateSpecificComponents(SideContentPane sideContentPane) {
        //this.modelPanel.destroy();
    }


    public void activate() {
        GL3DCameraSelectorModel.getInstance().activate(this.mainComponentView.getAdapter(GL3DSceneGraphView.class));
    }

    public void deactivate() {
        getMainComponentView().deactivate();
    }

    public boolean createViewChains() {
        Log.info("Start creating view chains");

        boolean firstTime = (mainComponentView == null);

        // Create main view chain
        GL3DViewchainFactory mainFactory = this.viewchainFactory;
        mainComponentView = mainFactory.createViewchainMain(mainComponentView, false);


        ViewListenerDistributor.getSingletonInstance().setView(mainComponentView);
        // imageSelectorPanel.setLayeredView(mainComponentView.getAdapter(LayeredView.class));

        return firstTime;
    }


    public ViewStateEnum getType() {
        return ViewStateEnum.View3D;
    }

    public TopToolBar getTopToolBar() {
        if (topToolBar == null) {
            topToolBar = new GL3DTopToolBar();
        }
        return topToolBar;
    }

    public ComponentView getMainComponentView() {
        return mainComponentView;
    }

    public ComponentView getOverviewComponentView() {
        return overviewComponentView;
    }

    public RenderModeStatusPanel getRenderModeStatus() {
        return renderModeStatus;
    }

    public GL3DViewchainFactory getViewchainFactory() {
        return this.viewchainFactory;
    }

    public ImagePanelInputController getDefaultInputController() {
        return new GL3DCameraMouseController();
    }

    public boolean isOverviewPanelInteractionEnabled() {
        return false;
    }
}
