package org.helioviewer.jhv.gui.components.newComponents;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.helioviewer.jhv.gui.components.calendar.JHVCalendar;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class AddLayerPanel extends JDialog{

	private final JPanel contentPanel = new JPanel();
	private SimpleDateFormat simpleDateFormat;
	private JTextField txtFieldStart, txtFieldEnd;
	private String lastAcceptedStartTime = "", lastAcceptedEndTime = "";
	
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
				FormFactory.DEFAULT_ROWSPEC,}));

		{
            JHVCalendarDatePicker calendarStartDate = new JHVCalendarDatePicker();
			contentPanel.add(calendarStartDate, "2, 2");
		}
		{
			this.txtFieldStart = new JTextField();
			txtFieldStart.addFocusListener(new FocusAdapter() {	
				@Override
				public void focusLost(FocusEvent e) {
					SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm:ss");
					try {
						txtFieldStart.setText(simpleDateFormat.format(new SimpleDateFormat("HH:mm:ss").parse(txtFieldStart.getText())).toString());
					} catch (Exception e1) {
						try {
							txtFieldStart.setText(simpleDateFormat.format(new SimpleDateFormat("HH:mm").parse(txtFieldStart.getText())).toString());
						} catch (Exception e2) {
							try {
								txtFieldStart.setText(simpleDateFormat.format(new SimpleDateFormat("HH").parse(txtFieldStart.getText())).toString());
							} catch (Exception e3) {
								txtFieldStart.setText(lastAcceptedStartTime);
							}
						}
					}
					lastAcceptedStartTime = txtFieldStart.getText();
				}				
			});contentPanel.add(txtFieldStart, "4, 2, 3, 1, fill, default");
		}
		{
            JHVCalendarDatePicker calendarStartDate = new JHVCalendarDatePicker();
			contentPanel.add(calendarStartDate, "2, 4");
		}
		{
			this.txtFieldEnd = new JTextField();
			txtFieldEnd.addFocusListener(new FocusAdapter() {	
				@Override
				public void focusLost(FocusEvent e) {
					SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm:ss");
					try {
						txtFieldEnd.setText(simpleDateFormat.format(new SimpleDateFormat("HH:mm:ss").parse(txtFieldEnd.getText())).toString());
					} catch (Exception e1) {
						try {
							txtFieldEnd.setText(simpleDateFormat.format(new SimpleDateFormat("HH:mm").parse(txtFieldEnd.getText())).toString());
						} catch (Exception e2) {
							try {
								txtFieldEnd.setText(simpleDateFormat.format(new SimpleDateFormat("HH").parse(txtFieldEnd.getText())).toString());
							} catch (Exception e3) {
								txtFieldEnd.setText(lastAcceptedEndTime);
							}
						}
					}
					lastAcceptedEndTime = txtFieldEnd.getText();
				}				
			});
			contentPanel.add(txtFieldEnd, "4, 4, 3, 1, fill, default");
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
			JLabel lblNewLabel_1 = new JLabel("New label");
			contentPanel.add(lblNewLabel_1, "2, 10");
		}
		{
			JComboBox comboBox = new JComboBox();
			contentPanel.add(comboBox, "6, 10, fill, default");
		}
		{
			JLabel lblNewLabel_2 = new JLabel("New label");
			contentPanel.add(lblNewLabel_2, "2, 12");
		}
		{
			JComboBox comboBox = new JComboBox();
			contentPanel.add(comboBox, "6, 12, fill, default");
		}
		{
			JLabel lblNewLabel_3 = new JLabel("New label");
			contentPanel.add(lblNewLabel_3, "2, 14");
		}
		{
			JComboBox comboBox = new JComboBox();
			contentPanel.add(comboBox, "6, 14, fill, default");
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

}
