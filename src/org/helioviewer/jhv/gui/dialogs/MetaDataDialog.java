package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.helioviewer.jhv.base.Globals;
import org.helioviewer.jhv.base.Settings;
import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.base.Globals.DialogType;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.PredefinedFileFilter;
import org.helioviewer.jhv.layers.AbstractImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.LayerListener;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.TimeLine;
import org.helioviewer.jhv.viewmodel.TimeLine.TimeLineListener;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

//TODO: close streams properly during exceptions: try(...)

/**
 * Dialog that is used to display meta data for an image.
 */
public class MetaDataDialog extends JDialog implements LayerListener, TimeLineListener
{
	private final JButton closeButton = new JButton("Close");
	private final JButton exportFitsButton = new JButton("Export FITS header as XML");

	private List<String> infoList = new ArrayList<String>();
	private JList<String> listBox = new JList<String>();
	private boolean metaDataOK;
	private @Nullable String outFileName;
	JScrollPane listScroller;

	private @Nullable Document xmlDoc;
	private static final String LAST_DIRECTORY = "metadata.save.lastPath";
	
	public MetaDataDialog()
	{
		super(MainFrame.SINGLETON, "Image metainfo");
		
    	Telemetry.trackEvent("Dialog", "Type", getClass().getSimpleName());
		
		setLayout(new BorderLayout());
		setResizable(false);

		listBox.setFont(new Font("Courier", Font.PLAIN, 12));

		listBox.setCellRenderer(new ListCellRenderer<Object>()
			{
				@SuppressWarnings("null")
				public Component getListCellRendererComponent(@Nullable JList<?> list, @Nullable Object value, int index, boolean isSelected, boolean cellHasFocus)
				{
					JTextArea textArea = new JTextArea(value.toString().trim());
					textArea.setLineWrap(true);
					textArea.setEditable(false);
					textArea.setWrapStyleWord(true);
					textArea.setFont(list.getFont());
					return textArea;
				}
			});

		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		bottomPanel.add(exportFitsButton);
		// bottomPanel.add(exportButton);
		bottomPanel.add(closeButton);

		listScroller = new JScrollPane(listBox);
		add(listScroller, BorderLayout.CENTER);
		add(bottomPanel, BorderLayout.PAGE_END);

		// add action listeners to the buttons
		closeButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(@Nullable ActionEvent e)
			{
				infoList.clear();
				resetData();
				dispose();
			}
		});
		exportFitsButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(@Nullable ActionEvent e)
			{
				if(xmlDoc==null)
					return;
				
				// Open save-dialog
				final File file = Globals.showFileDialog(
						DialogType.SAVE_FILE,
						"Save metadata",
						Settings.getString(LAST_DIRECTORY),
						true,
						outFileName,
						PredefinedFileFilter.XML);
				
				if (file == null)
					return;
				
				@SuppressWarnings("null")
				DOMSource source = new DOMSource(xmlDoc.getDocumentElement().getElementsByTagName("fits").item(0));
				if (!saveXMLDocument(source, file.getPath() + outFileName))
					JOptionPane.showMessageDialog(MetaDataDialog.this, "Could not save document.");
			}
		});
		
		// exportButton.addActionListener(this);
		pack();
		Layers.addLayerListener(this);
		TimeLine.SINGLETON.addListener(this);
		setLocationRelativeTo(MainFrame.SINGLETON);
		
		addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentShown(@Nullable ComponentEvent e)
			{
				updateData();
			}
		});
		
		getRootPane().registerKeyboardAction(new ActionListener()
		{
			@Override
			public void actionPerformed(@Nullable ActionEvent e)
			{
				setVisible(false);
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		pack();
		setSize(450, 600);

		setLocationRelativeTo(MainFrame.SINGLETON);
		setVisible(true);
	}

	public void updateData()
	{
		AbstractImageLayer il=Layers.getActiveImageLayer();
		if (il != null)
		{
			MetaData md=il.getMetaData(TimeLine.SINGLETON.getCurrentDateTime());
			Document doc=il.getMetaDataDocument(TimeLine.SINGLETON.getCurrentDateTime());
			
			if(md!=null && doc!=null)
				setMetaData(md,doc);
			else
			{
				resetData();
				addDataItem("Metadata not available.");
			}
		}
		else
			resetData();
	}
	

	private void resetData()
	{
		infoList.clear();

		// update the listBox
		listBox.setListData(infoList.toArray(new String[0]));

		// set the status of export button
		if (!metaDataOK)
			exportFitsButton.setEnabled(false);
		else
			exportFitsButton.setEnabled(true);
	}

	/**
	 * Adds a data item to the list
	 * 
	 * @param _item
	 *            New item to add
	 * @see #setMetaData(MetaDataView)
	 */
	public void addDataItem(String _item)
	{
		infoList.add(_item);

		// update the listBox
		listBox.setListData(infoList.toArray(new String[0]));
	}

	/**
	 * Sets the full document which can be found reading the given MetaDataView.
	 * 
	 * @param metaDataView
	 *            Source to read
	 * @see #addDataItem(String)
	 */
	@SuppressWarnings("null")
	public void setMetaData(@Nonnull MetaData metaData, @Nonnull Document doc)
	{
		metaDataOK = true;
		resetData();
		addDataItem("-------------------------------");
		addDataItem("       Basic Information       ");
		addDataItem("-------------------------------");
		addDataItem("Observatory : " + metaData.getObservatory());
		addDataItem("Instrument  : " + metaData.getInstrument());
		addDataItem("Detector    : " + metaData.getDetector());
		addDataItem("Measurement : " + metaData.getMeasurement());
		addDataItem("Date        : " + metaData.getLocalDateTime().toLocalDate());
		addDataItem("Time        : " + metaData.getLocalDateTime().toLocalTime());

		// Send xml data to meta data dialog box
		Node root = doc.getDocumentElement().getElementsByTagName("fits").item(0);
		writeXMLData(root, 0);
		root = doc.getDocumentElement().getElementsByTagName("helioviewer").item(0);
		if (root != null)
			writeXMLData(root, 0);
		
		// set the xml data for the MetaDataDialog
		xmlDoc = doc;

		// set the export file name for MetaDataDialog
		outFileName = "";
		
		if(metaData.getFullName()!=null)
			outFileName += metaData.getFullName().replace(" ", "_") + " ";
		
		outFileName += metaData.getLocalDateTime().format(
						Globals.FILE_DATE_TIME_FORMATTER) + ".fits.xml";
	}

	/**
	 * A method that writes the xml box specified by its root node to the list
	 * box in image info dialog box.
	 * 
	 * @param node
	 *            Node to write
	 * @param indent
	 *            Number of tabstops to insert
	 */
	private void writeXMLData(Node node, int indent)
	{
		// get element name and value
		String nodeName = node.getNodeName();
		String nodeValue = getElementValue(node);

		if (nodeName.equals("fits")) {
			addDataItem("-------------------------------");
			addDataItem("          FITS Header");
			addDataItem("-------------------------------");
		} else if (nodeName.equals("helioviewer")) {
			addDataItem("-------------------------------");
			addDataItem("      Helioviewer Header");
			addDataItem("-------------------------------");
		}
		else
		{
			String tab = new String(new char[indent]).replace((char)0, '\t');
			
			addDataItem(tab + nodeName + ": " + nodeValue);
		}

		// write the child nodes recursively
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				writeXMLData(child, indent + 1);
			}
		}
	}

	/**
	 * A method that gets the value of a node element.
	 * 
	 * If the node itself has children and no text value, an empty string is
	 * returned. This is maybe an overkill for our purposes now, but takes into
	 * account the possibility of nested tags.
	 * 
	 * @param elem
	 *            Node to read
	 * @return value of the node
	 */
	private final String getElementValue(Node elem)
	{
		if (elem != null && elem.hasChildNodes())
			for (Node child = elem.getFirstChild(); child != null; child = child.getNextSibling())
				if (child.getNodeType() == Node.TEXT_NODE)
					return child.getNodeValue();
		
		return "";
	}

	/**
	 * This routine saves the fits data into an XML file.
	 * 
	 * @param source
	 *            XML document to save
	 * @param filename
	 *            XML file name
	 */
	private boolean saveXMLDocument(DOMSource source, String filename) {
		// open the output stream where XML Document will be saved
		File xmlOutFile = new File(filename);
		FileOutputStream fos;
		Transformer transformer;
		try
		{
			fos = new FileOutputStream(xmlOutFile);
		}
		catch (FileNotFoundException e)
		{
			Telemetry.trackException(e);
			return false;
		}
		try {
			// Use a Transformer for the purpose of output
			TransformerFactory transformerFactory = TransformerFactory
					.newInstance();
			try {
				transformer = transformerFactory.newTransformer();
			} catch (TransformerConfigurationException e) {
				Telemetry.trackException(e);
				return false;
			}

			// The source is the fits header

			// The destination for output
			StreamResult result = new StreamResult(fos);

			// transform source into result will do a file save
			try {
				transformer.transform(source, result);
			} catch (TransformerException e) {
				Telemetry.trackException(e);
			}
			return true;
		} finally {
			try {
				fos.close();
			} catch (IOException e) {
				Telemetry.trackException(e);
			}
		}
	}

	@Override
	public void timeStampChanged(LocalDateTime current, LocalDateTime last)
	{
		AbstractImageLayer il = Layers.getActiveImageLayer();
		if (il != null)
		{
			MetaData md=il.getMetaData(TimeLine.SINGLETON.getCurrentDateTime());
			Document doc=il.getMetaDataDocument(TimeLine.SINGLETON.getCurrentDateTime());
			
			if(md!=null && doc!=null)
				setMetaData(md,doc);
			else
			{
				resetData();
				addDataItem("Metadata not available.");
			}
		}
		else
			resetData();
	}

	@Override
	public void dateTimesChanged(int framecount)
	{
	}

	@Override
	public void layerAdded()
	{
	}

	@Override
	public void layersRemoved()
	{
	}

	@Override
	public void activeLayerChanged(@Nullable Layer layer)
	{
		updateData();
	}
}
