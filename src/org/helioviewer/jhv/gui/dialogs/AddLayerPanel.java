package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;

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

import org.helioviewer.jhv.Telemetry;
import org.helioviewer.jhv.base.Observatories;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.dialogs.calender.DatePicker;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.plugins.AbstractPlugin;
import org.helioviewer.jhv.plugins.Plugins;
import org.helioviewer.jhv.viewmodel.jp2view.newjpx.KakaduLayer;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

//FIXME: remember last entered dates, times & instruments
public class AddLayerPanel extends JDialog
{
	private final JPanel contentPanel = new JPanel();

	private JLabel lblFilter, lblFilter1, lblFilter2;
	private JComboBox<Observatories.Observatory> cmbbxObservatory;
	private JComboBox<Observatories.Filter> cmbbxFilter, cmbbxFilter1, cmbbxFilter2;
	private JComboBox<TimeSteps> cmbbxTimeSteps;
	private DatePicker datePickerStartDate;
	private DatePicker datePickerEndDate;
	private JSpinner cadence;
	private JComboBox<AbstractPlugin> cmbbxPlugin;
	//private JTabbedPane tabbedPane;
	private JPanel layerPanel;

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

	/**
	 * Create the dialog.
	 */
	public AddLayerPanel()
	{
		super(MainFrame.SINGLETON, "Add Layer", true);
		
    	Telemetry.trackEvent("Dialog", "Type", getClass().getSimpleName());

		this.setResizable(false);
		setMinimumSize(new Dimension(450, 370));
		setPreferredSize(new Dimension(450, 370));
		setLocationRelativeTo(MainFrame.SINGLETON);
		initGui();
		addData();

		getRootPane().registerKeyboardAction(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				setVisible(false);
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		this.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentShown(ComponentEvent e)
			{
				/*if (tabbedPane.getSelectedComponent() == pluginPanel) {
					tabbedPane.setSelectedComponent(layerPanel);
				}
				tabbedPane.setEnabledAt(tabbedPane
						.indexOfComponent(pluginPanel),
						!UltimatePluginInterface.SINGLETON.getInactivePlugins()
								.isEmpty());
								*/
			}
		});
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
			public void itemStateChanged(ItemEvent e)
			{
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
			public void itemStateChanged(ItemEvent e)
			{
				cmbbxFilter1.removeAllItems();
				cmbbxFilter2.removeAllItems();
				for (Observatories.Filter filter : ((Observatories.Filter) e.getItem()).getFilters())
					cmbbxFilter1.addItem(filter);

				Observatories.Filter filter = (Observatories.Filter) cmbbxFilter2.getSelectedItem();
				if (filter == null)
					filter = (Observatories.Filter) cmbbxFilter1.getSelectedItem();

				if (filter != null && filter.getStart() != null)
				{
					datePickerStartDate.setToolTip("Data available after : " + filter.getStart());
					datePickerEndDate.setToolTip("Data available before : " + filter.getEnd());
				}
			}
		});

		cmbbxFilter1.addItemListener(new ItemListener()
		{
			@Override
			public void itemStateChanged(ItemEvent e)
			{
				cmbbxFilter2.removeAllItems();
				for (Observatories.Filter filter : ((Observatories.Filter) e.getItem()).getFilters())
					cmbbxFilter2.addItem(filter);

				Observatories.Filter filter = (Observatories.Filter) cmbbxFilter2.getSelectedItem();
				if (filter == null)
					filter = (Observatories.Filter) cmbbxFilter1.getSelectedItem();
				
				if (filter != null && filter.getStart() != null)
				{
					datePickerStartDate.setToolTip("Data available after : " + filter.getStart());
					datePickerEndDate.setToolTip("Data available before : " + filter.getEnd());
				}
				else
				{
					datePickerStartDate.setToolTip("");
					datePickerEndDate.setToolTip("");
				}
			}
		});
		if (cmbbxObservatory.getItemCount() > 0)
		{
			cmbbxObservatory.setSelectedIndex(1);
			cmbbxObservatory.setSelectedIndex(0);
		}
	}

	private void initGui()
	{
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		//tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		contentPanel.setLayout(new BorderLayout());
		//contentPanel.add(tabbedPane, BorderLayout.CENTER);
		layerPanel = new JPanel();
		initLayerGui(layerPanel);
		contentPanel.add(layerPanel, BorderLayout.CENTER);
		//tabbedPane.addTab("Layers", null, layerPanel, "Add an imagelayer");
		
		//pluginPanel = new JPanel();
		//initPluginGui(pluginPanel);
		//tabbedPane.addTab("Plugins", null, pluginPanel, "Add a plugin");
	}

	private void initLayerGui(JPanel contentPanel)
	{
		contentPanel.setLayout(
				new FormLayout(
						new ColumnSpec[]
						{
							FormFactory.RELATED_GAP_COLSPEC,
							ColumnSpec.decode("default:grow"),
							FormFactory.RELATED_GAP_COLSPEC,
							FormFactory.DEFAULT_COLSPEC,
							FormFactory.RELATED_GAP_COLSPEC,
							ColumnSpec.decode("default:grow")
						},
						new RowSpec[]
						{
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
							FormFactory.DEFAULT_ROWSPEC
						}));
		
		//FIXME: remove minusYears for release, check data availability
		datePickerStartDate = new DatePicker(LocalDateTime.now().minusYears(1).minusDays(1), this);
		contentPanel.add(datePickerStartDate, "2, 2, 5, 1, fill, top");
		datePickerEndDate = new DatePicker(LocalDateTime.now().minusYears(1), this);
		contentPanel.add(datePickerEndDate, "2, 4, 5, 1, fill, top");
		JLabel lblCadence = new JLabel("Time Step");
		contentPanel.add(lblCadence, "2, 6");
		cadence = new JSpinner();
		cadence.setValue(120);
		cadence.setPreferredSize(new Dimension(80, 20));
		
		
		((DefaultEditor)cadence.getEditor()).getTextField().addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusGained(FocusEvent _e)
			{
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
		
		
		contentPanel.add(cadence, "4, 6");
		cmbbxTimeSteps = new JComboBox<TimeSteps>(TimeSteps.values());
		cmbbxTimeSteps.setSelectedItem(TimeSteps.MIN);
		
		contentPanel.add(cmbbxTimeSteps, "6, 6, fill, default");
		JSeparator separator = new JSeparator();
		contentPanel.add(separator, "2, 8, 5, 1");
		JLabel lblObservatory = new JLabel("Observatory");
		contentPanel.add(lblObservatory, "2, 10");
		cmbbxObservatory = new JComboBox<Observatories.Observatory>();
		contentPanel.add(cmbbxObservatory, "6, 10, fill, default");
		lblFilter = new JLabel("Instrument");
		contentPanel.add(lblFilter, "2, 12");
		cmbbxFilter = new JComboBox<Observatories.Filter>();
		contentPanel.add(cmbbxFilter, "6, 12, fill, default");
		lblFilter1 = new JLabel("");
		contentPanel.add(lblFilter1, "2, 14");
		cmbbxFilter1 = new JComboBox<Observatories.Filter>();
		contentPanel.add(cmbbxFilter1, "6, 14, fill, default");
		lblFilter2 = new JLabel("");
		contentPanel.add(lblFilter2, "2, 16");
		cmbbxFilter2 = new JComboBox<Observatories.Filter>();
		contentPanel.add(cmbbxFilter2, "6, 16, fill, default");

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);

		JButton okButton = new JButton("OK");
		okButton.setActionCommand("OK");
		buttonPane.add(okButton);
		getRootPane().setDefaultButton(okButton);
		okButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				//if (tabbedPane.getSelectedComponent() == layerPanel) {
					Observatories.Filter filter = (Observatories.Filter) cmbbxFilter2.getSelectedItem();
					if (filter == null)
						filter = (Observatories.Filter) cmbbxFilter1.getSelectedItem();
					if (filter == null)
						filter = (Observatories.Filter) cmbbxFilter.getSelectedItem();

					if (filter != null)
					{
						int cadence = (int) AddLayerPanel.this.cadence.getValue()
								* ((TimeSteps) cmbbxTimeSteps.getSelectedItem()).factor;
						cadence = Math.max(cadence, 1);
						Layers.addLayer(new KakaduLayer(
								filter.sourceId,
								datePickerStartDate.getDateTime(),
								datePickerEndDate.getDateTime(),
								cadence, filter.getNickname()));
						
						setVisible(false);
					}
				/*}
				else if (tabbedPane.getSelectedComponent() == pluginPanel){
					UltimatePluginInterface.SINGLETON.addPlugin((AbstractPlugin)cmbbxPlugin.getSelectedItem());
				}*/
			}
		});
		
		JButton cancelButton = new JButton("Cancel");
		cancelButton.setActionCommand("Cancel");
		buttonPane.add(cancelButton);
		cancelButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				setVisible(false);
			}
		});
		
		DialogTools.setDefaultButtons(okButton,cancelButton);
	}

	@SuppressWarnings("unused")
	private void initPluginGui(JPanel contentPanel)
	{
		contentPanel.setLayout(new FormLayout(
				new ColumnSpec[]
				{
					FormFactory.RELATED_GAP_COLSPEC,
					FormFactory.DEFAULT_COLSPEC,
					FormFactory.RELATED_GAP_COLSPEC,
					ColumnSpec.decode("default:grow")
				},
				new RowSpec[]
				{
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC
				}));

		JLabel lblName = new JLabel("Plugin");
		contentPanel.add(lblName, "2, 2, right, default");

		cmbbxPlugin = new JComboBox<AbstractPlugin>();
		contentPanel.add(cmbbxPlugin, "4, 2, fill, default");

		contentPanel.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentShown(ComponentEvent e)
			{
				cmbbxPlugin.removeAllItems();
				for (AbstractPlugin plugin : Plugins.SINGLETON.getInactivePlugins())
					cmbbxPlugin.addItem(plugin);

			}
		});

	}
}
