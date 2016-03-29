package org.helioviewer.jhv.gui.dialogs.calender;

import java.awt.Color;
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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

public class DatePicker extends JPanel
{
	private enum KEY_MODE
	{
		UP, DOWN, NONE
	}
	
	private List<ActionListener> changeListeners = new ArrayList<>();
	
	private boolean popupVisibile = false;
	private KEY_MODE keyMode = KEY_MODE.NONE;
	private int factor = 0;
	private int counter = 0;
	private JButton btnDatePicker;
	private JTextField txtFieldDate, txtFieldTime;
	private DatePickerPopup newDatePickerPopup;
	private LocalDateTime dateTime;
	private boolean containsValidDateTime = false;
	
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final long MAX_TIME_STEPS = 3600 * 24 * 30;

	public DatePicker(LocalDateTime _dateTime, JDialog _dialog)
	{
		newDatePickerPopup = new DatePickerPopup(this, _dialog);
		initGUI();
		
		setDateTime(_dateTime);
	}
	
	public void dispose()
	{
		newDatePickerPopup.dispose();
	}
	
	public void addChangeListener(ActionListener _al)
	{
		changeListeners.add(_al);
	}

	public void removeChangeListener(ActionListener _al)
	{
		changeListeners.remove(_al);
	}

	private void initGUI()
	{
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0, 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, 0.0, 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
		setLayout(gridBagLayout);

		txtFieldDate = new JTextField();
		txtFieldDate.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusGained(@Nullable FocusEvent _e)
			{
				//required to call as a callback because
				//of timing-issues in swing
				SwingUtilities.invokeLater(() -> txtFieldDate.selectAll());
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
		btnDatePicker.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(@Nullable ActionEvent e)
			{
				if (popupVisibile)
				{
					newDatePickerPopup.setVisible(false);
					popupVisibile = false;
				}
				else
				{
					int x = txtFieldDate.getLocationOnScreen().x;
					int y = txtFieldDate.getLocationOnScreen().y + txtFieldDate.getSize().height - 4;
					newDatePickerPopup.setLocation(x, y);
					newDatePickerPopup.setVisible(true);
					newDatePickerPopup.requestFocus();
					popupVisibile = true;
				}
			}
		});
		GridBagConstraints gbcBtnDatePicker = new GridBagConstraints();
		gbcBtnDatePicker.insets = new Insets(0, 0, 0, 5);
		gbcBtnDatePicker.gridx = 1;
		gbcBtnDatePicker.gridy = 0;
		add(btnDatePicker, gbcBtnDatePicker);

		txtFieldTime = new JTextField();
		txtFieldTime.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusGained(@Nullable FocusEvent _e)
			{
				//required to call as a callback because
				//of timing-issues in swing
				SwingUtilities.invokeLater(() -> txtFieldTime.selectAll());
			}
		});
		txtFieldTime.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyReleased(@Nullable KeyEvent e)
			{
				factor = 0;
				keyMode = KEY_MODE.NONE;
			}

			@Override
			public void keyPressed(@Nullable KeyEvent e)
			{
				if(e==null)
					return;
				
				long time = (long) Math.pow(2, factor);
				time = time > MAX_TIME_STEPS ? MAX_TIME_STEPS : time;
				if (++counter > 30)
				{
					factor++;
					counter = 0;
				}
				if (e.getKeyCode() == KeyEvent.VK_UP && keyMode != KEY_MODE.DOWN)
				{
					if (keyMode == KEY_MODE.UP)
						dateTime = dateTime.plusSeconds(time);
					else
					{
						keyMode = KEY_MODE.UP;
						factor = 0;
						counter = 0;
					}
				}
				else if (e.getKeyCode() == KeyEvent.VK_DOWN)
				{
					if (keyMode == KEY_MODE.DOWN)
						dateTime = dateTime.minusSeconds(time);
					else
					{
						keyMode = KEY_MODE.DOWN;
						factor = 0;
						counter = 0;
					}
				}

				if (keyMode != KEY_MODE.NONE)
				{
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
		
		
		
		txtFieldDate.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void removeUpdate(@Nullable DocumentEvent e)
			{
				parseDateTime();
			}
			
			@Override
			public void insertUpdate(@Nullable DocumentEvent e)
			{
				parseDateTime();
			}
			
			@Override
			public void changedUpdate(@Nullable DocumentEvent e)
			{
				parseDateTime();
			}
		});
		
		txtFieldTime.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void removeUpdate(@Nullable DocumentEvent e)
			{
				parseDateTime();
			}
			
			@Override
			public void insertUpdate(@Nullable DocumentEvent e)
			{
				parseDateTime();
			}
			
			@Override
			public void changedUpdate(@Nullable DocumentEvent e)
			{
				parseDateTime();
			}
		});
	}

	public void setDate(LocalDate _newDate)
	{
		dateTime = _newDate.atTime(dateTime.toLocalTime());
		txtFieldDate.setText(dateTime.format(DATE_FORMAT));
		newDatePickerPopup.setVisible(false);
		popupVisibile = false;
		parseDateTime();
	}

	public void setDateTime(LocalDateTime _newDateTime)
	{
		dateTime = _newDateTime;
		txtFieldTime.setText(dateTime.format(TIME_FORMAT));
		
		dateTime = _newDateTime;
		txtFieldDate.setText(dateTime.format(DATE_FORMAT));

		newDatePickerPopup.setVisible(false);
		popupVisibile = false;
		parseDateTime();
	}

	void hidePopup()
	{
		newDatePickerPopup.setVisible(false);
		popupVisibile = false;
		btnDatePicker.setEnabled(true);
	}

	public LocalDateTime getDateTime()
	{
		parseDateTime();
		return dateTime;
	}

	private void parseDateTime()
	{
		LocalDateTime previousDateTime = dateTime;
		boolean prevIsValid = containsValidDateTime;
		containsValidDateTime=true;

		LocalDate localdate = null;
		try
		{
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/D/yyyy");
			localdate = LocalDate.parse(txtFieldDate.getText(), formatter);
		}
		catch (DateTimeParseException e1)
		{
			try
			{
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-M-d");
				localdate = LocalDate.parse(txtFieldDate.getText(), formatter);
			}
			catch (DateTimeParseException e2)
			{
				try
				{
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d.M.yyyy");
					localdate = LocalDate.parse(txtFieldDate.getText(), formatter);
				}
				catch (DateTimeParseException e3)
				{
				}
			}
		}
		
		LocalTime localtime = null;
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:m:s");
		try
		{
			localtime = LocalTime.parse(txtFieldTime.getText(), formatter);
		}
		catch (DateTimeParseException e1)
		{
			try
			{
				formatter = DateTimeFormatter.ofPattern("H:m");
				localtime = LocalTime.parse(txtFieldTime.getText(), formatter);
			}
			catch (DateTimeParseException e2)
			{
				try
				{
					formatter = DateTimeFormatter.ofPattern("H");
					localtime = LocalTime.parse(txtFieldTime.getText(), formatter);
				}
				catch (DateTimeParseException e3)
				{
				}
			}
		}

		if(localdate != null)
		{
			dateTime = localdate.atTime(dateTime.toLocalTime());
			txtFieldDate.setForeground(null);
			newDatePickerPopup.setCurrentDate(localdate);
		}
		else
		{
			txtFieldDate.setForeground(Color.RED);
			containsValidDateTime=false;
		}
		
		if(localtime != null)
		{
			dateTime = localtime.atDate(dateTime.toLocalDate());
			txtFieldTime.setForeground(Color.BLACK);
		}
		else
		{
			txtFieldTime.setForeground(Color.RED);
			containsValidDateTime=false;
		}
		
		if(containsValidDateTime!=prevIsValid || (previousDateTime==null && dateTime!=null) || (previousDateTime!=null && dateTime==null) || (previousDateTime!=null && !previousDateTime.equals(dateTime)))
			for(ActionListener al:changeListeners)
				al.actionPerformed(new ActionEvent(this, 0, null));
	}

	public void setToolTip(@Nullable String s)
	{
		txtFieldDate.setToolTipText(s);
		txtFieldTime.setToolTipText(s);
	}
	
	public boolean containsValidDateTime()
	{
		return containsValidDateTime;
	}
}
