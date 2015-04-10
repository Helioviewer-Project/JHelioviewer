package org.helioviewer.jhv.internal_plugins.filter.channelMixer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.plugins.viewmodelplugin.filter.FilterAlignmentDetails;
import org.helioviewer.jhv.plugins.viewmodelplugin.filter.FilterPanel;
import org.helioviewer.jhv.plugins.viewmodelplugin.filter.FilterTabPanelManager.Area;
import org.helioviewer.jhv.viewmodel.filter.Filter;
import org.helioviewer.jhv.viewmodel.imagedata.ColorMask;

/**
 * Panel containing three check boxes to modify the color mask of an image.
 * 
 * @author Markus Langenberg
 */
public class ChannelMixerPanel extends FilterPanel implements ItemListener, FilterAlignmentDetails {

    private static final long serialVersionUID = 1L;

    private JCheckBox redCheckBox;
    private JCheckBox greenCheckBox;
    private JCheckBox blueCheckBox;
    private JLabel title;
    private ChannelMixerFilter filter;

    /**
     * Default constructor.
     * 
     */
    public ChannelMixerPanel() {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        title = new JLabel("Channels:");
        title.setPreferredSize(new Dimension(FilterPanel.TITLE_WIDTH, FilterPanel.HEIGHT));
        add(title);

        JPanel boxPanel = new JPanel(new GridLayout(1, 3));
        // boxPanel.setLayout(new BoxLayout(boxPanel, BoxLayout.LINE_AXIS));

        redCheckBox = new JCheckBox("Red", true);
        redCheckBox.setPreferredSize(new Dimension(redCheckBox.getPreferredSize().width, FilterPanel.HEIGHT));
        redCheckBox.setToolTipText("Unchecked to omit the red color channel when drawing this layer");
        redCheckBox.addItemListener(this);
        boxPanel.add(redCheckBox, BorderLayout.WEST);

        greenCheckBox = new JCheckBox("Green", true);
        greenCheckBox.setPreferredSize(new Dimension(greenCheckBox.getPreferredSize().width, FilterPanel.HEIGHT));
        greenCheckBox.setToolTipText("Unchecked to omit the green color channel when drawing this layer");
        greenCheckBox.addItemListener(this);
        boxPanel.add(greenCheckBox, BorderLayout.CENTER);

        blueCheckBox = new JCheckBox("Blue", true);
        blueCheckBox.setPreferredSize(new Dimension(blueCheckBox.getPreferredSize().width, FilterPanel.HEIGHT));
        blueCheckBox.setToolTipText("Unchecked to omit the blue color channel when drawing this layer");
        blueCheckBox.addItemListener(this);
        boxPanel.add(blueCheckBox, BorderLayout.EAST);

        add(boxPanel);
        setEnabled(false);
    }

    /**
     * {@inheritDoc}
     */

    public void setFilter(Filter filter) {
        if (filter instanceof ChannelMixerFilter) {
            this.filter = (ChannelMixerFilter) filter;
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
     * Changes the channel selection of the image.
     */
    public void itemStateChanged(ItemEvent e) {
        filter.setColorMask(redCheckBox.isSelected(), greenCheckBox.isSelected(), blueCheckBox.isSelected());
        GuiState3DWCS.mainComponentView.getComponent().repaint();
    }

    public int getDetails() {
        return FilterAlignmentDetails.POSITION_CHANNELMIXER;
    }

    /**
     * Override the setEnabled method in order to keep the containing
     * components' enabledState synced with the enabledState of this component.
     */

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        redCheckBox.setEnabled(enabled);
        greenCheckBox.setEnabled(enabled);
        blueCheckBox.setEnabled(enabled);
        title.setEnabled(enabled);
    }

    /**
     * Sets the panel values.
     * 
     * This may be useful, if the values are changed from another source than
     * the panel itself.
     * 
     * @param colorMask
     *            Mask representing the new values
     */
    void setValue(ColorMask colorMask) {
        redCheckBox.setSelected(colorMask.showRed());
        greenCheckBox.setSelected(colorMask.showGreen());
        blueCheckBox.setSelected(colorMask.showBlue());
    }
}
