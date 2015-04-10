package org.helioviewer.jhv.internal_plugins.filter.opacity;

import java.awt.Dimension;

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
 * Panel containing a spinner for changing the opacity of the image.
 * 
 * @author Markus Langenberg
 * @author Malte Nuhn
 */
public class OpacityPanel extends FilterPanel implements ChangeListener, FilterAlignmentDetails {

    private static final long serialVersionUID = 1L;
    private JSlider opacitySlider;
    private JLabel opacityLabel;
    private JLabel title;
    private OpacityFilter filter;

    /**
     * Default constructor.
     * 
     */
    public OpacityPanel() {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        title = new JLabel("Opacity:");
        title.setPreferredSize(new Dimension(FilterPanel.TITLE_WIDTH, FilterPanel.HEIGHT));
        add(title);
        
        opacitySlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
        opacitySlider.setMajorTickSpacing(20);
        opacitySlider.setPaintTicks(true);
        opacitySlider.setPreferredSize(new Dimension(150, opacitySlider.getPreferredSize().height));
        opacitySlider.addChangeListener(this);
        WheelSupport.installMouseWheelSupport(opacitySlider);
        add(opacitySlider);
        
        opacityLabel = new JLabel("0%");
        opacityLabel.setHorizontalAlignment(JLabel.RIGHT);
        opacityLabel.setPreferredSize(new Dimension(FilterPanel.VALUE_WIDTH, FilterPanel.HEIGHT));
        add(opacityLabel);

        setEnabled(false);
    }

    /**
     * Override the setEnabled method in order to keep the containing
     * components' enabledState synced with the enabledState of this component.
     */
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        opacitySlider.setEnabled(enabled);
        opacityLabel.setEnabled(enabled);
        title.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */

    public void setFilter(Filter filter) {
        if (filter instanceof OpacityFilter) {
            this.filter = (OpacityFilter) filter;
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
     * Sets the opacity of the image.
     */
    public void stateChanged(ChangeEvent e) {
        if (filter != null) {
            float value = (float) opacitySlider.getValue() / 100.0f;
            opacityLabel.setText(opacitySlider.getValue() + "%");
            filter.setOpacity(value);
            GuiState3DWCS.mainComponentView.getComponent().repaint();
        }
    }

    /**
     * Sets the opacity value.
     * 
     * This may be useful, if the opacity is changed from another source than
     * the slider itself.
     * 
     * @param opacity
     *            New opacity value. Must be within [0, 100]
     */
    void setValue(float opacity) {
        opacitySlider.setValue((int) (opacity * 100));
    }

    /**
     * {@inheritDoc}
     */
    public int getDetails() {
        return FilterAlignmentDetails.POSITION_OPACITY;
    }
}
