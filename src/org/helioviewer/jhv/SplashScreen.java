package org.helioviewer.jhv;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
 * 
 * @author Stephan Pagel
 */
class SplashScreen extends JFrame {

	// ////////////////////////////////////////////////////////////////
	// Definitions
	// ////////////////////////////////////////////////////////////////

	private static final long serialVersionUID = 1L;

	private static final SplashScreen SINGLETON = new SplashScreen();

	private SplashImagePanel imagePanel = new SplashImagePanel();
	private JProgressBar progressBar = new JProgressBar(0, 100);

	private int steps = 1;
	private int currentStep = 1;

	// ////////////////////////////////////////////////////////////////
	// Methods
	// ////////////////////////////////////////////////////////////////

	/**
	 * Default constructor.
	 * */
	private SplashScreen() {

		// initialize the frame itself
		initFrame();

		// initialize the visual components
		initVisualComponents();

		// show the splash screen
		setVisible(true);
		
		setAlwaysOnTop(true);
	}

	/**
	 * Method returns the sole instance of this class.
	 * 
	 * @return the only instance of this class.
	 * */
	public static SplashScreen getSingletonInstance() {
		return SINGLETON;
	}

	/**
	 * Initializes the dialog controller itself.
	 * */
	private void initFrame() {
		setTitle("ESA JHelioviewer");
		setSize(new Dimension(400, 220));
		setLocationRelativeTo(null);
		setLayout(new BorderLayout());
		setFocusable(false);
		setResizable(false);
		setUndecorated(true);
    setIconImage(IconBank.getIcon(JHVIcon.HVLOGO_SMALL).getImage());

    //if(isWindows())
      setType(java.awt.Window.Type.UTILITY);
	}
	
	/*private boolean isWindows()
	{
	  //should work, no matter what os/jdk release
	  try
	  {
  	  String os=System.getProperty("os.name");
  	  if(os==null)
  	    return false;
  	  
  	  return os.startsWith("Windows");
	  }
	  catch(Exception _e)
	  {
	    return false;
	  }
	}*/

	/**
	 * Initializes all visual components on the controller.
	 * */
	private void initVisualComponents() {
		progressBar.setValue(0);
		progressBar.setPreferredSize(new Dimension(progressBar.getWidth(), 20));
		imagePanel.setText("");
		add(imagePanel, BorderLayout.CENTER);
		add(progressBar, BorderLayout.SOUTH);
	}

	/**
	 * Sets the number of main progress steps. The lowest allowed value is 1.
	 * 
	 * @param steps
	 *            number of steps.
	 */
	public void setProgressSteps(int steps) {

		if (steps >= 1) {
			this.steps = steps;
			progressBar.setMaximum(steps * 100);
		}
	}

	/**
	 * Sets the current main progress step. Future changes to the progress bar
	 * value will be made inside the range of this step. The lowest allowed
	 * value is 1 and the highest value is the number of main progress steps.
	 * 
	 * @param step
	 *            current main progress step.
	 */
	public void setCurrentStep(int step) {

		if (step >= 1 && step <= steps) {
			this.currentStep = step - 1;

			progressBar.setValue(currentStep * 100);
		}
	}

	/**
	 * Returns the current main progress step.
	 * 
	 * @return current main progress step.
	 */
	public int getCurrentStep() {
		return currentStep + 1;
	}

	/**
	 * Sets the value of the progress bar which is displayed on the splash
	 * screen. The value must be between 0 and 100 otherwise it will be ignored.
	 * 
	 * @param value
	 *            new value for the progress bar.
	 * */
	public void setProgressValue(int value) {

		if (value >= 0 && value <= 100)
			progressBar.setValue(currentStep * 100 + value);
	}

	/**
	 * Sets the text which gives information about what actually happens. The
	 * text will be displayed above the progress bar. If the passed value is
	 * null nothing will happen.
	 * 
	 * @param text
	 *            new text which shall be displayed.
	 * */
	public void progressTo(String text)
	{
		System.out.println(text);
		if (text != null)
			imagePanel.setText(text);
		
		if (currentStep + 1 < steps) {
			currentStep++;

			progressBar.setValue(currentStep * 100);
		}
	}

	/**
	 * Returns a progress bar object. The values which will be set to this
	 * progress bar will be mapped to the progress bar which is displayed on the
	 * splash screen.
	 * 
	 * @return progress bar object.
	 * */
	public JProgressBar getProgressBar() {

		JProgressBar progressBar = new JProgressBar();

		progressBar.addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent e) {
				int value = ((JProgressBar) e.getSource()).getValue();
				int max = ((JProgressBar) e.getSource()).getMaximum();

				setProgressValue((int) ((float) value / (float) max * 100.0f));
			}
		});

		return progressBar;
	}

	/**
	 * Returns the label which displays the current information text.
	 * 
	 * @return label instance which is displayed in the splash screen.
	 * */
	public JLabel getInfoLabel() {
		return imagePanel.getLabel();
	}

	/**
	 * The panel acts as container which displays the splash screen image and
	 * position the label which displays the current status information.
	 * 
	 * @author Stephan Pagel
	 * */
	private static class SplashImagePanel extends JPanel {

		// ////////////////////////////////////////////////////////////
		// Definitions
		// ////////////////////////////////////////////////////////////

		private static final long serialVersionUID = 1L;

		private BufferedImage image = IconBank.getImage(JHVIcon.SPLASH);
		private JLabel label = new JLabel("");

		// ////////////////////////////////////////////////////////////
		// Methods
		// ////////////////////////////////////////////////////////////

		/**
		 * Default constructor.
		 * */
		public SplashImagePanel() {

			// set basic layout
			setLayout(null);

			// set size of panel
			if (image != null) {
				setPreferredSize(new Dimension(image.getWidth(),
						image.getHeight()));
				setSize(image.getWidth(), image.getHeight());
			} else {
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
		public void setText(String text) {
			label.setText(text);
		}

		/**
		 * Returns the instance of the label which displays the current status
		 * information.
		 * 
		 * @return label object which displays the current status information.
		 * */
		public JLabel getLabel() {
			return label;
		}

		/**
		 * Draws the splash screen image on the panel. If the image is not
		 * available nothing will happen.
		 * 
		 * @param g
		 *            Graphics object where image shall be drawn.
		 */
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (image != null)
				g.drawImage(image, 0, 0, null);
		}
	}
}
