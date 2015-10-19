package org.helioviewer.jhv.base;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.annotation.Nullable;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

/**
 * Represents the splash screen which will be displayed when program is
 * starting.
 * 
 * The splash screen manages a progress bar and a label, representing the
 * current state of starting JHV. It is connected to
 * {@link org.helioviewer.jhv.gui.components.StatusPanel}, so every call to
 * {@link org.helioviewer.jhv.gui.components.StatusPanel#setStatusInfoText(String)}
 * results in updating the splash screen to. This behavior is useful for
 * plugins.
 */
public class SplashScreen extends JFrame
{
	private SplashImagePanel imagePanel = new SplashImagePanel();
	private JProgressBar progressBar = new JProgressBar(0, 100);

	public SplashScreen(int _steps)
	{
		// initialize the frame itself
		setTitle("ESA JHelioviewer");
		setSize(new Dimension(400, 220));
		setLocationRelativeTo(null);
		setLayout(new BorderLayout());
		setFocusable(false);
		setResizable(false);
		setUndecorated(true);
		setIconImage(IconBank.getIcon(JHVIcon.HVLOGO_SMALL).getImage());

		// if(isWindows())
		setType(java.awt.Window.Type.UTILITY);

		// initialize the visual components
		progressBar.setValue(0);
		progressBar.setPreferredSize(new Dimension(progressBar.getWidth(), 20));
		progressBar.setMaximum(_steps);
		imagePanel.setText("");
		add(imagePanel, BorderLayout.CENTER);
		add(progressBar, BorderLayout.SOUTH);

		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		// show the splash screen
		setVisible(true);

		setAlwaysOnTop(true);
	}

	@Override
	public void dispose()
	{
		super.dispose();

		if (progressBar.getValue() != progressBar.getMaximum())
			throw new RuntimeException("Too many steps declared (" + progressBar.getMaximum() + " instead of "
					+ progressBar.getValue() + ")");
	}

	/**
	 * Sets the text which gives information about what actually happens. The
	 * text will be displayed above the progress bar. If the passed value is
	 * null nothing will happen.
	 * 
	 * @param text
	 *            new text which shall be displayed.
	 */
	public void progressTo(String text)
	{
		System.out.println(text);
		if (text != null)
			imagePanel.setText(text);

		if (progressBar.getValue() == progressBar.getMaximum())
			throw new RuntimeException("Too few steps declared (need >" + progressBar.getMaximum() + ")");

		progressBar.setValue(progressBar.getValue() + 1);
	}

	/**
	 * The panel acts as container which displays the splash screen image and
	 * position the label which displays the current status information.
	 */
	private static class SplashImagePanel extends JPanel
	{
		private final @Nullable BufferedImage image = IconBank.getImage(JHVIcon.SPLASH);
		private JLabel label = new JLabel("");

		@SuppressWarnings("null")
		public SplashImagePanel()
		{
			// set basic layout
			setLayout(null);

			// set size of panel
			if (image != null)
			{
				setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
				setSize(image.getWidth(), image.getHeight());
			}
			else
			{
				setPreferredSize(new Dimension(400, 200));
				setSize(400, 200);
			}

			// set label for displaying status information
			label.setOpaque(false);
			label.setBounds(2, this.getHeight() - 20, 396, 20);
			label.setForeground(Color.WHITE);
			add(label);
		}

		/**
		 * Sets the information text of the current status.
		 * 
		 * @param text
		 *            text which shall be displayed.
		 */
		public void setText(String text)
		{
			label.setText(text);
		}

		/**
		 * Draws the splash screen image on the panel. If the image is not
		 * available nothing will happen.
		 * 
		 * @param g
		 *            Graphics object where image shall be drawn.
		 */
		protected void paintComponent(@Nullable Graphics g)
		{
			if(g==null)
				return;
			
			super.paintComponent(g);
			if (image != null)
				g.drawImage(image, 0, 0, null);
		}
	}
}
