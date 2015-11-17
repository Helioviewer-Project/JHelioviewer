package org.helioviewer.jhv.gui.components;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.base.Settings;
import org.helioviewer.jhv.base.Settings.StringKey;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.actions.ResetCameraAction;
import org.helioviewer.jhv.gui.actions.SetCameraPanInteractionAction;
import org.helioviewer.jhv.gui.actions.SetCameraRotationInteractionAction;
import org.helioviewer.jhv.gui.actions.SetCameraTrackAction;
import org.helioviewer.jhv.gui.actions.SetCameraYAxisBlockedAction;
import org.helioviewer.jhv.gui.actions.SetCameraZoomBoxInteractionAction;
import org.helioviewer.jhv.gui.actions.View2DAction;
import org.helioviewer.jhv.gui.actions.View3DAction;
import org.helioviewer.jhv.gui.actions.Zoom1To1Action;
import org.helioviewer.jhv.gui.actions.ZoomFitAction;
import org.helioviewer.jhv.gui.actions.ZoomInAction;
import org.helioviewer.jhv.gui.actions.ZoomOutAction;
import org.helioviewer.jhv.gui.sdocutout.SDOCutOutButton;

/**
 * Toolbar containing the most common actions.
 * 
 * <p>
 * The toolbar provides a context menu to change its appearance.
 */
public class TopToolBar extends JToolBar implements MouseListener
{
	private enum DisplayMode
	{
		ICONANDTEXT, ICONONLY, TEXTONLY
	}

	private DisplayMode displayMode;

	private JToggleButton panButton;
	private JToggleButton rotateButton;
	private JToggleButton zoomBoxButton;

	private JToggleButton trackingEnabledButton;
	private JToggleButton view2DButton;
	private JToggleButton view3DButton;

	@SuppressWarnings("null")
	public TopToolBar()
	{
		//setRollover(true);
		setFloatable(false);

		displayMode = DisplayMode.valueOf(Settings.getString(StringKey.TOOLBAR_DISPLAY));

		createNewToolBar();
		addMouseListener(this);
	}

	public void mouseClicked(@Nullable MouseEvent e)
	{
		maybeShowPopup(e);
	}

	public void mouseEntered(@Nullable MouseEvent e)
	{
	}

	public void mouseExited(@Nullable MouseEvent e)
	{
	}

	public void mousePressed(@Nullable MouseEvent e)
	{
		maybeShowPopup(e);
	}

	public void mouseReleased(@Nullable MouseEvent e)
	{
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
		JButton zoomInButton = new JButton(new ZoomInAction(false));
		JButton zoomOutButton = new JButton(new ZoomOutAction(false));
		JButton zoomFitButton = new JButton(new ZoomFitAction(false));
		JButton zoom1to1Button = new JButton(new Zoom1To1Action(false));
		JButton resetCamera = new JButton(new ResetCameraAction());

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

		trackingEnabledButton = new JToggleButton(new SetCameraTrackAction());
		trackingEnabledButton.setIcon(IconBank.getIcon(JHVIcon.NEW_TRACK, 24, 24));
		trackingEnabledButton.setToolTipText("Enable Solar Rotation Tracking");
		addButton(trackingEnabledButton);

		/*JToggleButton coronaVisibilityButton = new JToggleButton(new ToggleCoronaVisibilityAction());
		coronaVisibilityButton.setSelected(false);
		coronaVisibilityButton.setIcon(IconBank.getIcon(JHVIcon.SUN_WITH_128x128, 24, 24));
		coronaVisibilityButton.setSelectedIcon(IconBank.getIcon(JHVIcon.SUN_WITHOUT_128x128, 24, 24));
		coronaVisibilityButton.setToolTipText("Toggle Corona Visibility");
		addButton(coronaVisibilityButton);*/

		JToggleButton rotateButtonYAxis = new JToggleButton(new SetCameraYAxisBlockedAction());
		rotateButtonYAxis.setIcon(IconBank.getIcon(JHVIcon.NEW_ROTATION_Y_AXIS, 24, 24));
		
		rotateButtonYAxis.setToolTipText("Enable rotation on Y-Axis");
		addButton(rotateButtonYAxis);
		
		addSeparator();

		ButtonGroup stateGroup = new ButtonGroup();
		view2DButton = new JToggleButton(new View2DAction());
		view2DButton.setIcon(IconBank.getIcon(JHVIcon.CAMERA_MODE_2D, 24 ,24));
		view2DButton.setSelectedIcon(IconBank.getIcon(JHVIcon.CAMERA_MODE_2D, 24 ,24));
		view2DButton.setText("2D");
		stateGroup.add(view2DButton);

		view3DButton = new JToggleButton(new View3DAction());
		view3DButton.setIcon(IconBank.getIcon(JHVIcon.CAMERA_MODE_3D, 24 ,24));
		view3DButton.setSelectedIcon(IconBank.getIcon(JHVIcon.CAMERA_MODE_3D, 24 ,24));
		view3DButton.setText("3D");
		view3DButton.setSelected(true);
		stateGroup.add(view3DButton);

		addButton(view2DButton);
		addButton(view3DButton);
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
	public void addButton(AbstractButton button)
	{
		// button.setMargin(buttonMargin);
		button.setVerticalTextPosition(SwingConstants.BOTTOM);
		button.setHorizontalTextPosition(SwingConstants.CENTER);
		if (button instanceof JButton)
			button.setBorderPainted(false);
		
		//button.setFocusPainted(false);
		//button.setOpaque(false);
		button.addMouseListener(this);

		switch (displayMode)
		{
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
	 * @param _mode
	 *            Display mode, can be either ICONANDTEXT, ICONONLY or TEXTONLY
	 */
	public void setDisplayMode(@Nonnull DisplayMode _mode)
	{
		DisplayMode oldDisplayMode = displayMode;
		displayMode = _mode;
		Settings.setString(StringKey.TOOLBAR_DISPLAY, _mode.toString());
		
		createNewToolBar();

		firePropertyChange("displayMode", oldDisplayMode, displayMode);

		revalidate();
		repaint();
	}

	public void addToolbarPlugin(AbstractButton button)
	{
		if (displayMode == DisplayMode.ICONANDTEXT)
			add(button);
		else if (displayMode == DisplayMode.TEXTONLY)
			add(new JToggleButton(button.getText()));
		else
			add(new JToggleButton(button.getIcon()));
	}

	/**
	 * Shows the popup if the correct mouse button was pressed.
	 * 
	 * @param e
	 *            MouseEvent that triggered the event
	 */
	private void maybeShowPopup(final @Nullable MouseEvent e)
	{
		if (e!= null && (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3))
		{
			JPopupMenu popUpMenu = new JPopupMenu();
			ButtonGroup group = new ButtonGroup();

			JRadioButtonMenuItem iconAndText = new JRadioButtonMenuItem("Icon and Text", displayMode == DisplayMode.ICONANDTEXT);
			iconAndText.addActionListener(new ActionListener()
			{
				public void actionPerformed(@Nullable ActionEvent e)
				{
					setDisplayMode(DisplayMode.ICONANDTEXT);
				}
			});
			group.add(iconAndText);
			popUpMenu.add(iconAndText);

			JRadioButtonMenuItem iconOnly = new JRadioButtonMenuItem("Icon Only", displayMode == DisplayMode.ICONONLY);
			iconOnly.addActionListener(new ActionListener()
			{
				public void actionPerformed(@Nullable ActionEvent e)
				{
					setDisplayMode(DisplayMode.ICONONLY);
				}
			});
			group.add(iconOnly);
			popUpMenu.add(iconOnly);

			JRadioButtonMenuItem textOnly = new JRadioButtonMenuItem("Text Only", displayMode == DisplayMode.TEXTONLY);
			textOnly.addActionListener(new ActionListener()
			{
				public void actionPerformed(@Nullable ActionEvent e)
				{
					setDisplayMode(DisplayMode.TEXTONLY);
				}
			});
			group.add(textOnly);
			popUpMenu.add(textOnly);

			popUpMenu.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	public void set2DMode()
	{
        view2DButton.setSelected(true);
        view3DButton.setSelected(false);
		panButton.setSelected(true);
		rotateButton.setEnabled(false);
		zoomBoxButton.setEnabled(true);
		//this.rotateButtonYAxis.setEnabled(false);
	}

	public void set3DMode()
	{
        view2DButton.setSelected(false);
	    view3DButton.setSelected(true);
		rotateButton.setSelected(true);
		rotateButton.setEnabled(true);
		zoomBoxButton.setEnabled(false);
		//rotateButtonYAxis.setEnabled(true);
	}

	public void setTracking(boolean _track)
	{
		trackingEnabledButton.setSelected(_track);
	}
}
