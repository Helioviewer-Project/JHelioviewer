package org.helioviewer.jhv.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import javax.swing.border.EmptyBorder;

public class SplashScreen extends JFrame
{
	private JProgressBar progressBar;
	JLabel lblAsdasd;

	public SplashScreen(int _steps)
	{
		BufferedImage image = IconBank.getImage(JHVIcon.SPLASH);
		lblAsdasd = new JLabel(" ");
		lblAsdasd.setBackground(Color.BLACK);
		lblAsdasd.setBorder(new EmptyBorder(5, 5, 5, 5));
		lblAsdasd.setForeground(Color.WHITE);
				
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
		progressBar.setValue(0);

		setType(java.awt.Window.Type.UTILITY);
		progressBar.setBorder(new LineBorder(new Color(0, 0, 0), 5));
		progressBar.setPreferredSize(new Dimension(0, 20));
		JLabel label = new JLabel(new ImageIcon(image));
		label.setBackground(Color.BLACK);
		getContentPane().add(label, BorderLayout.NORTH);
		getContentPane().add(lblAsdasd, BorderLayout.CENTER);
		getContentPane().add(progressBar, BorderLayout.SOUTH);

		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		validate();
		pack();
		
		// show the splash screen
		setVisible(true);
		
		setAlwaysOnTop(true);
		
		repaint();
		RepaintManager.currentManager(this).validateInvalidComponents();
		RepaintManager.currentManager(this).paintDirtyRegions();
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
			lblAsdasd.setText(text+" ");
		else
			lblAsdasd.setText(" ");

		if (progressBar.getValue() == progressBar.getMaximum())
			throw new RuntimeException("Too few steps declared (need >" + progressBar.getMaximum() + ")");

		progressBar.setValue(progressBar.getValue() + 1);
		
		repaint();
		RepaintManager.currentManager(this).paintDirtyRegions();
	}
}
