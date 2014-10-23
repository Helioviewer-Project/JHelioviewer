package org.helioviewer.jhv.gui.states;

/**
 * Contains the existing Gui States (2D, 3D)
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public enum ViewStateEnum {

    //View2D(new GuiState2D()),
    View3D(new GuiState3DWCS());

    private final GuiState3DWCS state;

    ViewStateEnum(GuiState3DWCS state) {
        this.state = state;
    }

    public GuiState3DWCS getState() {
        return this.state;
    }
}