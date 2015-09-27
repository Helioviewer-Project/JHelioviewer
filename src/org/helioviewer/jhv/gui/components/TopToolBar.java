package org.helioviewer.jhv.gui.components;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.actions.SetCameraPanInteractionAction;
import org.helioviewer.jhv.gui.actions.SetCameraRotationInteractionAction;
import org.helioviewer.jhv.gui.actions.SetCameraTrackAction;
import org.helioviewer.jhv.gui.actions.SetCameraYAxisBlockedAction;
import org.helioviewer.jhv.gui.actions.SetCameraZoomBoxInteractionAction;
import org.helioviewer.jhv.gui.actions.ToggleCoronaVisibilityAction;
import org.helioviewer.jhv.gui.actions.View2DAction;
import org.helioviewer.jhv.gui.actions.View3DAction;
import org.helioviewer.jhv.gui.sdocutout.SDOCutOutButton;
import org.helioviewer.jhv.opengl.camera.actions.ResetCameraAction;
import org.helioviewer.jhv.opengl.camera.actions.Zoom1To1Action;
import org.helioviewer.jhv.opengl.camera.actions.ZoomFitAction;
import org.helioviewer.jhv.opengl.camera.actions.ZoomInAction;
import org.helioviewer.jhv.opengl.camera.actions.ZoomOutAction;

/**
 * Toolbar containing the most common actions.
 * 
 * <p>
 * The toolbar provides a context menu to change its appearance.
 * 
 * @author Markus Langenberg
 * @author Andre Dau
 */
public class TopToolBar extends JToolBar implements MouseListener
{
	private static final long serialVersionUID = 1L;

	private enum DisplayMode
	{
		ICONANDTEXT, ICONONLY, TEXTONLY
	};

	private DisplayMode displayMode;

	private JToggleButton panButton;
	private JToggleButton rotateButton;
	private JToggleButton rotateButtonYAxis;
	private JToggleButton zoomBoxButton;

	private JToggleButton trackButton;
	private JToggleButton coronaVisibilityButton;
	private JButton zoomInButton, zoomOutButton, zoomFitButton, zoom1to1Button;
	private JButton resetCamera;
	private JToggleButton view2d;
	private JToggleButton view3d;

	/**
	 * Default constructor.
	 */
	public TopToolBar()
	{
		//setRollover(true);
		setFloatable(false);

		displayMode = DisplayMode.valueOf(Settings.getProperty("display.toolbar").toUpperCase());

		createNewToolBar();
		addMouseListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	public void mouseClicked(MouseEvent e) {
		maybeShowPopup(e);
	}

	/**
	 * {@inheritDoc}
	 */
	public void mouseEntered(MouseEvent e) {
	}

	/**
	 * {@inheritDoc}
	 */
	public void mouseExited(MouseEvent e) {
	}

	/**
	 * {@inheritDoc}
	 */
	public void mousePressed(MouseEvent e) {
		maybeShowPopup(e);
	}

	/**
	 * {@inheritDoc}
	 */
	public void mouseReleased(MouseEvent e) {
	}

	/**
	 * (Re)creates the toolbar.
	 * 
	 * This function is called during the construction of this panel as well as
	 * after the display mode has changed.
	 * 
	 *            Current selection mode, to select the correct button.
	 * @see #setDisplayMode(DisplayMode)
	 */
	private void createNewToolBar() {
		//removeAll();
		// Zoom
		zoomInButton = new JButton(new ZoomInAction(false));
		zoomOutButton = new JButton(new ZoomOutAction(false));
		zoomFitButton = new JButton(new ZoomFitAction(false));
		zoom1to1Button = new JButton(new Zoom1To1Action(false));	
		resetCamera = new JButton(new ResetCameraAction());

		addButton(zoomInButton);
		addButton(zoomOutButton);
		addButton(zoomFitButton);
		addButton(zoom1to1Button);

		addButton(resetCamera);

		addSeparator();

		// Selection
		ButtonGroup group = new ButtonGroup();

		panButton = new JToggleButton(new SetCameraPanInteractionAction());
		panButton.setIcon(IconBank.getIcon(JHVIcon.NEW_PAN, 24, 24));
		panButton.setToolTipText("Select Panning");
		panButton.setPreferredSize(new Dimension(50, 50));
		group.add(panButton);
		addButton(panButton);

		zoomBoxButton = new JToggleButton(new SetCameraZoomBoxInteractionAction());
		zoomBoxButton.setIcon(IconBank.getIcon(JHVIcon.NEW_ZOOMBOX, 24 ,24));
		zoomBoxButton.setToolTipText("Select Zoom Box");
		group.add(zoomBoxButton);
		addButton(zoomBoxButton);
		
		rotateButton = new JToggleButton(new SetCameraRotationInteractionAction());
		rotateButton.setIcon(IconBank.getIcon(JHVIcon.NEW_ROTATION, 24, 24));
		rotateButton.setToolTipText("Select Rotating");
		group.add(rotateButton);
		addButton(rotateButton);
		addSeparator();

		trackButton = new JToggleButton(new SetCameraTrackAction());
		trackButton.setIcon(IconBank.getIcon(JHVIcon.NEW_TRACK, 24, 24));
		trackButton
				.setToolTipText("Enable Solar Rotation Tracking");
		addButton(trackButton);

		coronaVisibilityButton = new JToggleButton(new ToggleCoronaVisibilityAction());
		coronaVisibilityButton.setSelected(false);
		coronaVisibilityButton.setIcon(IconBank
				.getIcon(JHVIcon.SUN_WITH_128x128, 24 ,24));
		coronaVisibilityButton.setSelectedIcon(IconBank
				.getIcon(JHVIcon.SUN_WITHOUT_128x128, 24 ,24));
		coronaVisibilityButton.setToolTipText("Toggle Corona Visibility");
		addButton(coronaVisibilityButton);

		rotateButtonYAxis = new JToggleButton(new SetCameraYAxisBlockedAction());
		rotateButtonYAxis.setIcon(IconBank.getIcon(JHVIcon.NEW_ROTATION_Y_AXIS, 24, 24));
		
		rotateButtonYAxis.setToolTipText("Enable rotation on Y-Axis");
		addButton(rotateButtonYAxis);
		
		addSeparator();

		ButtonGroup stateGroup = new ButtonGroup();
		view2d = new JToggleButton(new View2DAction());
		view2d.setIcon(IconBank.getIcon(JHVIcon.CAMERA_MODE_2D, 24 ,24));
		view2d.setSelectedIcon(IconBank.getIcon(JHVIcon.CAMERA_MODE_2D, 24 ,24));
		view2d.setText("2D");
		stateGroup.add(view2d);

		view3d = new JToggleButton(new View3DAction());
		view3d.setIcon(IconBank.getIcon(JHVIcon.CAMERA_MODE_3D, 24 ,24));
		view3d.setSelectedIcon(IconBank.getIcon(JHVIcon.CAMERA_MODE_3D, 24 ,24));
		view3d.setText("3D");
		view3d.setSelected(true);
		stateGroup.add(view3d);

		addButton(view2d);
		addButton(view3d);
		addSeparator();
		
		addButton(new SDOCutOutButton());
		addSeparator();
		
		set3DMode();

	}

	/**
	 * Adds a given button to the toolbar.
	 * 
	 * This function sets some standard values of the button regarding the
	 * appearance. The current display mode is taken into account.
	 * 
	 * @param button
	 *            Button to add
	 */
	public void addButton(AbstractButton button) {
		// button.setMargin(buttonMargin);
		button.setVerticalTextPosition(SwingConstants.BOTTOM);
		button.setHorizontalTextPosition(SwingConstants.CENTER);
		if (button instanceof JButton)
			button.setBorderPainted(false);
		
		//button.setFocusPainted(false);
		//button.setOpaque(false);
		button.addMouseListener(this);

		switch (displayMode) {
		case TEXTONLY:
			button.setIcon(null);
			break;
		case ICONONLY:
			button.setText("");
			break;
		default:
			break;
		}

		add(button);
	}

	/**
	 * Sets the current display mode.
	 * 
	 * This changes the way the toolbar is display.
	 * 
	 * @param mode
	 *            Display mode, can be either ICONANDTEXT, ICONONLY or TEXTONLY
	 */
	public void setDisplayMode(DisplayMode mode) {
		DisplayMode oldDisplayMode = displayMode;
		if (mode != null) {
			displayMode = mode;
			Settings.setProperty("display.toolbar", mode.toString()
					.toLowerCase());
		}
		
		createNewToolBar();

		firePropertyChange("displayMode", oldDisplayMode, displayMode);

		revalidate();
	}

	public void addToolbarPlugin(AbstractButton button) {
		if (displayMode == DisplayMode.ICONANDTEXT)
			this.add(button);
		else if (displayMode == DisplayMode.TEXTONLY)
			this.add(new JToggleButton(button.getText()));
		else
			this.add(new JToggleButton(button.getIcon()));
	}

	/**
	 * Shows the popup if the correct mouse button was pressed.
	 * 
	 * @param e
	 *            MouseEvent that triggered the event
	 */
	private void maybeShowPopup(MouseEvent e) {
		if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {

			JPopupMenu popUpMenu = new JPopupMenu();
			ButtonGroup group = new ButtonGroup();

			JRadioButtonMenuItem iconAndText = new JRadioButtonMenuItem(
					"Icon and Text", displayMode == DisplayMode.ICONANDTEXT);
			iconAndText.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setDisplayMode(DisplayMode.ICONANDTEXT);
					repaint();
				}
			});
			group.add(iconAndText);
			popUpMenu.add(iconAndText);

			JRadioButtonMenuItem iconOnly = new JRadioButtonMenuItem(
					"Icon Only", displayMode == DisplayMode.ICONONLY);
			iconOnly.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setDisplayMode(DisplayMode.ICONONLY);
					repaint();
				}
			});
			group.add(iconOnly);
			popUpMenu.add(iconOnly);

			JRadioButtonMenuItem textOnly = new JRadioButtonMenuItem(
					"Text Only", displayMode == DisplayMode.TEXTONLY);
			textOnly.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setDisplayMode(DisplayMode.TEXTONLY);
					repaint();
				}
			});
			group.add(textOnly);
			popUpMenu.add(textOnly);

			popUpMenu.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	public void set2DMode() {
        this.view2d.setSelected(true);
        this.view3d.setSelected(false);
		this.panButton.setSelected(true);
		this.rotateButton.setEnabled(false);
		this.zoomBoxButton.setEnabled(true);
		//this.rotateButtonYAxis.setEnabled(false);
	}

	public void set3DMode() {
        this.view2d.setSelected(false);
	    this.view3d.setSelected(true);
		this.rotateButton.setSelected(true);
		this.rotateButton.setEnabled(true);
		this.zoomBoxButton.setEnabled(false);
		//this.rotateButtonYAxis.setEnabled(true);
	}

	public void setTrack(boolean track) {
		this.trackButton.setSelected(track);
	}
}
