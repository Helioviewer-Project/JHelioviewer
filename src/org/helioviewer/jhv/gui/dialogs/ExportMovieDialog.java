package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.jhv.viewmodel.view.ComponentView;
import org.helioviewer.jhv.viewmodel.view.LinkedMovieManager;
import org.helioviewer.jhv.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.jhv.viewmodel.view.jp2view.JHVJPXView.SpeedType;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;


public class ExportMovieDialog implements ActionListener{
	private JHVJPXView timedJHVJPXView = null;
	private ComponentView mainComponentView = null;
	
	private long speed = 0;
	private IMediaWriter writer;

	private MovieFileFilter selectedOutputFormat = MovieFileFilter.ImplementedMovieFilter.MP4.getMovieFilter();
	
	private String txtTargetFile;
	private ProgressDialog progressDialog;
	
	private boolean started = false;
	
	private String directory;
	private String filename;
	private Timer timer;
	private int i = 0;
	
	private FileOutputStream fileOutputStream;
	private ZipOutputStream zipOutputStream;

    private static final String SETTING_MOVIE_EXPORT_LAST_DIRECTORY = "export.movie.last.directory";

    private static final String SETTING_IMG_WIDTH = "export.movie.image.width";
    private static final String SETTING_IMG_HEIGHT = "export.movie.image.height";
    private static final String SETTING_USE_CURRENT_OPENGL_SIZE = "export.movie.use.current.opengl.size";

    private boolean useCurrentOpenGlSize;
    private int imageWidth;
    private int imageHeight;
    private ExportMovieDialog exportMovieDialog;
    
	public ExportMovieDialog() {
		exportMovieDialog = this;
		if (openFileChooser() == JFileChooser.APPROVE_OPTION) {
			this.loadSettings();
		    Settings settings = Settings.getSingletonInstance();
	        settings.setProperty(SETTING_MOVIE_EXPORT_LAST_DIRECTORY, directory);
	        settings.save();
	        ImageViewerGui.getMainFrame().setEnabled(false);
			
	        
	        progressDialog = new ProgressDialog(exportMovieDialog);
			progressDialog.setVisible(true);
	        
	        i = 0;
			
	        this.initExportMovie();
			timer = new Timer(0, this);
			timer.start();
			//exportMovie();	
					
		}
	}

	private int openFileChooser(){
		//txtTargetFile = new String(JHVDirectory.EXPORTS.getPath() + "JHV_movie_created_");
		txtTargetFile = new String();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
        txtTargetFile += dateFormat.format(new Date());
        txtTargetFile += selectedOutputFormat.getExtension();

        // Open save-dialog
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileHidingEnabled(false);
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setAcceptAllFileFilterUsed(false);
        
        Settings settings = Settings.getSingletonInstance();
        String val;
        try {
            val = settings.getProperty(SETTING_MOVIE_EXPORT_LAST_DIRECTORY);
            if (val != null && !(val.length() == 0)) {
                fileChooser.setCurrentDirectory(new File(val));
            }
        } catch (Throwable t) {
            Log.error(t);
        }
        
        // add Filter
        for (MovieFileFilter.ImplementedMovieFilter movieFilter : MovieFileFilter.ImplementedMovieFilter.values()){
        	fileChooser.addChoosableFileFilter(movieFilter.getMovieFilter());
        }

        // if txtTargetFile's set the selectedOutputFormat and fileChooser's
        // filter according to txtTargetFile's extension
        
        for (FileFilter fileFilter : fileChooser.getChoosableFileFilters()) {
                if (txtTargetFile.endsWith(((MovieFileFilter) fileFilter).getExtension())) {
                    fileChooser.setFileFilter(fileFilter);
                    selectedOutputFormat = (MovieFileFilter) fileFilter;
                }
            }
        
        txtTargetFile = txtTargetFile.substring(0, txtTargetFile.lastIndexOf(selectedOutputFormat.getExtension()));

        fileChooser.setSelectedFile(new File(txtTargetFile));

        int retVal = fileChooser.showDialog(ImageViewerGui.getMainFrame(), "OK");
        selectedOutputFormat = (MovieFileFilter) fileChooser.getFileFilter();
        directory = fileChooser.getCurrentDirectory().getPath() + "/";
        filename = fileChooser.getSelectedFile().getName();
        
        for (FileFilter fileFilter : fileChooser.getChoosableFileFilters()){
        	if (txtTargetFile.endsWith(((MovieFileFilter) fileFilter).getExtension())) {
        		selectedOutputFormat = (MovieFileFilter)fileFilter;
                filename = filename.substring(0, filename.lastIndexOf(selectedOutputFormat.getExtension()));
                return retVal;
            }
        }
        
        return retVal;
	}
	
	
	private void loadSettings(){
		Settings settings = Settings.getSingletonInstance();
        String val;  
        
        try {
            val = settings.getProperty(SETTING_USE_CURRENT_OPENGL_SIZE);
            if (val != null && !(val.length() == 0)) {
                useCurrentOpenGlSize = Boolean.parseBoolean(val);
            }
        } catch (Throwable t) {
            Log.error(t);
        }

        
        try {
            val = settings.getProperty(SETTING_IMG_HEIGHT);
            if (val != null && !(val.length() == 0)) {
                this.imageHeight = Integer.parseInt(val);
            }
        } catch (Throwable t) {
            Log.error(t);
        }
        
        try {
            val = settings.getProperty(SETTING_IMG_WIDTH);
            if (val != null && !(val.length() == 0)) {
            	this.imageWidth = Integer.parseInt(val);
            }
        } catch (Throwable t) {
            Log.error(t);
        }
        
        

      //default settings if nothing was specified so far
      if(imageWidth == 0)
        imageWidth = 1280;

      if(imageHeight == 0)
        imageHeight = 720;
	}
	
	
	private void initExportMovie(){
		mainComponentView = GuiState3DWCS.mainComponentView;
		//mainComponentView.stop();
		timedJHVJPXView = LinkedMovieManager.getActiveInstance().getMasterMovie();
		started = true;
		
		if (this.selectedOutputFormat.isMovieFile()){
			
			writer = ToolFactory.makeWriter(directory + filename + this.selectedOutputFormat.getExtension());
			
			if (timedJHVJPXView.getSpeedType() == SpeedType.RELATIV){
				speed = 1000/timedJHVJPXView.getDesiredSpeed();
			}
			
			else {
				long min = timedJHVJPXView.getFrameDateTime(0).getMillis();
				long max = timedJHVJPXView.getFrameDateTime(1).getMillis();
				speed = (max - min) / timedJHVJPXView.getDesiredSpeed() ;	
			}
			if (this.useCurrentOpenGlSize) {
				Dimension dimension = this.mainComponentView.getCanavasSize();
				this.imageWidth = dimension.width;
				this.imageHeight = dimension.height;
			}
			writer.addVideoStream(0, 0, this.selectedOutputFormat.getCodec(),
	                this.imageWidth, this.imageHeight);
		}

		else if (this.selectedOutputFormat.isCompressedFile()){
			try {
				fileOutputStream = new FileOutputStream(this.directory + this.filename + this.selectedOutputFormat.getExtension());
				zipOutputStream = new ZipOutputStream(fileOutputStream);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		else if (this.selectedOutputFormat.isImageFile()){
			File dir = new File(this.directory+this.filename);
			dir.mkdir();
			directory += this.filename + "/";
		}
		
		
		if (timedJHVJPXView != null)
			progressDialog.setMaximumOfProgressBar(timedJHVJPXView.getMaximumFrameNumber());
	}
	
	
	private void exportMovie(){
		/*mainComponentView = (ComponentView)ImageViewerGui.getSingletonInstance().getMainView();
		//mainComponentView.stop();
		timedJHVJPXView = LinkedMovieManager.getActiveInstance().getMasterMovie();
		started = true;
		
		if (this.selectedOutputFormat.isMovieFile()){
			
			writer = ToolFactory.makeWriter(directory + filename + this.selectedOutputFormat.getExtension());
			
			if (timedJHVJPXView.getSpeedType() == SpeedType.RELATIV){
				speed = 1000/timedJHVJPXView.getDesiredSpeed();
			}
			
			else {
				long min = timedJHVJPXView.getFrameDateTime(0).getMillis();
				long max = timedJHVJPXView.getFrameDateTime(1).getMillis();
				speed = (max - min) / timedJHVJPXView.getDesiredSpeed() ;	
			}
			if (this.useCurrentOpenGlSize) {
				Dimension dimension = this.mainComponentView.getCanavasSize();
				this.imageWidth = dimension.width;
				this.imageHeigth = dimension.height;
			}
			writer.addVideoStream(0, 0, this.selectedOutputFormat.getCodec(),
	                this.imageWidth, this.imageHeigth);
		}

		else if (this.selectedOutputFormat.isCompressedFile()){
			try {
				fileOutputStream = new FileOutputStream(this.directory + this.filename + this.selectedOutputFormat.getExtension());
				zipOutputStream = new ZipOutputStream(fileOutputStream);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		else if (this.selectedOutputFormat.isImageFile()){
			File dir = new File(this.directory+this.filename);
			dir.mkdir();
			directory += this.filename + "/";
		}
		
		
		if (timedJHVJPXView != null){
			progressDialog.setMaximumOfProgressBar(timedJHVJPXView.getMaximumFrameNumber());

		*/
			//for (i = 0; i < timedJHVJPXView.getMaximumFrameNumber(); i++){
				
				//if (!started) break;
		if (!started) stopExportMovie();
				else{
				timedJHVJPXView.setCurrentFrame(i, new ChangeEvent());
				
				BufferedImage bufferedImage = mainComponentView.getBufferedImage(imageWidth, imageHeight);
				
				progressDialog.updateProgressBar(i);
				
				if (this.selectedOutputFormat.isMovieFile() && started) {
					writer.encodeVideo(0, bufferedImage, speed * i , TimeUnit.MILLISECONDS);	
				}
				
				else if (this.selectedOutputFormat.isCompressedFile()  && started){
					String number = String.format("%04d", i);  
					try {
						zipOutputStream.putNextEntry(new ZipEntry(filename + "/" + this.filename + "-" + number + this.selectedOutputFormat.getInnerMovieFilter().getExtension()));
						ImageIO.write(bufferedImage, this.selectedOutputFormat.getInnerMovieFilter().getFileType(), zipOutputStream);
						zipOutputStream.closeEntry();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				else if (this.selectedOutputFormat.isImageFile()  && started) {
					String number = String.format("%04d", i);  
					try {
						ImageIO.write(bufferedImage, selectedOutputFormat.getFileType(),  new File(directory + this.filename + this.filename + "-" + number + this.selectedOutputFormat.getExtension()));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				i++;
					if (i > timedJHVJPXView.getMaximumFrameNumber()){
					started = false;
					stopExportMovie();
				}
				}

			//}
		
				/*
			this.timedJHVJPXView.setCurrentFrame(0, new ChangeEvent());
			// export movie
			if (selectedOutputFormat.isMovieFile()) writer.close();
			else if (selectedOutputFormat.isCompressedFile()){
				try {
					zipOutputStream.close();
					fileOutputStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			progressDialog.dispose();
			ImageViewerGui.getMainFrame().setEnabled(true);
			
			mainComponentView.start();*/
			
			
		//}
	}
	
	public void stopExportMovie(){
		this.timedJHVJPXView.setCurrentFrame(0, new ChangeEvent());
		// export movie
		if (selectedOutputFormat.isMovieFile()) writer.close();
		else if (selectedOutputFormat.isCompressedFile()){
			try {
				zipOutputStream.close();
				fileOutputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		progressDialog.dispose();
		ImageViewerGui.getMainFrame().setEnabled(true);
		timer.stop();
		mainComponentView.start();
	}
	
	public void cancelMovie() {
		started = false;
	}
	


	public class ProgressDialog extends JDialog implements ActionListener{
		
    private static final long serialVersionUID=-488930636247393662L;
    private JProgressBar progressBar;
		private JButton btnCancel;
		private ExportMovieDialog exportMovieDialog;
		private final JPanel contentPanel = new JPanel();
		
		public ProgressDialog(ExportMovieDialog exportMovieDialog) {
			setAlwaysOnTop(true);
			this.exportMovieDialog = exportMovieDialog;
			setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			setResizable(false);
			setTitle("Movie export");
			setBounds(100, 100, 450, 300);
						
			getContentPane().setLayout(new BorderLayout());
			contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
			
			getContentPane().add(contentPanel, BorderLayout.CENTER);
			contentPanel.setLayout(new BorderLayout(0, 0));
			
			{
				JLabel lblMovieExportIs = new JLabel("Movie export is running");
				contentPanel.add(lblMovieExportIs, BorderLayout.NORTH);
			}
			{
				progressBar = new JProgressBar();
				contentPanel.add(progressBar, BorderLayout.CENTER);
			}
			
			{
				JPanel buttonPane = new JPanel();
				buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
				getContentPane().add(buttonPane, BorderLayout.SOUTH);
				{
					btnCancel = new JButton("Cancel");
					buttonPane.add(btnCancel);
					btnCancel.addActionListener(this);
				}
			}
			
			this.pack();
			this.setLocationRelativeTo(ImageViewerGui.getMainFrame());
			
		}
		
		public void setMaximumOfProgressBar(int maximum){
			this.progressBar.setMaximum(maximum);
		}
		
		public void updateProgressBar(int value){
			this.progressBar.setValue(value);
		}
		
		@Override
		public void dispose() {
			ImageViewerGui.getMainFrame().setEnabled(true);
			super.dispose();
		}

		@Override
		public void actionPerformed(ActionEvent ae) {
			if (ae.getSource() == btnCancel){
				this.exportMovieDialog.cancelMovie();
				dispose();
			}
			
		}
	}



	@Override
	public void actionPerformed(ActionEvent arg0) {
		exportMovie();
	}
	
	



}


