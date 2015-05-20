package org.helioviewer.jhv.gui.components.newComponents;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.time.LocalDateTime;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.border.EmptyBorder;

import org.helioviewer.jhv.layers.Layers;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class AddLayerPanel extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5707539021281900015L;
	private final JPanel contentPanel = new JPanel();

	private JLabel lblFilter1, lblFilter2, lblInstrument;
	private JComboBox<InstrumentModel.Observatory> cmbbxObservatory;
	private JComboBox<InstrumentModel.Instrument> cmbbxInstrument;
	private JComboBox<InstrumentModel.Filter> cmbbxFilter1, cmbbxFilter2;
	private NewDatePicker datePickerStartDate;
	private NewDatePicker datePickerEndDate;
	private JSpinner candence;

	public enum TIME_STEPS {
		SEC("sec"), MIN("min"), HOUR("hour"), DAY("day"), GET_ALL("get all");

		private String name;

		TIME_STEPS(String name) {
			this.name = name;
		}

	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			AddLayerPanel dialog = new AddLayerPanel();
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public AddLayerPanel() {
		// super(ImageViewerGui.getMainFrame());
		this.setAlwaysOnTop(true);
		setBounds(100, 100, 450, 310);
		initGui();
		addData();
	}

	public void addData() {
		InstrumentModel instrumentModel = InstrumentModel.singelton;
		LocalDateTime endDateTime = LocalDateTime.now();
		LocalDateTime startDateTime = endDateTime.minusDays(1);

		for (InstrumentModel.Observatory observatory : instrumentModel
				.getObservatories()) {
			cmbbxObservatory.addItem(observatory);
		}

		cmbbxObservatory.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				InstrumentModel.Observatory observatory = ((InstrumentModel.Observatory) e
						.getItem());
				lblInstrument.setText("");
				lblFilter1.setText("");
				lblFilter2.setText("");
				lblInstrument.setVisible(true);
				lblFilter1.setVisible(true);
				lblFilter2.setVisible(true);
				cmbbxInstrument.setVisible(true);
				cmbbxFilter1.setVisible(true);
				cmbbxFilter2.setVisible(true);
				switch (observatory.getUiLabels().size()) {
				case 0:
					lblInstrument.setVisible(false);
					cmbbxInstrument.setVisible(false);
				case 1:
					lblFilter1.setVisible(false);
					cmbbxFilter1.setVisible(false);
				case 2:
					lblFilter2.setVisible(false);
					cmbbxFilter2.setVisible(false);
					break;

				default:
					break;
				}
				try {
					lblInstrument.setText(observatory.getUiLabels().get(0));
					try {
						lblFilter1.setText(observatory.getUiLabels().get(1));
						try {
							lblFilter2
									.setText(observatory.getUiLabels().get(2));
						} catch (Exception e2) {
							System.out.println("Filter2 not available");
						}
					} catch (Exception e2) {
						System.out.println("Filter1 not available");
					}
				} catch (Exception e2) {
					System.out.println("Insturment not available");
				}

				cmbbxInstrument.removeAllItems();
				cmbbxFilter1.removeAllItems();
				cmbbxFilter2.removeAllItems();
				for (InstrumentModel.Instrument instrument : observatory
						.getInstruments()) {
					cmbbxInstrument.addItem(instrument);
				}
			}
		});

		cmbbxInstrument.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				System.out.println(e.getItem());
				cmbbxFilter1.removeAllItems();
				cmbbxFilter2.removeAllItems();
				for (InstrumentModel.Filter filter : ((InstrumentModel.Instrument) e
						.getItem()).getFilters()) {
					cmbbxFilter1.addItem(filter);
				}
			}
		});

		cmbbxFilter1.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				cmbbxFilter2.removeAllItems();
				for (InstrumentModel.Filter filter : ((InstrumentModel.Filter) e
						.getItem()).getFilters()) {
					cmbbxFilter2.addItem(filter);
				}
			}
		});
		if (cmbbxObservatory.getItemCount() > 0) {
			cmbbxObservatory.setSelectedIndex(1);
			cmbbxObservatory.setSelectedIndex(0);
		}
	}

	private void initGui() {
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel
				.setLayout(new FormLayout(new ColumnSpec[] {
						FormFactory.RELATED_GAP_COLSPEC,
						ColumnSpec.decode("default:grow"),
						FormFactory.RELATED_GAP_COLSPEC,
						FormFactory.DEFAULT_COLSPEC,
						FormFactory.RELATED_GAP_COLSPEC,
						ColumnSpec.decode("default:grow"), }, new RowSpec[] {
						FormFactory.RELATED_GAP_ROWSPEC,
						FormFactory.DEFAULT_ROWSPEC,
						FormFactory.RELATED_GAP_ROWSPEC,
						FormFactory.DEFAULT_ROWSPEC,
						FormFactory.RELATED_GAP_ROWSPEC,
						FormFactory.DEFAULT_ROWSPEC,
						FormFactory.RELATED_GAP_ROWSPEC,
						FormFactory.DEFAULT_ROWSPEC,
						FormFactory.RELATED_GAP_ROWSPEC,
						FormFactory.DEFAULT_ROWSPEC,
						FormFactory.RELATED_GAP_ROWSPEC,
						FormFactory.DEFAULT_ROWSPEC,
						FormFactory.RELATED_GAP_ROWSPEC,
						FormFactory.DEFAULT_ROWSPEC,
						FormFactory.RELATED_GAP_ROWSPEC,
						FormFactory.DEFAULT_ROWSPEC, }));

		{
			datePickerStartDate = new NewDatePicker(LocalDateTime.now()
					.minusDays(1));
			contentPanel.add(datePickerStartDate, "2, 2, 5, 1, fill, top");
		}
		{
			datePickerEndDate = new NewDatePicker(LocalDateTime.now());
			contentPanel.add(datePickerEndDate, "2, 4, 5, 1, fill, top");
		}
		{
			JLabel lblCadence = new JLabel("Time Step");
			contentPanel.add(lblCadence, "2, 6");
		}
		{
			candence = new JSpinner();
			candence.setValue(20);
			candence.setPreferredSize(new Dimension(80, 20));
			contentPanel.add(candence, "4, 6");
		}
		{
			JComboBox<String> comboBox = new JComboBox<String>();
			for (TIME_STEPS timeStep : TIME_STEPS.values()) {
				comboBox.addItem(timeStep.name);
			}
			contentPanel.add(comboBox, "6, 6, fill, default");
		}
		{
			JSeparator separator = new JSeparator();
			contentPanel.add(separator, "2, 8, 5, 1");
		}
		{
			JLabel lblObservatory = new JLabel("Observatory");
			contentPanel.add(lblObservatory, "2, 10");
		}
		{
			cmbbxObservatory = new JComboBox<InstrumentModel.Observatory>();
			contentPanel.add(cmbbxObservatory, "6, 10, fill, default");
		}
		{
			lblInstrument = new JLabel("Instrument");
			contentPanel.add(lblInstrument, "2, 12");
		}
		{
			cmbbxInstrument = new JComboBox<InstrumentModel.Instrument>();
			contentPanel.add(cmbbxInstrument, "6, 12, fill, default");
		}
		{
			lblFilter1 = new JLabel("");
			contentPanel.add(lblFilter1, "2, 14");
		}
		{
			cmbbxFilter1 = new JComboBox<InstrumentModel.Filter>();
			contentPanel.add(cmbbxFilter1, "6, 14, fill, default");
		}

		{
			lblFilter2 = new JLabel("");
			contentPanel.add(lblFilter2, "2, 16");
		}
		{
			cmbbxFilter2 = new JComboBox<InstrumentModel.Filter>();
			contentPanel.add(cmbbxFilter2, "6, 16, fill, default");
		}

		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
				okButton.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						InstrumentModel.Filter filter = (InstrumentModel.Filter) cmbbxFilter2
								.getSelectedItem();
						if (filter == null) {
							filter = (InstrumentModel.Filter) cmbbxFilter1
									.getSelectedItem();
						}

						if (filter != null) {
							Layers.LAYERS.addLayer(filter.sourceId,
									datePickerStartDate.getDateTime(),
									datePickerEndDate.getDateTime(),
									(int) candence.getValue());
						}

						setVisible(false);
					}
				});
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
				cancelButton.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						setVisible(false);
					}
				});
			}
		}
	}
}
