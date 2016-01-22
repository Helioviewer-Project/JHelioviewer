package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;

import javax.annotation.Nullable;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.helioviewer.jhv.base.Globals;
import org.helioviewer.jhv.base.Observatories;
import org.helioviewer.jhv.base.Observatories.Filter;
import org.helioviewer.jhv.base.Observatories.Observatory;
import org.helioviewer.jhv.base.Settings;
import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.dialogs.calender.DatePicker;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.jp2view.newjpx.KakaduLayer;

public class AddLayerDialog extends JDialog
{
	private final JPanel contentPanel = new JPanel();

	private JLabel lblFilter, lblFilter1, lblFilter2;
	private JComboBox<Observatories.Observatory> cmbbxObservatory;
	private JComboBox<Observatories.Filter> cmbbxFilter, cmbbxFilter1, cmbbxFilter2;
	private JComboBox<TimeSteps> cmbbxTimeSteps;
	private DatePicker datePickerStartDate;
	private DatePicker datePickerEndDate;
	private JSpinner cadence;

	//FIXME: remove minusYears for release, check data availability
	private static LocalDateTime lastStart = LocalDateTime.now().minusYears(3).minusDays(1);
	private static LocalDateTime lastEnd = LocalDateTime.now().minusYears(3); 
	private JPanel layerPanel;
	private JPanel panel;
	
	private enum TimeSteps
	{
		SEC("sec", 1),
		MIN("min", 60),
		HOUR("hour", 3600),
		DAY("day", 3600 * 24),
		GET_ALL("get all", 0);

		final String name;
		final int factor;

		TimeSteps(String _name, int _factor)
		{
			name = _name;
			factor = _factor;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	public AddLayerDialog()
	{
		super(MainFrame.SINGLETON, "Add Layer", true);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
    	Telemetry.trackEvent("Dialog", "Type", getClass().getSimpleName());

		setResizable(false);
		setMinimumSize(new Dimension(450, 370));
		setPreferredSize(new Dimension(450, 370));
		setLocationRelativeTo(MainFrame.SINGLETON);
		initGui();
		addData();

		getRootPane().registerKeyboardAction(new ActionListener()
		{
			@Override
			public void actionPerformed(@Nullable ActionEvent e)
			{
				setVisible(false);
				dispose();
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	private void addData()
	{
		if(Observatories.getObservatories().isEmpty())
			Observatories.addUpdateListener(new Runnable()
			{
				@Override
				public void run()
				{
					if(isVisible())
						addData();
				}
			});
		
		cmbbxObservatory.setEnabled(true);
		
		for (Observatories.Observatory observatory : Observatories.getObservatories())
			cmbbxObservatory.addItem(observatory);
		
		cmbbxObservatory.addItemListener(new ItemListener()
		{
			@Override
			public void itemStateChanged(@Nullable ItemEvent e)
			{
				if(e==null)
					return;
				
				Observatories.Observatory observatory = ((Observatories.Observatory) e.getItem());
				lblFilter.setText("");
				lblFilter1.setText("");
				lblFilter2.setText("");
				lblFilter.setVisible(true);
				lblFilter1.setVisible(true);
				lblFilter2.setVisible(true);
				cmbbxFilter.setVisible(true);
				cmbbxFilter1.setVisible(true);
				cmbbxFilter2.setVisible(true);
				switch (observatory.getUiLabels().size())
				{
					case 0:
						lblFilter.setVisible(false);
						cmbbxFilter.setVisible(false);
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
				try
				{
					lblFilter.setText(observatory.getUiLabels().get(0));
					lblFilter1.setText(observatory.getUiLabels().get(1));
					lblFilter2.setText(observatory.getUiLabels().get(2));
				}
				catch (Exception e2)
				{
					//fill UI on a best-effort basis
				}
				
				cmbbxFilter.removeAllItems();
				cmbbxFilter1.removeAllItems();
				cmbbxFilter2.removeAllItems();
				for (Observatories.Filter instrument : observatory.getInstruments())
					cmbbxFilter.addItem(instrument);
			}
		});

		cmbbxFilter.addItemListener(new ItemListener()
		{
			@Override
			public void itemStateChanged(@Nullable ItemEvent e)
			{
				if(e==null)
					return;
				
				cmbbxFilter1.removeAllItems();
				cmbbxFilter2.removeAllItems();
				for (Observatories.Filter filter : ((Observatories.Filter) e.getItem()).getFilters())
					cmbbxFilter1.addItem(filter);

				Observatories.Filter filter = (Observatories.Filter) cmbbxFilter2.getSelectedItem();
				if (filter == null)
					filter = (Observatories.Filter) cmbbxFilter1.getSelectedItem();

				if (filter != null && filter.getStart() != null)
				{
					datePickerStartDate.setToolTip("Data available after " + filter.getStart());
					datePickerEndDate.setToolTip("Data available before " + filter.getEnd());
				}
			}
		});

		cmbbxFilter1.addItemListener(new ItemListener()
		{
			@Override
			public void itemStateChanged(@Nullable ItemEvent e)
			{
				if(e==null)
					return;
				
				cmbbxFilter2.removeAllItems();
				for (Observatories.Filter filter : ((Observatories.Filter) e.getItem()).getFilters())
					cmbbxFilter2.addItem(filter);
				
				Observatories.Filter filter = (Observatories.Filter) cmbbxFilter2.getSelectedItem();
				if (filter == null)
					filter = (Observatories.Filter) cmbbxFilter1.getSelectedItem();
				
				if (filter != null && filter.getStart() != null)
				{
					datePickerStartDate.setToolTip("Data available after " + filter.getStart());
					datePickerEndDate.setToolTip("Data available before " + filter.getEnd());
				}
				else
				{
					datePickerStartDate.setToolTip(null);
					datePickerEndDate.setToolTip(null);
				}
			}
		});
		if (cmbbxObservatory.getItemCount() > 0)
		{
			cmbbxObservatory.setSelectedIndex(1);
			cmbbxObservatory.setSelectedIndex(0);
		}
		
		
		int sourceId=Settings.getInt(Settings.IntKey.ADDLAYER_LAST_SOURCEID);
		for(Observatory o:Observatories.getObservatories())
			for(Filter f:o.getInstruments())
			{
				if(f.sourceId==sourceId)
				{
					cmbbxObservatory.setSelectedItem(o);
					cmbbxFilter.setSelectedItem(f);
					return;
				}
				
				for(Filter f1:f.getFilters())
				{
					if(f1.sourceId==sourceId)
					{
						cmbbxObservatory.setSelectedItem(o);
						cmbbxFilter.setSelectedItem(f);
						cmbbxFilter1.setSelectedItem(f1);
						return;
					}
					
					for(Filter f2:f1.getFilters())
						if(f2.sourceId==sourceId)
						{
							cmbbxObservatory.setSelectedItem(o);
							cmbbxFilter.setSelectedItem(f);
							cmbbxFilter1.setSelectedItem(f1);
							cmbbxFilter2.setSelectedItem(f2);
							return;
						}
				}
			}
	}

	private void initGui()
	{
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		//tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		contentPanel.setLayout(new BorderLayout());
		//contentPanel.add(tabbedPane, BorderLayout.CENTER);
		layerPanel = new JPanel();
		initLayerGui(layerPanel);
		contentPanel.add(layerPanel, BorderLayout.CENTER);
	}

	private void initLayerGui(JPanel contentPanel)
	{
		GridBagLayout gbl_layerPanel = new GridBagLayout();
		gbl_layerPanel.columnWidths = new int[]{135, 0, 100, 0, 0};
		gbl_layerPanel.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
		gbl_layerPanel.columnWeights = new double[]{0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE};
		gbl_layerPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		layerPanel.setLayout(gbl_layerPanel);
		
		datePickerStartDate = new DatePicker(lastStart, this);
		GridBagConstraints gbc_datePickerStartDate = new GridBagConstraints();
		gbc_datePickerStartDate.anchor = GridBagConstraints.NORTH;
		gbc_datePickerStartDate.fill = GridBagConstraints.HORIZONTAL;
		gbc_datePickerStartDate.insets = new Insets(0, 0, 5, 0);
		gbc_datePickerStartDate.gridwidth = 4;
		gbc_datePickerStartDate.gridx = 0;
		gbc_datePickerStartDate.gridy = 0;
		contentPanel.add(datePickerStartDate, gbc_datePickerStartDate);
		datePickerEndDate = new DatePicker(lastEnd, this);
		GridBagConstraints gbc_datePickerEndDate = new GridBagConstraints();
		gbc_datePickerEndDate.anchor = GridBagConstraints.NORTH;
		gbc_datePickerEndDate.fill = GridBagConstraints.HORIZONTAL;
		gbc_datePickerEndDate.insets = new Insets(0, 0, 5, 0);
		gbc_datePickerEndDate.gridwidth = 4;
		gbc_datePickerEndDate.gridx = 0;
		gbc_datePickerEndDate.gridy = 1;
		contentPanel.add(datePickerEndDate, gbc_datePickerEndDate);
		JLabel lblCadence = new JLabel("Cadence");
		GridBagConstraints gbc_lblCadence = new GridBagConstraints();
		gbc_lblCadence.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblCadence.insets = new Insets(0, 0, 5, 5);
		gbc_lblCadence.gridx = 0;
		gbc_lblCadence.gridy = 2;
		contentPanel.add(lblCadence, gbc_lblCadence);
		cadence = new JSpinner();
		cadence.setValue(120);
		
		
		((DefaultEditor)cadence.getEditor()).getTextField().addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusGained(@Nullable FocusEvent _e)
			{
				//required to call as a callback because
				//of timing-issues in swing
				SwingUtilities.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						((DefaultEditor)cadence.getEditor()).getTextField().selectAll();
					}
				});
			}
		});
		
		
		GridBagConstraints gbc_cadence = new GridBagConstraints();
		gbc_cadence.fill = GridBagConstraints.HORIZONTAL;
		gbc_cadence.insets = new Insets(0, 0, 5, 5);
		gbc_cadence.gridx = 2;
		gbc_cadence.gridy = 2;
		contentPanel.add(cadence, gbc_cadence);
		cmbbxTimeSteps = new JComboBox<>();
		cmbbxTimeSteps.setModel(new DefaultComboBoxModel<>(TimeSteps.values()));
		
		cmbbxTimeSteps.setSelectedItem(TimeSteps.MIN);
		
		GridBagConstraints gbc_cmbbxTimeSteps = new GridBagConstraints();
		gbc_cmbbxTimeSteps.fill = GridBagConstraints.HORIZONTAL;
		gbc_cmbbxTimeSteps.anchor = GridBagConstraints.NORTH;
		gbc_cmbbxTimeSteps.insets = new Insets(0, 0, 5, 0);
		gbc_cmbbxTimeSteps.gridx = 3;
		gbc_cmbbxTimeSteps.gridy = 2;
		contentPanel.add(cmbbxTimeSteps, gbc_cmbbxTimeSteps);
		JSeparator separator = new JSeparator();
		GridBagConstraints gbc_separator = new GridBagConstraints();
		gbc_separator.anchor = GridBagConstraints.NORTH;
		gbc_separator.fill = GridBagConstraints.HORIZONTAL;
		gbc_separator.insets = new Insets(0, 0, 5, 0);
		gbc_separator.gridwidth = 4;
		gbc_separator.gridx = 0;
		gbc_separator.gridy = 3;
		contentPanel.add(separator, gbc_separator);
		JLabel lblObservatory = new JLabel("Observatory");
		GridBagConstraints gbc_lblObservatory = new GridBagConstraints();
		gbc_lblObservatory.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblObservatory.insets = new Insets(0, 0, 5, 5);
		gbc_lblObservatory.gridx = 0;
		gbc_lblObservatory.gridy = 4;
		contentPanel.add(lblObservatory, gbc_lblObservatory);
		cmbbxObservatory = new JComboBox<>();
		GridBagConstraints gbc_cmbbxObservatory = new GridBagConstraints();
		gbc_cmbbxObservatory.gridwidth = 3;
		gbc_cmbbxObservatory.anchor = GridBagConstraints.NORTH;
		gbc_cmbbxObservatory.fill = GridBagConstraints.HORIZONTAL;
		gbc_cmbbxObservatory.insets = new Insets(0, 0, 5, 0);
		gbc_cmbbxObservatory.gridx = 1;
		gbc_cmbbxObservatory.gridy = 4;
		contentPanel.add(cmbbxObservatory, gbc_cmbbxObservatory);
		lblFilter = new JLabel("Instrument");
		GridBagConstraints gbc_lblFilter = new GridBagConstraints();
		gbc_lblFilter.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblFilter.insets = new Insets(0, 0, 5, 5);
		gbc_lblFilter.gridx = 0;
		gbc_lblFilter.gridy = 5;
		contentPanel.add(lblFilter, gbc_lblFilter);
		cmbbxFilter = new JComboBox<>();
		GridBagConstraints gbc_cmbbxFilter = new GridBagConstraints();
		gbc_cmbbxFilter.gridwidth = 3;
		gbc_cmbbxFilter.anchor = GridBagConstraints.NORTH;
		gbc_cmbbxFilter.fill = GridBagConstraints.HORIZONTAL;
		gbc_cmbbxFilter.insets = new Insets(0, 0, 5, 0);
		gbc_cmbbxFilter.gridx = 1;
		gbc_cmbbxFilter.gridy = 5;
		contentPanel.add(cmbbxFilter, gbc_cmbbxFilter);
		lblFilter1 = new JLabel("");
		GridBagConstraints gbc_lblFilter1 = new GridBagConstraints();
		gbc_lblFilter1.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblFilter1.insets = new Insets(0, 0, 5, 5);
		gbc_lblFilter1.gridx = 0;
		gbc_lblFilter1.gridy = 6;
		contentPanel.add(lblFilter1, gbc_lblFilter1);
		cmbbxFilter1 = new JComboBox<>();
		GridBagConstraints gbc_cmbbxFilter1 = new GridBagConstraints();
		gbc_cmbbxFilter1.gridwidth = 3;
		gbc_cmbbxFilter1.anchor = GridBagConstraints.NORTH;
		gbc_cmbbxFilter1.fill = GridBagConstraints.HORIZONTAL;
		gbc_cmbbxFilter1.insets = new Insets(0, 0, 5, 0);
		gbc_cmbbxFilter1.gridx = 1;
		gbc_cmbbxFilter1.gridy = 6;
		contentPanel.add(cmbbxFilter1, gbc_cmbbxFilter1);
		lblFilter2 = new JLabel("");
		GridBagConstraints gbc_lblFilter2 = new GridBagConstraints();
		gbc_lblFilter2.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblFilter2.insets = new Insets(0, 0, 0, 5);
		gbc_lblFilter2.gridx = 0;
		gbc_lblFilter2.gridy = 7;
		contentPanel.add(lblFilter2, gbc_lblFilter2);
		cmbbxFilter2 = new JComboBox<>();
		GridBagConstraints gbc_cmbbxFilter2 = new GridBagConstraints();
		gbc_cmbbxFilter2.gridwidth = 3;
		gbc_cmbbxFilter2.anchor = GridBagConstraints.NORTH;
		gbc_cmbbxFilter2.fill = GridBagConstraints.HORIZONTAL;
		gbc_cmbbxFilter2.gridx = 1;
		gbc_cmbbxFilter2.gridy = 7;
		contentPanel.add(cmbbxFilter2, gbc_cmbbxFilter2);

		JPanel buttonPane = new JPanel();
		buttonPane.setBorder(new EmptyBorder(0, 10, 10, 10));
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);
		
		panel = new JPanel();
		buttonPane.add(panel);
				panel.setLayout(new GridLayout(0, 2, 15, 0));
		
				JButton okButton = new JButton("OK");
				panel.add(okButton);
				okButton.setActionCommand("OK");
				getRootPane().setDefaultButton(okButton);
				okButton.addActionListener(new ActionListener()
				{
					@Override
					public void actionPerformed(@Nullable ActionEvent e)
					{
						Observatories.Filter filter = (Observatories.Filter) cmbbxFilter2.getSelectedItem();
						if (filter == null)
							filter = (Observatories.Filter) cmbbxFilter1.getSelectedItem();
						
						if (filter == null)
							filter = (Observatories.Filter) cmbbxFilter.getSelectedItem();

						if (filter != null)
						{
							int cadence = (int) AddLayerDialog.this.cadence.getValue()
									* ((TimeSteps) cmbbxTimeSteps.getSelectedItem()).factor;
							cadence = Math.max(cadence, 1);
							
							KakaduLayer newLayer=new KakaduLayer(
									filter.sourceId,
									datePickerStartDate.getDateTime(),
									datePickerEndDate.getDateTime(),
									cadence, filter.getNickname());
							Layers.addLayer(newLayer);

							//FIXME: camera rotation not correct for COR1 layer
							newLayer.animateCameraToFacePlane = true;
							
							Settings.setInt(Settings.IntKey.ADDLAYER_LAST_SOURCEID, filter.sourceId);
							lastStart = datePickerStartDate.getDateTime();
							lastEnd = datePickerEndDate.getDateTime();
							
							setVisible(false);
							dispose();
						}
					}
				});
				
				JButton cancelButton = new JButton("Cancel");
				panel.add(cancelButton);
				cancelButton.setActionCommand("Cancel");
				cancelButton.addActionListener(new ActionListener()
				{
					@Override
					public void actionPerformed(@Nullable ActionEvent e)
					{
						setVisible(false);
					}
				});
				
				DialogTools.setDefaultButtons(okButton,cancelButton);
		
		if(Globals.isOSX())
		{
			buttonPane.remove(okButton);
			buttonPane.remove(cancelButton);
			
			buttonPane.add(cancelButton);
			buttonPane.add(okButton);
		}
	}
}
