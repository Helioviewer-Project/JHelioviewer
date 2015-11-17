package org.helioviewer.jhv.gui.dialogs.calender;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

import javax.annotation.Nullable;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

class DatePickerPopup extends JDialog
{
	private LocalDate currentDate;
	private CalenderCellRenderer calenderCellRenderer;
	private CalenderTableModel calenderTableModel;
	private DatePicker newDatePicker;
	private String[] columnNames;

	private JLabel lblMonth, lblYear;
	private JTable table;

	@SuppressWarnings("null")
	DatePickerPopup(final DatePicker newDatePicker, JDialog dialog)
	{
		super(dialog);
		this.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowDeactivated(@Nullable WindowEvent e)
			{
				newDatePicker.hidePopup();
			}
		});

		this.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(@Nullable FocusEvent e)
			{
				newDatePicker.hidePopup();
			}
		});

		this.setUndecorated(true);
		this.setPreferredSize(new Dimension(250, 180));
		this.setMinimumSize(new Dimension(250, 180));
		this.newDatePicker = newDatePicker;

		Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener()
		{
			@Override
			public void eventDispatched(@Nullable AWTEvent event)
			{
				if (event instanceof KeyEvent)
				{
					KeyEvent k = (KeyEvent) event;
					if (k.getID() == KeyEvent.KEY_PRESSED)
						if (k.getKeyCode() == KeyEvent.VK_ESCAPE)
							newDatePicker.hidePopup();
				}
			}
		}, AWTEvent.KEY_EVENT_MASK);

		currentDate = LocalDate.now();
		calenderCellRenderer = new CalenderCellRenderer();
		calenderTableModel = new CalenderTableModel(null, null);
		calenderCellRenderer.setCurrentDate(currentDate);

		initColumnNames();
		this.setBackground(Color.WHITE);
		setLayout(new BorderLayout());

		initControlPanel();
		initCalenderPanel();
		
		updateData();
	}

	private void initColumnNames()
	{
		columnNames = new String[7];
		int count = 0;
		for (DayOfWeek day : DayOfWeek.values())
		{
			columnNames[count++] = day.getDisplayName(TextStyle.SHORT, new Locale("en"));
		}
	}

	private void initControlPanel()
	{
		JPanel controlPanel = new JPanel();
		controlPanel.setBackground(Color.WHITE);
		controlPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		add(controlPanel, BorderLayout.NORTH);

		GridBagLayout gblPanel = new GridBagLayout();
		gblPanel.columnWidths = new int[] { 10, 28, 10, 38, 10, 32, 10, 0 };
		gblPanel.rowHeights = new int[] { 16, 0 };
		gblPanel.columnWeights = new double[] { 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		gblPanel.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
		controlPanel.setLayout(gblPanel);

		JLabel lblMonthLeft = new JLabel("<");
		lblMonthLeft.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(@Nullable MouseEvent e)
			{
				currentDate = currentDate.minusMonths(1);
				updateData();
			}
		});
		GridBagConstraints gbcLblMonthLeft = new GridBagConstraints();
		gbcLblMonthLeft.anchor = GridBagConstraints.NORTHWEST;
		gbcLblMonthLeft.insets = new Insets(0, 0, 0, 5);
		gbcLblMonthLeft.gridx = 0;
		gbcLblMonthLeft.gridy = 0;

		lblMonth = new JLabel(currentDate.getMonth().name());
		FontMetrics fontMetrics = lblMonth.getFontMetrics(lblMonth.getFont());
		int width = 0;
		for (Month month : Month.values())
		{
			int currentWidth = fontMetrics.stringWidth(month.name());
			width = width > currentWidth ? width : currentWidth;
		}
		controlPanel.add(lblMonthLeft, gbcLblMonthLeft);
		lblMonth.setHorizontalAlignment(JLabel.CENTER);
		GridBagConstraints gbcLblMonth = new GridBagConstraints();
		gbcLblMonth.anchor = GridBagConstraints.NORTH;
		gbcLblMonth.insets = new Insets(0, 0, 0, 5);
		gbcLblMonth.gridx = 1;
		gbcLblMonth.gridy = 0;
		lblMonth.setPreferredSize(new Dimension(width, 20));
		controlPanel.add(lblMonth, gbcLblMonth);

		JLabel lblMonthRight = new JLabel(">");
		lblMonthRight.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(@Nullable MouseEvent e)
			{
				currentDate = currentDate.plusMonths(1);
				updateData();
			}
		});
		GridBagConstraints gbcLblMonthRight = new GridBagConstraints();
		gbcLblMonthRight.anchor = GridBagConstraints.NORTHWEST;
		gbcLblMonthRight.insets = new Insets(0, 0, 0, 5);
		gbcLblMonthRight.gridx = 2;
		gbcLblMonthRight.gridy = 0;
		controlPanel.add(lblMonthRight, gbcLblMonthRight);

		JLabel lblYearLeft = new JLabel("<");
		lblYearLeft.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(@Nullable MouseEvent e)
			{
				currentDate = currentDate.minusYears(1);
				updateData();
			}
		});
		GridBagConstraints gbcLblYearLeft = new GridBagConstraints();
		gbcLblYearLeft.anchor = GridBagConstraints.NORTHWEST;
		gbcLblYearLeft.insets = new Insets(0, 0, 0, 5);
		gbcLblYearLeft.gridx = 4;
		gbcLblYearLeft.gridy = 0;
		controlPanel.add(lblYearLeft, gbcLblYearLeft);

		this.lblYear = new JLabel(currentDate.getYear() + "");
		lblYear.setHorizontalAlignment(JLabel.CENTER);
		GridBagConstraints gbcLblYear = new GridBagConstraints();
		gbcLblYear.anchor = GridBagConstraints.NORTHWEST;
		gbcLblYear.insets = new Insets(0, 0, 0, 5);
		gbcLblYear.gridx = 5;
		gbcLblYear.gridy = 0;
		controlPanel.add(lblYear, gbcLblYear);

		JLabel lblYearRight = new JLabel(">");
		lblYearRight.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(@Nullable MouseEvent e)
			{
				currentDate = currentDate.plusYears(1);
				updateData();
			}
		});
		GridBagConstraints gbcLblYearRight = new GridBagConstraints();
		gbcLblYearRight.anchor = GridBagConstraints.NORTHWEST;
		gbcLblYearRight.gridx = 6;
		gbcLblYearRight.gridy = 0;
		controlPanel.add(lblYearRight, gbcLblYearRight);

	}

	private void initCalenderPanel()
	{
		JPanel calenderPanel = new JPanel();
		calenderPanel.setBackground(Color.WHITE);
		add(calenderPanel, BorderLayout.CENTER);

		table = new JTable(calenderTableModel);
		table.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(@Nullable MouseEvent e)
			{
				int row = table.getSelectedRow();
				int column = table.getSelectedColumn();
				if (((LocalDate) table.getValueAt(row, column)).getMonthValue() == currentDate.getMonthValue())
				{
					currentDate = (LocalDate) table.getValueAt(row, column);
					updateData();
					calenderCellRenderer.setCurrentDate(currentDate);
					newDatePicker.updateDate(currentDate);
				}
			}
		});
		table.setCellSelectionEnabled(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		// avoid reordering and resizing of columns
		table.getTableHeader().setReorderingAllowed(false);
		table.getTableHeader().setResizingAllowed(false);
		table.setShowGrid(false);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		table.updateUI();
		table.setMaximumSize(new Dimension(200, 200));

		JPanel headerPanel = new JPanel();
		headerPanel.setBackground(Color.WHITE);
		headerPanel.setLayout(new BorderLayout());
		headerPanel.add(table.getTableHeader(), BorderLayout.CENTER);
		headerPanel.add(new JSeparator(), BorderLayout.SOUTH);
		calenderPanel.add(headerPanel);
		calenderPanel.add(table, BorderLayout.CENTER);
	}

	public void setCurrentDate(LocalDate date)
	{
		this.currentDate = date;
		calenderCellRenderer.setCurrentDate(currentDate);
	}

	private void updateData()
	{
		calenderCellRenderer.setCurrentMonth(currentDate);
		LocalDate firstDayOfMonth = currentDate.withDayOfMonth(1);
		LocalDate firstDay = firstDayOfMonth.minusDays(firstDayOfMonth.getDayOfWeek().getValue() - 1);
		Object[][] data = new Object[6][7];
		for (int j = 0; j < 6; j++)
		{
			for (int i = 0; i < 7; i++)
			{
				data[j][i] = firstDay;
				firstDay = firstDay.plusDays(1);
			}
		}

		this.calenderTableModel.setDataVector(data, columnNames);
		for (int i = 0; i < table.getColumnCount(); i++)
		{
			table.getColumnModel().getColumn(i).setCellRenderer(calenderCellRenderer);
			table.getColumnModel().getColumn(i).setHeaderRenderer(new CalenderHeaderRenderer());
			table.getColumnModel().getColumn(i).setPreferredWidth(250 / 7);
			table.getColumnModel().getColumn(i).setWidth(250 / 7);
		}
		lblMonth.setText(currentDate.getMonth().name());
		lblYear.setText(currentDate.getYear() + "");
	}

	private static class CalenderTableModel extends DefaultTableModel
	{
		public CalenderTableModel(Object[][] data, String[] columnNames)
		{
			super(data, columnNames);
		}

		@Override
		public boolean isCellEditable(int row, int column)
		{
			return false;
		}
	}

	private static class CalenderCellRenderer extends DefaultTableCellRenderer
	{
		private @Nullable LocalDate currentMonth, currentDateTime;
		private final Color GRAY = new Color(195, 195, 195);
		private final Color SELECTION_BACKGROUND = new Color(203, 233, 247);
		private final Color SELECTION_BORDER = new Color(93, 184, 228);

		public CalenderCellRenderer()
		{
			setHorizontalAlignment(CENTER);
		}

		@SuppressWarnings("null")
		@Override
		public Component getTableCellRendererComponent(@Nullable JTable table, @Nullable Object value,
				boolean isSelected, boolean hasFocus, int row, int column)
		{
			if(value==null || currentDateTime==null || currentMonth==null)
				return this;
			
			super.getTableCellRendererComponent(table, ((LocalDate) value).getDayOfMonth(), false, hasFocus, row, column);
			super.setBackground(Color.WHITE);
			if (currentDateTime.isEqual((LocalDate) value))
			{
				super.setBackground(SELECTION_BACKGROUND);
				setBorder(new LineBorder(SELECTION_BORDER));
			}
			else if (currentMonth.getMonth() != ((LocalDate) value).getMonth())
				super.setForeground(GRAY);
			else
				super.setForeground(Color.BLACK);
			
			return this;
		}

		public void setCurrentDate(LocalDate _currentDateTime)
		{
			currentDateTime = _currentDateTime;
		}

		public void setCurrentMonth(LocalDate _currentMonth)
		{
			currentMonth = _currentMonth;
		}

	}

	private static class CalenderHeaderRenderer extends DefaultTableCellRenderer
	{
		public CalenderHeaderRenderer()
		{
			setHorizontalAlignment(CENTER);
		}

		@Override
		public Component getTableCellRendererComponent(@Nullable JTable table, @Nullable Object value,
				boolean isSelected, boolean hasFocus, int row, int column)
		{
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			super.setBackground(Color.WHITE);
			return this;
		}
	}

}
