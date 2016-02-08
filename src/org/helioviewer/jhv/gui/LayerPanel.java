package org.helioviewer.jhv.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.time.LocalDateTime;

import javax.annotation.Nullable;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.helioviewer.jhv.base.Globals;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.dialogs.AddLayerDialog;
import org.helioviewer.jhv.gui.dialogs.DownloadMovieDialog;
import org.helioviewer.jhv.gui.dialogs.MetaDataDialog;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.LayerListener;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.TimeLine;
import org.helioviewer.jhv.viewmodel.TimeLine.TimeLineListener;

public class LayerPanel extends JPanel implements LayerListener, TimeLineListener
{
	private static final int SIZE;
	private static final ImageIcon ICON_REMOVE;
	
	private JTable table;
	private LayerTableModel tableModel;
	private static final String[] COLUMN_TITLES=new String[]{ "", "", "", "", "" };
	private static final ImageIcon[] LAYER_LOADING=new ImageIcon[8];

	static
	{
		SIZE = new JLabel("Wy").getPreferredSize().height;
		ICON_REMOVE = IconBank.getIcon(JHVIcon.REMOVE_NEW, SIZE, SIZE);
		
		for(int i=0;i<LAYER_LOADING.length;i++)
			LAYER_LOADING[i]=IconBank.getIcon(JHVIcon.LAYER_LOADING, SIZE-4, SIZE-4, Math.PI*2/2/LAYER_LOADING.length*i);
	}
	
	private JButton btnDownloadLayer;
	private static final Cursor HAND_CURSOR = new Cursor(Cursor.HAND_CURSOR);
	private static final ImageIcon WARNING_BAD_REQUEST = IconBank.getIcon(JHVIcon.WARNING, SIZE-2, SIZE-2);
	
	
	private @Nullable Layer activePopupLayer;

	private JPopupMenu popupMenu;

	private JMenuItem showLayer;
	private JMenuItem hideLayer;
	private JMenuItem showMetaView;
	private JMenuItem downloadLayer;
	private JMenuItem removeLayer;
	
	private JButton btnShowInfo;

	public LayerPanel()
	{
		initPopup();
		initGUI();
		updateData();
		Layers.addLayerListener(this);
		TimeLine.SINGLETON.addListener(this);
	}

	private void initPopup()
	{
		popupMenu = new JPopupMenu();
		showMetaView = new JMenuItem("Show metadata...", IconBank.getIcon(JHVIcon.INFO_NEW, SIZE, SIZE));
		showMetaView.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(@Nullable ActionEvent e)
			{
				new MetaDataDialog();
			}
		});
		
		downloadLayer = new JMenuItem("Download movie", IconBank.getIcon(JHVIcon.DOWNLOAD_NEW, SIZE, SIZE));
		downloadLayer.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(@Nullable ActionEvent e)
			{
				if(activePopupLayer==null)
					return;
				
				String downloadURL=activePopupLayer.getDownloadURL();
				if (downloadURL != null)
					new DownloadMovieDialog(downloadURL, activePopupLayer);
			}
		});
		
		hideLayer = new JMenuItem("Hide layer", IconBank.getIcon(JHVIcon.HIDDEN, SIZE, SIZE));
		hideLayer.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(@Nullable ActionEvent e)
			{
				if(activePopupLayer==null)
					return;
				
				activePopupLayer.setVisible(false);
				updateData();
			}
		});
		showLayer = new JMenuItem("Show layer", IconBank.getIcon(JHVIcon.VISIBLE, SIZE, SIZE));
		showLayer.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(@Nullable ActionEvent e)
			{
				if(activePopupLayer==null)
					return;
				
				activePopupLayer.setVisible(true);
				updateData();
			}
		});
				
		removeLayer = new JMenuItem("Close layer", IconBank.getIcon(JHVIcon.REMOVE_NEW, SIZE, SIZE));
		removeLayer.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(@Nullable ActionEvent e)
			{
				Layers.removeLayer(activePopupLayer);
			}
		});
		showLayer.setVisible(false);
		hideLayer.setVisible(false);

		popupMenu.add(showMetaView);
		popupMenu.add(downloadLayer);
		popupMenu.addSeparator();
		popupMenu.add(hideLayer);
		popupMenu.add(showLayer);
		popupMenu.add(removeLayer);

		popupMenu.addPopupMenuListener(new PopupMenuListener()
		{
			@Override
			public void popupMenuWillBecomeVisible(@Nullable PopupMenuEvent e)
			{
				if(activePopupLayer==null)
					return;
				
				if (activePopupLayer instanceof ImageLayer)
				{
					showMetaView.setEnabled(true);
					downloadLayer.setEnabled(activePopupLayer.getDownloadURL()!=null);
					removeLayer.setEnabled(false);
				}
				else
				{
					showMetaView.setEnabled(false);
					downloadLayer.setEnabled(false);
					removeLayer.setEnabled(false);
				}
			}
			
			@Override
			public void popupMenuWillBecomeInvisible(@Nullable PopupMenuEvent e)
			{
			}
			
			@Override
			public void popupMenuCanceled(@Nullable PopupMenuEvent e)
			{
			}
		});
	}
	
	private void setFixedWidth(int width, int column)
	{
		table.getColumnModel().getColumn(column).setMinWidth(width);
		table.getColumnModel().getColumn(column).setMaxWidth(width);
		table.getColumnModel().getColumn(column).setPreferredWidth(width);
		table.getColumnModel().getColumn(column).setWidth(width);
		table.getColumnModel().getColumn(column).setResizable(false);
	}
	
	private int ignoreTableEvents=0;
	
	private void initGUI()
	{
		setLayout(new BorderLayout(0, 0));

		tableModel = new LayerTableModel(new Object[1][5], COLUMN_TITLES);
		
		tableModel.addTableModelListener(new TableModelListener()
		{
			@Override
			public void tableChanged(@Nullable TableModelEvent e)
			{
				if(ignoreTableEvents>0)
					return;
				
				if(e==null)
					return;
				
				if(e.getColumn()==0)
				{
					Layer layer = Layers.getLayer(e.getFirstRow());
					if (layer != null)
						layer.setVisible(!layer.isVisible());
				}
			}
		});
		
		table = new JTable(tableModel);
		table.setTableHeader(null);
		
		//visible
		setFixedWidth(SIZE+4, 0);
		
		//status (retry, ...)
		table.getColumnModel().getColumn(1).setCellRenderer(new ImageIconCellRenderer());
		setFixedWidth(SIZE, 1);
		
		//name
		//table.getColumnModel().getColumn(2).setCellRenderer(new ImageIconCellRenderer());
		
		//date/time
		table.getColumnModel().getColumn(3).setCellRenderer(new ImageIconCellRenderer());
		table.getColumnModel().getColumn(3).setResizable(false);
		setFixedWidth(150, 3);
		
		//delete
		table.getColumnModel().getColumn(4).setCellRenderer(new ImageIconCellRenderer());
		setFixedWidth(SIZE, 4);
		
		
		//table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setShowGrid(false);
		table.setIntercellSpacing(new Dimension(0, 0));
		table.setRowHeight(SIZE + 4);
		
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener()
				{
					@Override
					public void valueChanged(@Nullable ListSelectionEvent e)
					{
						Layers.setActiveLayer(table.getSelectedRow());
					}
				});
		
		table.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(@Nullable MouseEvent e)
			{
				if(e==null)
					return;
				
				int row = table.rowAtPoint(e.getPoint());
				int column = table.columnAtPoint(e.getPoint());
				if (column == 4)
					Layers.removeLayer(row);
				else if (column == 1)
				{
					boolean value = (boolean) table.getValueAt(row, column);
					if (value)
					{
						Layers.getLayer(row).retry();
						updateData();
					}
				}
			}

			@Override
			public void mousePressed(@Nullable MouseEvent e)
			{
				if(e==null)
					return;
				
				if (e.isPopupTrigger())
				{
					int row = table.rowAtPoint(e.getPoint());
					if (row >= 0 && row < Layers.getLayers().size())
					{
						Layers.setActiveLayer(row);
						activePopupLayer=Layers.getLayer(row);
						
						hideLayer.setVisible(activePopupLayer.isVisible());
						showLayer.setVisible(!activePopupLayer.isVisible());
						popupMenu.show(table, e.getX(), e.getY());
					}
				}
			}
			
			@Override
			public void mouseReleased(@Nullable MouseEvent e)
			{
				//popup triggers should be checked on mousePressed AND mouseReleased
				mousePressed(e);
			}
		});
		
		table.addMouseMotionListener(new MouseMotionAdapter()
		{
			@Override
			public void mouseMoved(@Nullable MouseEvent e)
			{
				if(e==null)
					return;
				
				JTable jTable = (JTable) e.getSource();
				int row = jTable.rowAtPoint(e.getPoint());
				int column = table.columnAtPoint(e.getPoint());
				if (column == 0 || column == 4)
					table.setCursor(HAND_CURSOR);
				else if (column == 1)
				{
					boolean value = (boolean) table.getValueAt(row, column);
					if (value)
						table.setCursor(HAND_CURSOR);						
					else 
						table.setCursor(Cursor.getDefaultCursor());
				}
				else
					table.setCursor(Cursor.getDefaultCursor());
			}
		});
		
		table.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(@Nullable KeyEvent e)
			{
				if(e==null)
					return;
				
				if (e.getKeyCode() == KeyEvent.VK_DELETE /*|| e.getKeyCode() == KeyEvent.VK_BACK_SPACE*/)
					Layers.removeLayer(table.getSelectedRow());
			}
		});
		
		add(table, BorderLayout.CENTER);
		
		JPanel panel = new JPanel();
		panel.setBorder(new EmptyBorder(10, 5, 10, 5));
		add(panel, BorderLayout.SOUTH);
		
		btnShowInfo = new JButton(IconBank.getIcon(JHVIcon.INFO_NEW, SIZE, SIZE));
		btnShowInfo.setToolTipText("Show the Metainformation of the currently selected Layer");
		btnShowInfo.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(@Nullable ActionEvent e)
			{
				new MetaDataDialog();
			}
		});
		panel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
		panel.add(btnShowInfo);
		
				btnDownloadLayer = new JButton(IconBank.getIcon(JHVIcon.DOWNLOAD_NEW, SIZE, SIZE));
				btnDownloadLayer.setToolTipText("Download the currently selected Layer");
				btnDownloadLayer.setEnabled(false);
				btnDownloadLayer.addActionListener(new ActionListener()
				{
					@Override
					public void actionPerformed(@Nullable ActionEvent e)
					{
						ImageLayer l = Layers.getActiveImageLayer();
						if (l == null)
							return;
						
						String downloadURL = l.getDownloadURL();
						if(downloadURL!=null)
							new DownloadMovieDialog(downloadURL, l);
					}
				});
				panel.add(btnDownloadLayer);
		
				JButton btnAddLayer = new JButton("Add Layer", IconBank.getIcon(JHVIcon.ADD_NEW, SIZE, SIZE));
				btnAddLayer.addActionListener(new ActionListener()
				{
					@Override
					public void actionPerformed(@Nullable ActionEvent e)
					{
						new AddLayerDialog().setVisible(true);
					}
				});
				panel.add(btnAddLayer);
	}
	
	private static int loadingFrameCounter = 0;
	private Timer loadingAnimation = new Timer(100,new ActionListener()
	{
		@Override
		public void actionPerformed(@Nullable ActionEvent e)
		{
			updateData();
		}
	});
	
	public void updateData()
	{
		loadingAnimation.stop();
		for (Layer layer : Layers.getLayers())
			if(layer.isLoading())
			{
				loadingAnimation.start();
				break;
			}
		
		try
		{
			ignoreTableEvents++;
			
			loadingFrameCounter = (int)(System.currentTimeMillis()/100) % LAYER_LOADING.length;		
			tableModel.setRowCount(Layers.getLayers().size());
			
			int row = 0;
			for (Layer layer : Layers.getLayers())
			{
				tableModel.setValueAt(layer.isVisible(), row, 0);
				tableModel.setValueAt(layer.retryNeeded(), row, 1);
				tableModel.setValueAt(layer.getName(), row, 2);
				tableModel.setValueAt(layer.getCurrentTime(), row, 3);
				row++;
			}
		}
		finally
		{
			ignoreTableEvents--;
		}
		
		if (Layers.getActiveLayer()!=null)
			table.setRowSelectionInterval(
					Layers.getActiveLayerIndex(),
					Layers.getActiveLayerIndex());
		else
			table.clearSelection();
	}

	private static class LayerTableModel extends DefaultTableModel
	{
		public LayerTableModel(Object[][] data, String[] columnNames)
		{
			super(data, columnNames);
		}

		@Override
		public Class<?> getColumnClass(int columnIndex)
		{
			if (columnIndex == 0)
				return Boolean.class;

			return super.getColumnClass(columnIndex);
		}

		@Override
		public boolean isCellEditable(int row, int column)
		{
			return column == 0;
		}
	}

	private static class ImageIconCellRenderer extends DefaultTableCellRenderer
	{
		public ImageIconCellRenderer()
		{
			// setOpaque(true);
			setHorizontalAlignment(CENTER);
			// setBackground(Color.WHITE);
		}

		@Override
		public Component getTableCellRendererComponent(@Nullable JTable table, @Nullable Object value, boolean isSelected, boolean hasFocus, final int row, int column)
		{
			Layer layer=Layers.getLayer(row);
			
			switch (column)
			{
				case 0:
					return super.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column);
				case 1:
					
					//TODO: better solution
					//TODO: white when selected for contrast
					JLabel label = (JLabel) super.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column);
					label.setPreferredSize(new Dimension(20, 20));
					if(layer!=null && layer.retryNeeded())
						label.setIcon(WARNING_BAD_REQUEST);
					else if(layer!=null && layer.isLoading())
						label.setIcon(LAYER_LOADING[loadingFrameCounter]);
					else
						label.setIcon(null);
					return label;
				case 2:
					return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				case 3:
					LocalDateTime localDateTime = (LocalDateTime) value;
					String date = localDateTime != null ? localDateTime.format(Globals.DATE_TIME_FORMATTER) : "";
					return super.getTableCellRendererComponent(table, date, isSelected, hasFocus, row, column);
				case 4:
					JLabel label4 = (JLabel) super.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column);
					if (layer instanceof ImageLayer)
					{
						label4.setIcon(ICON_REMOVE);
						label4.setPreferredSize(new Dimension(20, 20));
					}
					else
						label4.setIcon(null);
					return label4;
				default:
					return this;
			}
		}
	}

	@Override
	public void layerAdded()
	{
		updateData();
	}

	@Override
	public void layersRemoved()
	{
		updateData();
	}

	@Override
	public void activeLayerChanged(@Nullable Layer layer)
	{
		if (layer != null)
		{
			btnDownloadLayer.setEnabled(layer.getDownloadURL()!=null);
			btnShowInfo.setEnabled(layer instanceof ImageLayer);
			
			if (Layers.getActiveLayer()!=null)
				table.setRowSelectionInterval(
						Layers.getActiveLayerIndex(),
						Layers.getActiveLayerIndex());
		}
		else
			table.clearSelection();
	}

	@Override
	public void timeStampChanged(LocalDateTime current, LocalDateTime last)
	{
		updateData();		
	}

	@Override
	public void isPlayingChanged(boolean _isPlaying)
	{
	}

	@Override
	public void timeRangeChanged(LocalDateTime _start, LocalDateTime _end)
	{
	}
}
