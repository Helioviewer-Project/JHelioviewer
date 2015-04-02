package org.helioviewer.jhv.gui.components.newComponents;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;

import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;

import javax.swing.BoxLayout;

import com.jgoodies.forms.factories.FormFactory;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

public class Test extends JPanel {
	private JPanel panel;
	private JTable table;
	private TestModel model;
	private String[] columnNames;
	private LocalDateTime currentDateTime;
	private JLabel lblNewLabel;
	private JLabel lblYear;
	
	private final Color GRID_COLOR = new Color(237, 237, 237);
	/**
	 * Create the panel.
	 */
	public Test() {
		currentDateTime = LocalDateTime.now();
		setBorder(new EmptyBorder(4, 4, 4, 4));
		setLayout(new BorderLayout(0, 0));

		panel = new JPanel();
		panel.setBackground(Color.WHITE);
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		add(panel, BorderLayout.NORTH);
		
		GridBagLayout gblPanel = new GridBagLayout();
		gblPanel.columnWidths = new int[]{10, 28, 10, 38, 10, 32, 10, 0};
		gblPanel.rowHeights = new int[]{16, 0};
		gblPanel.columnWeights = new double[]{0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		gblPanel.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		panel.setLayout(gblPanel);

		JLabel lblMonthLeft = new JLabel("<");
		lblMonthLeft.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e) {
				currentDateTime = currentDateTime.minusMonths(1);
				updateData();
			}
		});
		GridBagConstraints gbcLblMonthLeft = new GridBagConstraints();
		gbcLblMonthLeft.anchor = GridBagConstraints.NORTHWEST;
		gbcLblMonthLeft.insets = new Insets(0, 0, 0, 5);
		gbcLblMonthLeft.gridx = 0;
		gbcLblMonthLeft.gridy = 0;
		panel.add(lblMonthLeft, gbcLblMonthLeft);
		
		lblNewLabel = new JLabel(currentDateTime.getMonth().name());
		lblNewLabel.setHorizontalAlignment(JLabel.CENTER);
		lblNewLabel.setPreferredSize(new Dimension(70, 20));
		GridBagConstraints gbcLblMonth = new GridBagConstraints();
		gbcLblMonth.anchor = GridBagConstraints.NORTH;
		gbcLblMonth.insets = new Insets(0, 0, 0, 5);
		gbcLblMonth.gridx = 1;
		gbcLblMonth.gridy = 0;
		panel.add(lblNewLabel, gbcLblMonth);

		JLabel lblMonthRight = new JLabel(">");
		lblMonthRight.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e) {
				currentDateTime = currentDateTime.plusMonths(1);
				updateData();
			}
		});
		GridBagConstraints gbcLblMonthRight = new GridBagConstraints();
		gbcLblMonthRight.anchor = GridBagConstraints.NORTHWEST;
		gbcLblMonthRight.insets = new Insets(0, 0, 0, 5);
		gbcLblMonthRight.gridx = 2;
		gbcLblMonthRight.gridy = 0;
		panel.add(lblMonthRight, gbcLblMonthRight);

		JLabel lblYearLeft = new JLabel("<");
		lblYearLeft.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e) {
				currentDateTime = currentDateTime.minusYears(1);
				updateData();
			}
		});
		GridBagConstraints gbcLblYearLeft = new GridBagConstraints();
		gbcLblYearLeft.anchor = GridBagConstraints.NORTHWEST;
		gbcLblYearLeft.insets = new Insets(0, 0, 0, 5);
		gbcLblYearLeft.gridx = 4;
		gbcLblYearLeft.gridy = 0;
		panel.add(lblYearLeft, gbcLblYearLeft);
		
		this.lblYear = new JLabel(currentDateTime.getYear() + "");
		lblYear.setHorizontalAlignment(JLabel.CENTER);
		GridBagConstraints gbcLblYear = new GridBagConstraints();
		gbcLblYear.anchor = GridBagConstraints.NORTHWEST;
		gbcLblYear.insets = new Insets(0, 0, 0, 5);
		gbcLblYear.gridx = 5;
		gbcLblYear.gridy = 0;
		panel.add(lblYear, gbcLblYear);

		JLabel lblYearRight = new JLabel(">");
		lblYearRight.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e) {
				currentDateTime = currentDateTime.plusYears(1);
				updateData();
			}
		});
		GridBagConstraints gbcLblYearRight = new GridBagConstraints();
		gbcLblYearRight.anchor = GridBagConstraints.NORTHWEST;
		gbcLblYearRight.gridx = 6;
		gbcLblYearRight.gridy = 0;
		panel.add(lblYearRight, gbcLblYearRight);

		JPanel panel_1 = new JPanel();
		panel_1.setBackground(Color.WHITE);
		add(panel_1, BorderLayout.CENTER);

		LocalDateTime firstDayOfMonth = currentDateTime.withDayOfMonth(1);
		DayOfWeek dayOfWeek = firstDayOfMonth.getDayOfWeek();
		Month month = firstDayOfMonth.getMonth();
		int days = currentDateTime.getDayOfMonth();
		columnNames = new String[7];
		int count = 0;
		for (DayOfWeek day : DayOfWeek.values()) {
			columnNames[count++] = day.getDisplayName(TextStyle.SHORT,
					new Locale("en"));
		}

		System.out.println("firstDay = " + firstDayOfMonth.getDayOfMonth());
		System.out.println("firstDay = "
				+ firstDayOfMonth.getDayOfWeek().getValue());
		LocalDateTime firstDay = firstDayOfMonth.minusDays(firstDayOfMonth
				.getDayOfWeek().getValue() - 1);

		Object[][] data = new Object[6][7];
		System.out.println("firstDay = " + firstDay.getDayOfMonth());
		CellRenderer cellRenderer = new CellRenderer();
		for (int j = 0; j < 6; j++) {
			for (int i = 0; i < 7; i++) {
				data[j][i] = firstDay.getDayOfMonth();
				System.out.println(firstDay.getDayOfMonth());
				firstDay = firstDay.plusDays(1);
			}
		}

		this.model = new TestModel(data, columnNames);
		table = new JTable(model);
		for (int i = 0; i < table.getColumnCount(); i++){
			table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
			table.getColumnModel().getColumn(i).setHeaderRenderer(new HeaderRenderer());
			table.getColumnModel().getColumn(i).setPreferredWidth(250/7);
			table.getColumnModel().getColumn(i).setWidth(250/7);
		}

		table.setCellSelectionEnabled(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setShowGrid(true);
		table.setGridColor(GRID_COLOR);
		// avoid reordering and resizing of columns
		table.getTableHeader().setReorderingAllowed(false);
		table.getTableHeader().setResizingAllowed(false);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		table.updateUI();
		table.setMaximumSize(new Dimension(200, 200));
		//JScrollPane scrollPane = new JScrollPane(table);
		//scrollPane.setSize(100, 100);
		panel_1.add(table.getTableHeader(), BorderLayout.NORTH);
		panel_1.add(table, BorderLayout.CENTER);

	}

	public void updateData() {
		int month = currentDateTime.getMonthValue();
		LocalDateTime firstDayOfMonth = currentDateTime.withDayOfMonth(1);
		LocalDateTime firstDay = firstDayOfMonth.minusDays(firstDayOfMonth
				.getDayOfWeek().getValue() - 1);
		Object[][] data = new Object[6][7];
		for (int j = 0; j < 6; j++) {
			for (int i = 0; i < 7; i++) {
				System.out.println(month + "  /  " + firstDay.getMonthValue());
				if (month != firstDay.getMonthValue()){
					System.out.println("month : " + month);
					System.out.println("month1: " + firstDay.getMonthValue());
					
				}
				else {
				}
				data[j][i] = firstDay.getDayOfMonth();
				System.out.println(firstDay.getDayOfMonth());
				firstDay = firstDay.plusDays(1);
			}
		}
		
		this.model.setDataVector(data, columnNames);
		CellRenderer cellRenderer = new CellRenderer();
		for (int i = 0; i < table.getColumnCount(); i++){
			table.getColumnModel().getColumn(i).setCellRenderer(new CellRenderer());
			table.getColumnModel().getColumn(i).setHeaderRenderer(new HeaderRenderer());
			table.getColumnModel().getColumn(i).setPreferredWidth(250/7);
			table.getColumnModel().getColumn(i).setWidth(250/7);
		}
		lblNewLabel.setText(currentDateTime.getMonth().name());
		lblYear.setText(currentDateTime.getYear()+"");
	}

	public static void main(String[] args) {
		Test t = new Test();
		JFrame frame = new JFrame();
		
		//final JFXPanel fxPanel = new JFXPanel();
		//frame.getContentPane().add(fxPanel);
		//DatePicker datePicker = new DatePicker(LocalDate.now());
		frame.getContentPane().add(t);
		frame.setSize(250, 300);
		frame.setVisible(true);
	}

}
