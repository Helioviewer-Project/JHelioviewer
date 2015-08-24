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

import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.helioviewer.jhv.JHVException.MetaDataException;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.actions.filefilters.FileFilter;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;
import org.helioviewer.jhv.layers.AbstractLayer;
import org.helioviewer.jhv.layers.LayerListener;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine.TimeLineListener;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Dialog that is used to display meta data for an image.
 * 
 * @author Alen Agheksanterian
 * @author Stephan Pagel
 */
public class MetaDataDialog extends JDialog implements ActionListener,
		ShowableDialog, LayerListener, TimeLineListener {

	private static final long serialVersionUID = 1L;

	private final JButton closeButton = new JButton("Close");
	private final JButton exportFitsButton = new JButton(
			"Export FITS header as XML");

	private List<String> infoList = new ArrayList<String>();
	private JList<String> listBox = new JList<String>();
	private boolean metaDataOK;
	private String outFileName;
	JScrollPane listScroller;

	private Document xmlDoc;
		private static final String LAST_DIRECTORY = "metadata.save.lastPath";
	/**
	 * The private constructor that sets the fields and the dialog.
	 */
	public MetaDataDialog() {
		super(MainFrame.SINGLETON, "Image metainfo");
		setLayout(new BorderLayout());
		setResizable(false);

		listBox.setFont(new Font("Courier", Font.PLAIN, 12));

		listBox.setCellRenderer(new ListCellRenderer<Object>() {
			public Component getListCellRendererComponent(JList<?> list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
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
		closeButton.addActionListener(this);
		exportFitsButton.addActionListener(this);
		// exportButton.addActionListener(this);
		pack();
		Layers.addNewLayerListener(this);
		TimeLine.SINGLETON.addListener(this);
		setLocationRelativeTo(MainFrame.SINGLETON);
		
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				if (Layers.getActiveImageLayer() != null){
				try {
					setMetaData(Layers.getActiveImageLayer().getMetaData(TimeLine.SINGLETON.getCurrentDateTime()));
				} catch (MetaDataException e1) {
					resetData();
					addDataItem(e1.getMessage());
				}}
				else{
					resetData();
				}
			}
		});
		
		getRootPane().registerKeyboardAction(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	/**
	 * Resets the list.
	 */
	public void resetData() {
		infoList.clear();

		// update the listBox
		listBox.setListData(infoList.toArray(new String[0]));

		// set the status of export button
		if (!metaDataOK) {
			exportFitsButton.setEnabled(false);
		} else {
			exportFitsButton.setEnabled(true);
		}
	}

	/**
	 * Adds a data item to the list
	 * 
	 * @param _item
	 *            New item to add
	 * @see #setMetaData(MetaDataView)
	 */
	public void addDataItem(String _item) {
		infoList.add(_item);

		// update the listBox
		listBox.setListData(infoList.toArray(new String[0]));
	}

	/**
	 * {@inheritDoc}
	 */
	public void showDialog() {
		pack();
		setSize(450, 600);

		setLocationRelativeTo(MainFrame.SINGLETON);
		setVisible(true);
	}

	/**
	 * {@inheritDoc}
	 */
	public void actionPerformed(ActionEvent _a) {
		if (_a.getSource() == closeButton) {
			infoList.clear();
			resetData();
			dispose();

		} else if (_a.getSource() == exportFitsButton) {
			if (JHVGlobals.isFXAvailable()){
				openFileChooserFX(outFileName);
			}
			else {
				openFileChooser(outFileName);
			}
		}
	}
	
	private void saveFits(String fn){
			DOMSource source = new DOMSource(xmlDoc.getDocumentElement()
					.getElementsByTagName("fits").item(0));
			
			boolean saveSuccessful = saveXMLDocument(source, fn
					+ outFileName);
			if (!saveSuccessful)
				JOptionPane.showMessageDialog(this,
						"Could not save document.");
	}

	private void openFileChooserFX(final String fileName){
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle("Save metadata");
				fileChooser.setInitialFileName(fileName);
				String val = Settings.getProperty(LAST_DIRECTORY);
				if (val != null){
					File lastPath = new File(val);
					if (lastPath.exists())
				fileChooser.setInitialDirectory(new File(Settings
						.getProperty("default.local.path")));
				}
				
				fileChooser.getExtensionFilters().addAll(FileFilter.IMPLEMENTED_FILE_FILTER.XML.getFileFilter().getExtensionFilter());
				final File selectedFile = fileChooser
						.showSaveDialog(new Stage());
				
				if (selectedFile != null) {
					// remember the current directory for future
					Settings.setProperty(LAST_DIRECTORY,
							selectedFile.getParent());
					saveFits(selectedFile.toString());
				}
			}

		});
	}
	
	private void openFileChooser(String _filename) {
		// Open save-dialog
		final JFileChooser fileChooser = JHVGlobals.getJFileChooser();
		fileChooser.setDialogTitle("Save metadata");
		fileChooser.setFileHidingEnabled(false);
		fileChooser.setMultiSelectionEnabled(false);
		fileChooser.setAcceptAllFileFilterUsed(true);
		fileChooser.setFileFilter(FileFilter.IMPLEMENTED_FILE_FILTER.XML.getFileFilter());
		String val = Settings.getProperty(LAST_DIRECTORY);
		String fileName = "";
		if (val != null){
			File lastPath = new File(val);
			if (lastPath.exists())
			fileName += val + "/";
		}

		fileChooser.setSelectedFile(new File(fileName + _filename));
		
		int retVal = fileChooser.showDialog(MainFrame.SINGLETON, "OK");
		if (retVal == JFileChooser.APPROVE_OPTION)
			saveFits(fileChooser.getSelectedFile().getPath());
	}

	/**
	 * Sets the full document which can be found reading the given MetaDataView.
	 * 
	 * @param metaDataView
	 *            Source to read
	 * @see #addDataItem(String)
	 */
	public void setMetaData(MetaData metaData) {
		if (metaData == null) return;
		metaDataOK = true;
		resetData();
		addDataItem("-------------------------------");
		addDataItem("       Basic Information       ");
		addDataItem("-------------------------------");
		addDataItem("Observatory : " + metaData.getObservatory());
		addDataItem("Instrument  : " + metaData.getInstrument());
		addDataItem("Detector    : " + metaData.getDetector());
		addDataItem("Measurement : " + metaData.getMeasurement());
		addDataItem("Date        : " + metaData.getLocalDateTime());
		addDataItem("Time        : " + metaData.getLocalDateTime());

		//Document doc = metaData.getDocument();
		Document doc = null;
		if (doc == null) return;
		// Send xml data to meta data dialog box
		Node root = doc.getDocumentElement().getElementsByTagName("fits")
				.item(0);
		writeXMLData(root, 0);
		root = doc.getDocumentElement().getElementsByTagName("helioviewer")
				.item(0);
		if (root != null) {
			writeXMLData(root, 0);
		}

		// set the xml data for the MetaDataDialog
		xmlDoc = doc;

		// set the export file name for
		// MetaDataDialog

		outFileName = metaData.getFullName().replace(" ", "_")
				+ " "
				+ metaData.getLocalDateTime().format(
						JHVGlobals.FILE_DATE_TIME_FORMATTER) + ".fits.xml";

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
	private void writeXMLData(Node node, int indent) {
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
		} else {

			String tab = "";
			for (int i = 0; i < indent; i++) {
				tab = tab + "\t";
			}

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
	private final String getElementValue(Node elem) {
		Node child;
		if (elem != null) {
			if (elem.hasChildNodes()) {
				for (child = elem.getFirstChild(); child != null; child = child
						.getNextSibling()) {
					if (child.getNodeType() == Node.TEXT_NODE) {
						return child.getNodeValue();
					}
				}
			}
		}
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
		try {
			fos = new FileOutputStream(xmlOutFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		try {
			// Use a Transformer for the purpose of output
			TransformerFactory transformerFactory = TransformerFactory
					.newInstance();
			try {
				transformer = transformerFactory.newTransformer();
			} catch (TransformerConfigurationException e) {
				e.printStackTrace();
				return false;
			}

			// The source is the fits header

			// The destination for output
			StreamResult result = new StreamResult(fos);

			// transform source into result will do a file save
			try {
				transformer.transform(source, result);
			} catch (TransformerException e) {
				e.printStackTrace();
			}
			return true;
		} finally {
			try {
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

	@Override
	public void timeStampChanged(LocalDateTime current, LocalDateTime last) {
		if (Layers.getActiveImageLayer() != null){
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					try {
						setMetaData(Layers.getActiveImageLayer().getMetaData(TimeLine.SINGLETON.getCurrentDateTime()));
					} catch (MetaDataException e) {
						resetData();
						addDataItem(e.getMessage());
					}
				}
			});
		}
		else {
			resetData();
		}
	}

	@Override
	public void dateTimesChanged(int framecount) {
		// TODO Auto-generated method stub

	}

	@Override
	public void newlayerAdded() {
	}

	@Override
	public void newlayerRemoved(int idx) {
		// TODO Auto-generated method stub

	}

	@Override
	public void activeLayerChanged(AbstractLayer layer) {
		// setMetaData(layer.getMetaData());
	}
}
