package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.helioviewer.jhv.base.Globals;
import org.helioviewer.jhv.base.Globals.DialogType;
import org.helioviewer.jhv.base.Settings;
import org.helioviewer.jhv.base.Settings.StringKey;
import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.base.math.MathUtils;
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

/**
 * Dialog that is used to display meta data for an image.
 */
public class MetaDataDialog extends JDialog implements TimeLineListener, LayerListener
{
	private final JButton closeButton = new JButton("Close");
	private final JButton exportFitsButton = new JButton("Export FITS header as XML");

	private JTextArea listBox = new JTextArea();
	private @Nullable String outFileName;
	JScrollPane listScroller;

	private @Nullable Document xmlDoc;
	private final JPanel panel = new JPanel();

	public MetaDataDialog()
	{
		super(MainFrame.SINGLETON, "Image metadata");

		Telemetry.trackEvent("Dialog", "Type", getClass().getSimpleName());

		getContentPane().setLayout(new BorderLayout());

		JPanel bottomPanel = new JPanel();
		FlowLayout fl_bottomPanel = new FlowLayout(FlowLayout.RIGHT);
		fl_bottomPanel.setVgap(10);
		fl_bottomPanel.setHgap(10);
		bottomPanel.setLayout(fl_bottomPanel);
		bottomPanel.add(exportFitsButton);
		// bottomPanel.add(exportButton);
		bottomPanel.add(closeButton);
		panel.setBorder(new EmptyBorder(10, 10, 0, 10));
		
		getContentPane().add(panel, BorderLayout.CENTER);
				panel.setLayout(new BorderLayout(0, 0));
		
				listBox.setLineWrap(true);
				listBox.setEditable(false);
				listBox.setWrapStyleWord(true);

		listScroller = new JScrollPane(listBox);
		panel.add(listScroller);
		getContentPane().add(bottomPanel, BorderLayout.PAGE_END);

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

				DOMSource source = new DOMSource(xmlDoc.getDocumentElement().getElementsByTagName("fits").item(0));
				if (!exportXML(source, file.getPath() + outFileName))
					JOptionPane.showMessageDialog(MetaDataDialog.this, "Could not save document.");
			}
		});

		// exportButton.addActionListener(this);
		Layers.addLayerListener(this);
		TimeLine.SINGLETON.addListener(this);
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

		setSize(649, 719);

		setLocationRelativeTo(MainFrame.SINGLETON);
		setVisible(true);
	}
	
	@Override
	public void dispose()
	{
		super.dispose();
		Layers.removeLayerListener(this);
		TimeLine.SINGLETON.removeListener(this);
	}
	
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
	
	@Override
	public void timeStampChanged(long current, long last)
	{
		updateData();
	}

	@Override
	public void isPlayingChanged(boolean _isPlaying)
	{
	}

	@Override
	public void timeRangeChanged(long _start, long _end)
	{
	}

	private void updateData()
	{
		final Point oldPoint = listScroller.getViewport().getViewPosition();
		
		data.setLength(0);
		exportFitsButton.setEnabled(false);
		
		ImageLayer il = Layers.getActiveImageLayer();
		if (il != null)
		{
			MetaData md = il.getMetaData(TimeLine.SINGLETON.getCurrentTimeMS());
			Document doc = il.getMetaDataDocument(TimeLine.SINGLETON.getCurrentTimeMS());

			if (md != null && doc != null)
			{
				readData(md, doc);
				exportFitsButton.setEnabled(true);
			}
			else
				addLine("Metadata not available.");
		}
		
		listBox.setText(data.toString());
		
		SwingUtilities.invokeLater(() -> listScroller.getViewport().setViewPosition(oldPoint));
	}
	
	private StringBuilder data=new StringBuilder();

	private void addLine(String _item)
	{
		if(data.length()>0)
			data.append('\n');

		data.append(_item);
	}

	private void readData(@Nonnull MetaData metaData, @Nonnull Document doc)
	{
		addLine("-------------------------------");
		addLine("       Basic Information       ");
		addLine("-------------------------------");
		addLine("Observatory : " + metaData.observatory);
		addLine("Instrument  : " + metaData.instrument);
		addLine("Detector    : " + metaData.detector);
		addLine("Measurement : " + metaData.measurement);
		addLine("Date        : " + MathUtils.toLDT(metaData.timeMS).toLocalDate());
		addLine("Time        : " + MathUtils.toLDT(metaData.timeMS).toLocalTime());

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

		outFileName += MathUtils.toLDT(metaData.timeMS).format(Globals.FILE_DATE_TIME_FORMATTER) + ".fits.xml";
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
				addLine("-------------------------------");
				addLine("          FITS Header");
				addLine("-------------------------------");
				break;
			case "helioviewer":
				addLine("-------------------------------");
				addLine("      Helioviewer Header");
				addLine("-------------------------------");
				break;
			default:
				String tab = new String(new char[indent]).replace((char) 0, '\t');
				addLine(tab + nodeName + ": " + nodeValue);
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
