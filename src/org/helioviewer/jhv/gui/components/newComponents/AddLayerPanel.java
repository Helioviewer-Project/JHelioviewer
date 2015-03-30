package org.helioviewer.jhv.gui.components.newComponents;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.DatePicker;
import javafx.scene.paint.Color;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;
import org.helioviewer.jhv.gui.components.newComponents.InstrumentModel.Filter;
import org.helioviewer.jhv.gui.components.newComponents.InstrumentModel.Instrument;
import org.helioviewer.jhv.gui.components.newComponents.InstrumentModel.Observatory;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class AddLayerPanel extends JDialog{

	private final JPanel contentPanel = new JPanel();
	private SimpleDateFormat simpleDateFormat;
	private JTextField txtFieldStartTime, txtFieldEndTime;
	private String lastAcceptedStartTime = "", lastAcceptedEndTime = "";
	
	private JHVCalendarDatePicker calendarStartDate, calendarEndDate;
	
	private JLabel lblFilter1, lblFilter2, lblInstrument;
	private JComboBox<InstrumentModel.Observatory> cmbbxObservatory;
	private JComboBox<InstrumentModel.Instrument> cmbbxInstrument;
	private JComboBox<InstrumentModel.Filter> cmbbxFilter1, cmbbxFilter2;
	
	public enum TIME_STEPS{
		SEC("sec"), MIN("min"), HOUR("hour"), DAY("day"), GET_ALL("get all");
		
		private String name;
		TIME_STEPS(String name){
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
		setBounds(100, 100, 450, 300);
		initGui();
		addData();
	}
	
	public void addData(){
		InstrumentModel instrumentModel = InstrumentModel.singelton;
		LocalDateTime endDateTime = LocalDateTime.now();
		LocalDateTime startDateTime = endDateTime.minusDays(1);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
		txtFieldEndTime.setText(endDateTime.format(formatter));
		txtFieldStartTime.setText(startDateTime.format(formatter));
		
		for (InstrumentModel.Observatory observatory : instrumentModel.getObservatories()){
			cmbbxObservatory.addItem(observatory);
		}
		
		cmbbxObservatory.addItemListener(new ItemListener() {	
			@Override
			public void itemStateChanged(ItemEvent e) {
				InstrumentModel.Observatory observatory = ((InstrumentModel.Observatory)e.getItem());
				lblInstrument.setText("");
				lblFilter1.setText("");
				lblFilter2.setText("");
				System.out.println(observatory.getUiLabels().size());
				lblInstrument.setEnabled(true);
				lblFilter1.setEnabled(true);
				lblFilter2.setEnabled(true);
				cmbbxInstrument.setEnabled(true);
				cmbbxFilter1.setEnabled(true);
				cmbbxFilter2.setEnabled(true);
				switch (observatory.getUiLabels().size()) {
				case 0:
					lblInstrument.setEnabled(false);
					cmbbxInstrument.setEnabled(false);
				case 1:
					lblFilter1.setEnabled(false);
					cmbbxFilter1.setEnabled(false);
				case 2:					
					lblFilter2.setEnabled(false);
					cmbbxFilter2.setEnabled(false);
					break;

				default:
					break;
				}
				try {
					lblInstrument.setText(observatory.getUiLabels().get(0));
					try {
						lblFilter1.setText(observatory.getUiLabels().get(1));
						try {
							lblFilter2.setText(observatory.getUiLabels().get(2));
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
				for (InstrumentModel.Instrument instrument : observatory.getInstruments()){
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
				for (InstrumentModel.Filter filter : ((InstrumentModel.Instrument)e.getItem()).getFilters()){
					cmbbxFilter1.addItem(filter);
				}
			}
		});
		
		cmbbxFilter1.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				cmbbxFilter2.removeAllItems();
				for (InstrumentModel.Filter filter : ((InstrumentModel.Filter)e.getItem()).getFilters()){
					cmbbxFilter2.addItem(filter);
				}
			}
		});
	}
	
	private void initGui(){
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),},
			new RowSpec[] {
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
				FormFactory.DEFAULT_ROWSPEC,}));

		{
            calendarStartDate = new JHVCalendarDatePicker();
            final JFXPanel fxPanel = new JFXPanel();
            contentPanel.add(fxPanel, "2, 2");
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    initFX(fxPanel);
                }
           });
		}
		{
			this.txtFieldStartTime = new JTextField();
			txtFieldStartTime.addFocusListener(new FocusAdapter() {	
				@Override
				public void focusLost(FocusEvent e) {
					SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm:ss");
					try {
						txtFieldStartTime.setText(simpleDateFormat.format(new SimpleDateFormat("HH:mm:ss").parse(txtFieldStartTime.getText())).toString());
					} catch (Exception e1) {
						try {
							txtFieldStartTime.setText(simpleDateFormat.format(new SimpleDateFormat("HH:mm").parse(txtFieldStartTime.getText())).toString());
						} catch (Exception e2) {
							try {
								txtFieldStartTime.setText(simpleDateFormat.format(new SimpleDateFormat("HH").parse(txtFieldStartTime.getText())).toString());
							} catch (Exception e3) {
								txtFieldStartTime.setText(lastAcceptedStartTime);
							}
						}
					}
					lastAcceptedStartTime = txtFieldStartTime.getText();
				}				
			});contentPanel.add(txtFieldStartTime, "4, 2, 3, 1, fill, default");
		}
		{
            calendarEndDate = new JHVCalendarDatePicker();
			contentPanel.add(calendarEndDate, "2, 4");
		}
		{
			this.txtFieldEndTime = new JTextField();
			txtFieldEndTime.addFocusListener(new FocusAdapter() {	
				@Override
				public void focusLost(FocusEvent e) {
					SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm:ss");
					try {
						txtFieldEndTime.setText(simpleDateFormat.format(new SimpleDateFormat("HH:mm:ss").parse(txtFieldEndTime.getText())).toString());
					} catch (Exception e1) {
						try {
							txtFieldEndTime.setText(simpleDateFormat.format(new SimpleDateFormat("HH:mm").parse(txtFieldEndTime.getText())).toString());
						} catch (Exception e2) {
							try {
								txtFieldEndTime.setText(simpleDateFormat.format(new SimpleDateFormat("HH").parse(txtFieldEndTime.getText())).toString());
							} catch (Exception e3) {
								txtFieldEndTime.setText(lastAcceptedEndTime);
							}
						}
					}
					lastAcceptedEndTime = txtFieldEndTime.getText();
				}				
			});
			contentPanel.add(txtFieldEndTime, "4, 4, 3, 1, fill, default");
		}
		{
			JLabel lblNewLabel = new JLabel("Time Step");
			contentPanel.add(lblNewLabel, "2, 6");
		}
		{
			JSpinner spinner = new JSpinner();
			contentPanel.add(spinner, "4, 6");
		}
		{
			JComboBox<String> comboBox = new JComboBox<String>();
			for (TIME_STEPS timeStep : TIME_STEPS.values()){
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
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
	}

	private static void initFX(JFXPanel fxPanel) {
        // This method is invoked on the JavaFX thread
        Scene scene = createScene();
        fxPanel.setScene(scene);
    }

    private static Scene createScene() {
        Group  root  =  new  Group();
        Scene  scene  =  new  Scene(root, Color.ALICEBLUE);
        DatePicker datePicker = new DatePicker(LocalDate.now());
        root.getChildren().add(datePicker);

        return (scene);
    }
}
