package org.helioviewer.jhv.gui.components.newComponents;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

public class NewDatePicker extends JPanel{

	/**
	 * 
	 */
	private static final long serialVersionUID = 3241360217403676930L;
	
	private enum KEY_MODE { UP, DOWN, NONE};
	private boolean popupVisibility = false;
	private KEY_MODE keyMode = KEY_MODE.NONE;
	private int factor = 0;
	private int counter = 0;
	private JButton btnDatePicker;
	private JTextField txtFieldDate, txtFieldTime;
	private NewDatePickerPopup newDatePickerPopup;
	private LocalDateTime dateTime;
	private final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
	private final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private final long MAX_TIME_STEPS = 3600 * 24 * 30; 
	public NewDatePicker(LocalDateTime dateTime) {
		this.dateTime = dateTime;
		newDatePickerPopup = new NewDatePickerPopup(this);
		initGUI();
		initData();
	}
	
	private void initData(){
		txtFieldTime.setText(this.dateTime.format(TIME_FORMAT));
		txtFieldDate.setText(this.dateTime.format(DATE_FORMAT));
	}
	
	
	private void initGUI(){
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0, 0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0};
		gridBagLayout.columnWeights = new double[]{1.0, 0.0, 1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);
		
		txtFieldDate = new JTextField();
		txtFieldDate.addFocusListener(new FocusAdapter() {	
			@Override
			public void focusLost(FocusEvent e) {
				LocalDate localdate = null;
				try {
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/D/yyyy");
					localdate = LocalDate.parse(txtFieldDate.getText(), formatter);
				} catch (Exception e1) {
					try {
						DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-M-d");
						localdate = LocalDate.parse(txtFieldDate.getText(), formatter);
					} catch (Exception e2) {
						try {
							DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d.M.yyyy");
							localdate = LocalDate.parse(txtFieldDate.getText(), formatter);
						} catch (Exception e3) {
						}
					}
				}
				
				if (localdate != null){
					dateTime = localdate.atTime(dateTime.toLocalTime());
					newDatePickerPopup.setCurrentDate(localdate);
				}
				txtFieldDate.setText(dateTime.format(DATE_FORMAT));
				txtFieldTime.setText(dateTime.format(TIME_FORMAT));
			}				
		});
		
		GridBagConstraints gbcDate = new GridBagConstraints();
		gbcDate.gridwidth = 2;
		gbcDate.insets = new Insets(0, 0, 0, 22);
		gbcDate.fill = GridBagConstraints.HORIZONTAL;
		gbcDate.gridx = 0;
		gbcDate.gridy = 0;
		add(txtFieldDate, gbcDate);
		txtFieldDate.setColumns(10);
		
		btnDatePicker = new JButton(IconBank.getIcon(JHVIcon.CALENDER));
		btnDatePicker.setPreferredSize(new Dimension(22, 22));
		btnDatePicker.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (popupVisibility){
					newDatePickerPopup.setVisible(false);
					popupVisibility = false;
				}
				else{
					int x = txtFieldDate.getLocationOnScreen().x;
		        	int y = txtFieldDate.getLocationOnScreen().y + txtFieldDate.getSize().height - 4;
		        	newDatePickerPopup.setLocation(x, y);
		        	newDatePickerPopup.setVisible(true);
		        	newDatePickerPopup.requestFocus();
		        	popupVisibility = true;
				}
			}
		});
		GridBagConstraints gbcBtnDatePicker = new GridBagConstraints();
		gbcBtnDatePicker.insets = new Insets(0, 0, 0, 5);
		gbcBtnDatePicker.gridx = 1;
		gbcBtnDatePicker.gridy = 0;
		add(btnDatePicker, gbcBtnDatePicker);
		
		txtFieldTime = new JTextField();
		txtFieldTime.addFocusListener(new FocusAdapter() {	
			@Override
			public void focusLost(FocusEvent e) {
				LocalTime localTime = null;
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:m:s");
				try {
					localTime = LocalTime.parse(txtFieldTime.getText(), formatter);
				} catch (Exception e1) {
					try {
						formatter = DateTimeFormatter.ofPattern("H:m");
						localTime = LocalTime.parse(txtFieldTime.getText(), formatter);
					} catch (Exception e2) {
						try {
							formatter = DateTimeFormatter.ofPattern("H");
							localTime = LocalTime.parse(txtFieldTime.getText(), formatter);
						} catch (Exception e3) {
						}
					}
				}
				if (localTime != null)
					dateTime = localTime.atDate(dateTime.toLocalDate());
				txtFieldTime.setText(dateTime.toLocalTime().format(TIME_FORMAT));
			}				
		});
		txtFieldTime.addKeyListener(new KeyAdapter() {
			
			@Override
			public void keyReleased(KeyEvent e) {
				factor = 0;
				keyMode = KEY_MODE.NONE;
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
				long time = (long) Math.pow(2, factor);
				time = time > MAX_TIME_STEPS ? MAX_TIME_STEPS : time;
				if (++counter > 30) {
					factor++;
					counter = 0;
				}
				if (e.getKeyCode() == KeyEvent.VK_UP && keyMode != KEY_MODE.DOWN){
					if (keyMode == KEY_MODE.UP){
						dateTime = dateTime.plusSeconds(time);
					}
					else {
						keyMode = KEY_MODE.UP;
						factor = 0;
						counter = 0;
					}
				}
				else if (e.getKeyCode() == KeyEvent.VK_DOWN){
					if (keyMode == KEY_MODE.DOWN){
						dateTime = dateTime.minusSeconds(time);
					}
					else {
						keyMode = KEY_MODE.DOWN;
						factor = 0;
						counter = 0;
					}
				}

				if (keyMode != KEY_MODE.NONE){
					txtFieldDate.setText(dateTime.format(DATE_FORMAT));
					txtFieldTime.setText(dateTime.format(TIME_FORMAT));
				}
				
			}
		});
		GridBagConstraints gbcMonth = new GridBagConstraints();
		gbcMonth.fill = GridBagConstraints.HORIZONTAL;
		gbcMonth.gridx = 2;
		gbcMonth.gridy = 0;
		add(txtFieldTime, gbcMonth);
		txtFieldTime.setColumns(10);
	}
	
	public void setDateTime(LocalDateTime dateTime){
		this.dateTime = dateTime;
	}

	public void updateDate(LocalDate newDate) {
		this.dateTime = newDate.atTime(this.dateTime.toLocalTime());
		this.txtFieldDate.setText(dateTime.format(DATE_FORMAT));
		newDatePickerPopup.setVisible(false);
		popupVisibility = false;
	}

	public void hidePopup() {
		newDatePickerPopup.setVisible(false);
		popupVisibility = false;				
		btnDatePicker.setEnabled(true);
	}
	
	public LocalDateTime getDateTime(){
		return dateTime;
	}
	
}
