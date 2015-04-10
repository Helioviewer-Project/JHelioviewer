package org.helioviewer.jhv.internal_plugins.filter.gammacorrection;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.gui.components.WheelSupport;
import org.helioviewer.jhv.plugins.viewmodelplugin.filter.FilterAlignmentDetails;
import org.helioviewer.jhv.plugins.viewmodelplugin.filter.FilterPanel;
import org.helioviewer.jhv.plugins.viewmodelplugin.filter.FilterTabPanelManager.Area;
import org.helioviewer.jhv.viewmodel.filter.Filter;

/**
 * Panel containing a slider for changing the gamma value of the image.
 * 
 * <p>
 * To be able to reset the gamma value to 1.0, the slider snaps to 1.0 if it
 * close to it.
 * 
 * @author Markus Langenberg
 */
public class GammaCorrectionPanel extends FilterPanel implements ChangeListener, MouseListener, FilterAlignmentDetails {

    private static final long serialVersionUID = 1L;
    private static double factor = 0.01 * Math.log(10);

    private JSlider gammaSlider;
    private JLabel title;
    private JLabel gammaLabel;
    private GammaCorrectionFilter filter;

    /**
     * Default constructor.
     * 
     */
    public GammaCorrectionPanel() {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        title = new JLabel("Gamma:");
        title.setPreferredSize(new Dimension(FilterPanel.TITLE_WIDTH, FilterPanel.HEIGHT));
        add(title);

        gammaSlider = new JSlider(JSlider.HORIZONTAL, -100, 100, 0);
        gammaSlider.setMajorTickSpacing(20);
        gammaSlider.setPaintTicks(true);
        gammaSlider.setPreferredSize(new Dimension(150, gammaSlider.getPreferredSize().height));
        gammaSlider.addChangeListener(this);
        gammaSlider.addMouseListener(this);
        WheelSupport.installMouseWheelSupport(gammaSlider);
        add(gammaSlider);

        gammaLabel = new JLabel("1.0");
        gammaLabel.setHorizontalAlignment(JLabel.RIGHT);
        gammaLabel.setPreferredSize(new Dimension(FilterPanel.VALUE_WIDTH, FilterPanel.HEIGHT));
        add(gammaLabel);

        setEnabled(false);
    }

    /**
     * {@inheritDoc}
     */

    public void setFilter(Filter filter) {
        if (filter instanceof GammaCorrectionFilter) {
            this.filter = (GammaCorrectionFilter) filter;
            this.filter.setPanel(this);
            setEnabled(true);
        } else {
            setEnabled(false);
        }
    }

    /**
     * {@inheritDoc}
     */

    public Area getArea() {
        return Area.TOP;
    }

    /**
     * Sets the gamma value of the image.
     */
    public void stateChanged(ChangeEvent e) {
        int sliderValue = gammaSlider.getValue();

        double gamma = Math.exp(sliderValue * factor);
        filter.setGamma((float) gamma);

        String label = Double.toString(Math.round(gamma * 10.0) * 0.1);
        if (sliderValue == 100) {
            label = label.substring(0, 4);
        } else {
            label = label.substring(0, 3);
        }
        gammaLabel.setText(label);
        GuiState3DWCS.mainComponentView.getComponent().repaint();
    }

    /**
     * {@inheritDoc} In this case, does nothing.
     */
    public void mouseClicked(MouseEvent e) {
    }

    /**
     * {@inheritDoc} In this case, does nothing.
     */
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * {@inheritDoc} In this case, does nothing.
     */
    public void mouseExited(MouseEvent e) {
    }

    /**
     * {@inheritDoc} In this case, does nothing.
     */
    public void mousePressed(MouseEvent e) {
    }

    /**
     * {@inheritDoc} In this case, snaps the slider to 1.0 if it is close to it.
     */
    public void mouseReleased(MouseEvent e) {
        int sliderValue = gammaSlider.getValue();

        if (sliderValue <= 5 && sliderValue >= -5 && sliderValue != 0) {
            gammaSlider.setValue(0);
        }
    }

    public int getDetails() {
        return FilterAlignmentDetails.POSITION_GAMMA;
    }

    /**
     * Override the setEnabled method in order to keep the containing
     * components' enabledState synced with the enabledState of this component.
     */

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        gammaSlider.setEnabled(enabled);
        gammaLabel.setEnabled(enabled);
        title.setEnabled(enabled);
    }

    /**
     * Sets the panel values.
     * 
     * This may be useful, if the values are changed from another source than
     * the panel itself.
     * 
     * @param gamma
     *            New gamma value, must be within [0.1, 10]
     */
    void setValue(float gamma) {
        gammaSlider.setValue((int) (Math.log(gamma) / factor));
    }
}
