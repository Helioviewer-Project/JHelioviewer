package org.helioviewer.jhv.gui;

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
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public class SplashScreen extends JFrame
{
	private SplashImagePanel imagePanel = new SplashImagePanel();
	private JProgressBar progressBar;

	public SplashScreen(int _steps)
	{
		getContentPane().setBackground(Color.BLACK);
		// initialize the frame itself
		setTitle("ESA JHelioviewer");
		setSize(new Dimension(400, 224));
		setLocationRelativeTo(null);
		getContentPane().setLayout(new BorderLayout());
		setFocusable(false);
		setResizable(false);
		setUndecorated(true);
		setIconImage(IconBank.getIcon(JHVIcon.HVLOGO_SMALL).getImage());
		
		progressBar = new JProgressBar(0, _steps);

		// if(isWindows())
		setType(java.awt.Window.Type.UTILITY);
		progressBar.setBorder(new LineBorder(new Color(0, 0, 0), 5));
		progressBar.setBackground(Color.BLACK);
		progressBar.setPreferredSize(new Dimension(0, 20));
		imagePanel.setBackground(Color.BLACK);
		imagePanel.setText("");
		getContentPane().add(imagePanel, BorderLayout.CENTER);
		getContentPane().add(progressBar, BorderLayout.SOUTH);

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
	public void progressTo(final String text)
	{
		if(!SwingUtilities.isEventDispatchThread())
		{
			try
			{
				SwingUtilities.invokeAndWait(new Runnable()
				{
					@Override
					public void run()
					{
						progressTo(text);
					}
				});
			}
			catch (Throwable t)
			{
				Telemetry.trackException(t);
			}
			return;
		}
		
		System.out.println(text);
		if (text != null)
			imagePanel.setText(text);

		if (progressBar.getValue() == progressBar.getMaximum())
			throw new RuntimeException("Too few steps declared (need >" + progressBar.getMaximum() + ")");

		progressBar.setValue(progressBar.getValue() + 1);
		
		repaint();
		RepaintManager.currentManager(this).paintDirtyRegions();
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
			label.setBounds(7, this.getHeight() - 20, 396, 20);
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
