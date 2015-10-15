package org.helioviewer.jhv.gui.statusLabels;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;
import java.time.LocalDateTime;

import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.helioviewer.jhv.base.coordinates.HeliocentricCartesianCoordinate;
import org.helioviewer.jhv.base.coordinates.HeliographicCoordinate;
import org.helioviewer.jhv.base.coordinates.HelioprojectiveCartesianCoordinate;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.opengl.RayTrace.HitpointType;
import org.helioviewer.jhv.opengl.RayTrace.Ray;
import org.helioviewer.jhv.viewmodel.TimeLine;

/**
 * Status panel for displaying the current mouse position.
 * 
 * <p>
 * If the the physical dimension of the image are known, the physical position
 * will be shown, otherwise, shows the screen position.
 * 
 * <p>
 * Basically, the information of this panel is independent from the active
 * layer.
 * 
 * <p>
 * If there is no layer present, this panel will be invisible.
 */
public class PositionStatusPanel extends StatusLabel implements MouseListener
{
	private static final char DEGREE = '\u00B0';
	private static final String title = " (X, Y) : ";
	private PopupState popupState;

	/**
	 * Default constructor.
	 * 
	 * @param imagePanel
	 *            ImagePanel to show mouse position for
	 */
	public PositionStatusPanel(MainFrame imagePanel)
	{
		setBorder(BorderFactory.createEtchedBorder());

		popupState = new PopupState();
		imagePanel.MAIN_PANEL.addPanelMouseListener(this);
		imagePanel.OVERVIEW_PANEL.addPanelMouseListener(this);
		this.addMouseListener(this);
		this.setComponentPopupMenu(popupState);

		setToolTipText(popupState.selectedItem.popupItem.getText());
	}

	/**
	 * Updates the displayed position.
	 * 
	 * If the physical dimensions are available, translates the screen
	 * coordinates to physical coordinates.
	 * 
	 * @param position
	 *            Position on the screen.
	 */
	private void updatePosition(@Nullable Ray ray)
	{
		if (ray == null)
		{
			this.setText(title);
			return;
		}

		HeliocentricCartesianCoordinate cart = new HeliocentricCartesianCoordinate(ray.getHitpoint().x,
				ray.getHitpoint().y, ray.getHitpoint().z);

		DecimalFormat df;
		String point = null;
		switch (this.popupState.getSelectedState())
		{
			case ARCSECS:
				LocalDateTime current = TimeLine.SINGLETON.getCurrentDateTime();

				HelioprojectiveCartesianCoordinate hpc = cart.toHelioprojectiveCartesianCoordinate(current);
				df = new DecimalFormat("#");
				point = "(" + df.format(hpc.getThetaXAsArcSec()) + "\" ," + df.format(hpc.getThetaYAsArcSec()) + "\")";
				break;
			case DEGREE:
				HeliographicCoordinate newCoord = cart.toHeliographicCoordinate();
				df = new DecimalFormat("#.##");
				if (!(ray.getHitpointType() == HitpointType.PLANE))
					point = "(" + df.format(newCoord.getHgLongitudeAsDeg()) + DEGREE + " ,"
							+ df.format(newCoord.getHgLatitudeAsDeg()) + DEGREE + ") ";
				else
					point = "";
				break;

			default:
				break;
		}
		this.setText(title + point);
	}

	@Override
	public void mouseMoved(MouseEvent e, Ray ray)
	{
		updatePosition(ray);
	}

	@Override
	public void mouseExited(MouseEvent e, Ray ray)
	{
		updatePosition(null);
	}

	public void mouseDragged(MouseEvent e)
	{
	}

	@Override
	public void mouseClicked(@Nullable MouseEvent e)
	{
		if (e == null)
			return;

		if (e.isPopupTrigger())
		{
			// TODO ?
			// popup(e);
		}
	}

	@Override
	public void mousePressed(@Nullable MouseEvent e)
	{
	}

	@Override
	public void mouseReleased(@Nullable MouseEvent e)
	{
		if (e == null)
			return;

		if (e.isPopupTrigger())
		{
			// TODO ?
			// popup(e);
		}
	}

	@Override
	public void mouseEntered(@Nullable MouseEvent e)
	{
	}

	@Override
	public void mouseExited(@Nullable MouseEvent e)
	{
	}

	private class PopupState extends JPopupMenu
	{
		private PopupItemState.PopupItemStates selectedItem = PopupItemState.PopupItemStates.ARCSECS;

		public PopupState()
		{
			for (PopupItemState.PopupItemStates popupItems : PopupItemState.PopupItemStates.values())
			{
				this.add(popupItems.popupItem);
				popupItems.popupItem.addActionListener(new ActionListener()
				{
					@Override
					public void actionPerformed(@Nullable ActionEvent e)
					{
						if (e == null)
							return;

						for (PopupItemState.PopupItemStates popupItems : PopupItemState.PopupItemStates.values())
						{
							if (popupItems.popupItem == e.getSource())
							{
								selectedItem = popupItems;
								PositionStatusPanel.this.setToolTipText(selectedItem.popupItem.getText());
								break;
							}
						}
						updateText();
					}
				});
			}
			this.updateText();
		}

		private void updateText()
		{
			for (PopupItemState.PopupItemStates popupItems : PopupItemState.PopupItemStates.values())
			{
				if (selectedItem == popupItems)
					popupItems.popupItem.setText(popupItems.popupItem.selectedText);
				else
					popupItems.popupItem.setText(popupItems.popupItem.unselectedText);
			}
		}

		public PopupItemState.PopupItemStates getSelectedState()
		{
			return this.selectedItem;
		}

	}

	private static class PopupItemState extends JMenuItem
	{
		public enum PopupItemStates
		{
			DEGREE("degrees (Heliographic)"), ARCSECS("arcsecs (Helioprojective cartesian)");

			private PopupItemState popupItem;

			private PopupItemStates(String _name)
			{
				popupItem = new PopupItemState(_name);
			}
		}

		private String unselectedText;
		private String selectedText;

		public PopupItemState(String _name)
		{
			unselectedText = _name;
			selectedText = _name + "  \u2713";
		}
	}
}
