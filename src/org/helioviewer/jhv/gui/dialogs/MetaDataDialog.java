package org.helioviewer.jhv.gui.dialogs;

import org.helioviewer.jhv.base.Globals;
import org.helioviewer.jhv.base.Globals.DialogType;
import org.helioviewer.jhv.base.Settings;
import org.helioviewer.jhv.base.Settings.StringKey;
import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.PredefinedFileFilter;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.LayerListener;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.TimeLine;
import org.helioviewer.jhv.viewmodel.TimeLine.TimeLineListener;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog that is used to display meta data for an image.
 */
public class MetaDataDialog extends JDialog
{
	private final JButton closeButton = new JButton("Close");
	private final JButton exportFitsButton = new JButton("Export FITS header as XML");

	private List<String> infoList = new ArrayList<>();
	private JList<String> listBox = new JList<>();
	private boolean metaDataOK;
	private @Nullable String outFileName;
	JScrollPane listScroller;

	private @Nullable Document xmlDoc;

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
			public Component getListCellRendererComponent(@Nullable JList<?> list, @Nullable Object value, int index,
					boolean isSelected, boolean cellHasFocus)
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
				dispose();
			}
		});
		exportFitsButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(@Nullable ActionEvent e)
			{
				if (xmlDoc == null)
					return;

				// Open save-dialog
				final File file = Globals.showFileDialog(DialogType.SAVE_FILE, "Save metadata",
						Settings.getString(StringKey.METADATA_EXPORT_DIRECTORY), true, outFileName, PredefinedFileFilter.XML);

				if (file == null)
					return;

				@SuppressWarnings("null")
				DOMSource source = new DOMSource(xmlDoc.getDocumentElement().getElementsByTagName("fits").item(0));
				if (!exportXML(source, file.getPath() + outFileName))
					JOptionPane.showMessageDialog(MetaDataDialog.this, "Could not save document.");
			}
		});

		// exportButton.addActionListener(this);
		pack();
		Layers.addLayerListener(new LayerListener()
		{
			@Override
			public void layersRemoved()
			{
			}

			@Override
			public void layerAdded()
			{
			}

			@Override
			public void activeLayerChanged(@Nullable Layer _newLayer)
			{
				updateData();
			}
		});
		TimeLine.SINGLETON.addListener(new TimeLineListener()
		{
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
			public void dateTimesChanged(int framecount)
			{
			}
		});
		setLocationRelativeTo(MainFrame.SINGLETON);

		updateData();

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

	private void updateData()
	{
		ImageLayer il = Layers.getActiveImageLayer();
		if (il != null)
		{
			MetaData md = il.getMetaData(TimeLine.SINGLETON.getCurrentDateTime());
			Document doc = il.getMetaDataDocument(TimeLine.SINGLETON.getCurrentDateTime());

			if (md != null && doc != null)
				showData(md, doc);
			else
			{
				clearData();
				addDataItem("Metadata not available.");
			}
		}
		else
			clearData();
	}

	private void clearData()
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

	private void showData(@Nonnull MetaData metaData, @Nonnull Document doc)
	{
		metaDataOK = true;
		clearData();
		addDataItem("-------------------------------");
		addDataItem("       Basic Information       ");
		addDataItem("-------------------------------");
		addDataItem("Observatory : " + metaData.observatory);
		addDataItem("Instrument  : " + metaData.instrument);
		addDataItem("Detector    : " + metaData.detector);
		addDataItem("Measurement : " + metaData.measurement);
		addDataItem("Date        : " + metaData.localDateTime.toLocalDate());
		addDataItem("Time        : " + metaData.localDateTime.toLocalTime());

		// Send xml data to meta data dialog box
		Node root = doc.getDocumentElement().getElementsByTagName("fits").item(0);
		printXMLData(root, 0);
		root = doc.getDocumentElement().getElementsByTagName("helioviewer").item(0);
		if (root != null)
			printXMLData(root, 0);

		// set the xml data for the MetaDataDialog
		xmlDoc = doc;

		// set the export file name for MetaDataDialog
		outFileName = "";

		if (metaData.displayName != null)
			outFileName += metaData.displayName.replace(" ", "_") + " ";

		outFileName += metaData.localDateTime.format(Globals.FILE_DATE_TIME_FORMATTER) + ".fits.xml";
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
	private void printXMLData(Node node, int indent)
	{
		// get element name and value
		String nodeName = node.getNodeName();
		String nodeValue = getElementValue(node);

		switch (nodeName)
		{
			case "fits":
				addDataItem("-------------------------------");
				addDataItem("          FITS Header");
				addDataItem("-------------------------------");
				break;
			case "helioviewer":
				addDataItem("-------------------------------");
				addDataItem("      Helioviewer Header");
				addDataItem("-------------------------------");
				break;
			default:
				String tab = new String(new char[indent]).replace((char) 0, '\t');
				addDataItem(tab + nodeName + ": " + nodeValue);
				break;
		}

		// write the child nodes recursively
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++)
		{
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE)
				printXMLData(child, indent + 1);
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
	private String getElementValue(Node elem)
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
	 * @param _source
	 *            XML document to save
	 * @param _filename
	 *            XML file name
	 */
	private boolean exportXML(DOMSource _source, String _filename)
	{
		// open the output stream where XML Document will be saved
		try(FileOutputStream fos = new FileOutputStream(_filename))
		{
			// Use a Transformer for the purpose of output
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();

			// transform source into result will do a file save
			transformer.transform(_source, new StreamResult(fos));
			return true;
		}
		catch (Exception e)
		{
			Telemetry.trackException(e);
			return false;
		}
	}
}
