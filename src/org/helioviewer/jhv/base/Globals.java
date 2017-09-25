package org.helioviewer.jhv.base;

import java.awt.Desktop;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.Nullable;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;

import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.MainPanel;
import org.helioviewer.jhv.gui.PredefinedFileFilter;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Globals
{
	public static final String OBSERVATORIES_DATASOURCE = "https://api.helioviewer.org/v2/getDataSources/";
	public static final String JPX_DATASOURCE_TRADITIONAL = "https://api.helioviewer.org/v2/getJPX/";
	public static final String JPX_DATASOURCE_MIDPOINT = "https://api.helioviewer.org/v2/getJPXClosestToMidPoint/";

    public static final String VERSION = System.getProperty("jhvVersion") == null ? "developer" : System.getProperty("jhvVersion");
    public static final String RAYGUN_TAG = System.getProperty("raygunTag");
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter FILE_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH.mm.ss'Z'");
    
    public static final int LICENSE_VERSION = 1; //increase whenever license changes
	
    public static final int STARTUP_LAYER_ID = 10; //AIA 193
    
    public static final int CORES = Runtime.getRuntime().availableProcessors(); 

	public static final boolean JAVA_FX_AVAILABLE;
	
	private static LinkedBlockingQueue<Runnable> runnableWithGLContext;
	public static final ArrayList<Thread> GL_WORKER_THREADS=new ArrayList<>();
    
    private static LinkedBlockingQueue<JFileChooser> fileChooser;
	
	public static void createSharedGLContexts(GLAutoDrawable _master, GLDrawableFactory _factory, GLCapabilities _capabilities, int _numContexts)
	{
		if(runnableWithGLContext!=null)
			throw new IllegalStateException("Cannot create shared contexts twice");
		
		runnableWithGLContext = new LinkedBlockingQueue<>();
		for(int i=0;i<_numContexts;i++)
		{
			GLAutoDrawable slaveDrawable = _factory.createDummyAutoDrawable(null, true, _capabilities, null);
			slaveDrawable.setExclusiveContextThread(Thread.currentThread());
			
			final GLContext c=slaveDrawable.createContext(_master.getContext());
			Thread t=new Thread(() ->
			{
				c.makeCurrent();
				try
				{
					for(;;)
						runnableWithGLContext.take().run();
				}
				catch(InterruptedException e)
				{
				}
			});
			t.setName("GL Worker "+i);
			t.setDaemon(true);
			t.start();
			GL_WORKER_THREADS.add(t);
		}
	}
	
	public static void runWithGLContext(Runnable _r)
	{
		runnableWithGLContext.add(_r);
	}
    
    public static final boolean IS_RELEASE_VERSION = (RAYGUN_TAG!=null);
    public static final boolean IS_WINDOWS=System.getProperty("os.name").toUpperCase().contains("WIN");
    public static final boolean IS_LINUX=System.getProperty("os.name").toUpperCase().contains("LINUX");
    public static final boolean IS_OS_X=System.getProperty("os.name").toUpperCase().contains("MAC OS X");
	
	static
	{
		boolean javaFxAvailable = true;
		try
		{			
			Class.forName("com.sun.javafx.runtime.VersionInfo");
			
			if(SwingUtilities.isEventDispatchThread())
				new JFXPanel();
			else
				SwingUtilities.invokeAndWait(new Runnable()
				{
					@Override
					public void run()
					{
						new JFXPanel();
					}
				});
		}
		catch (ClassNotFoundException | InvocationTargetException | InterruptedException e)
		{
			javaFxAvailable = false;
			System.err.println("No JavaFX detected. Please install a Java 1.8 with JavaFX");
		}
		
		JAVA_FX_AVAILABLE = javaFxAvailable;

    	// FIXME: Linux java FX doesn't work reliably
		// see method showFileDialog!
		if(Globals.IS_LINUX || !JAVA_FX_AVAILABLE)
		{
			initFileChooserAsync();
		}
	}

    private Globals()
    {
    }
    
    /**
     * Opens the specified web page in the default web browser
     * 
     * @param url
     *            A web address (URL) of a web page (e.g
     *            "http://www.jhelioviewer.org/")
     */
    public static void openURL(String url)
    {
        try
        {
            Desktop.getDesktop().browse(new URI(url));
        }
        catch(Exception e)
        {
        	Telemetry.trackException(e);
            try
            {
                new ProcessBuilder("x-www-browser",url).start();
            }
            catch(Exception e2)
            {
            	Telemetry.trackException(e2);
            }
        }
    }
    
    public enum DialogType
    {
    	OPEN_FILE,
    	SAVE_FILE,
    	SELECT_DIRECTORY
    }
    
    @Nullable
    public static File showFileDialog(final DialogType _type, final String _title,
    		@Nullable final String _directory, final boolean _allowAllExtensions,
    		@Nullable final String _defaultName,    		
    		final PredefinedFileFilter... _filters)
    {
    	// FIXME: Linux java FX doesn't work reliably
		if (Globals.JAVA_FX_AVAILABLE && !Globals.IS_LINUX)
			try
			{
				final LinkedBlockingQueue<Stage> mainStage=new LinkedBlockingQueue<>();
				final LinkedBlockingQueue<Optional<File>> selectedFile=new LinkedBlockingQueue<>();
				
				MainFrame.SINGLETON.startWaitCursor();
				
				//idea stolen from http://stackoverflow.com/questions/28920758/javafx-filechooser-in-swing
				final JDialog modalBlocker = new JDialog();
		        modalBlocker.setModal(true);
		        modalBlocker.setUndecorated(true);
		        modalBlocker.setOpacity(0.0f);
		        modalBlocker.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		        modalBlocker.addFocusListener(new FocusListener()
				{
					@Override
					public void focusLost(@Nullable FocusEvent _e)
					{
					}
					
					@Override
					public void focusGained(@Nullable FocusEvent _e)
					{
						final Stage s = mainStage.peek();
						if(s!=null)
							Platform.runLater(new Runnable()
							{
								@Override
								public void run()
								{
									s.requestFocus();
								}
							});
					}
				});
				
		        Platform.setImplicitExit(false);
				Platform.runLater(new Runnable()
				{
					@Override
					public void run()
					{
						final Stage s;
						
						if(Globals.IS_OS_X)
						{
							// FIXME: OS_X: When a dialog is opened while the app is in Fullscreen-mode, the opacity doesn't work as intended
							
							// on macOS Sierra (V10.12.5), the released app crashes if the stage is set to UTILITY.
							// Apple AWT Internal Exception: utility panels cannot be fullscreen primary
							s=new Stage(StageStyle.UNDECORATED);
						}
						else
						{
							s=new Stage(StageStyle.UTILITY);
						}
						
						s.setOpacity(0);
						s.setWidth(MainFrame.SINGLETON.getWidth());
						s.setHeight(MainFrame.SINGLETON.getHeight());
						s.setX(MainFrame.SINGLETON.getX());
						s.setY(MainFrame.SINGLETON.getY());
						s.setScene(new Scene(new Group()));
						s.getScene().setCursor(javafx.scene.Cursor.WAIT);
						s.getScene().getRoot().setCursor(javafx.scene.Cursor.WAIT);
						s.show();
						
						mainStage.add(s);
						
						Platform.runLater(new Runnable()
						{
							@Override
							public void run()
							{
								try
								{
									switch(_type)
									{
										case OPEN_FILE:
										case SAVE_FILE:
											FileChooser fileChooser = new FileChooser();
											fileChooser.setTitle(_title);
											if(_directory!=null)
												fileChooser.setInitialDirectory(new File(_directory));
											
											for(PredefinedFileFilter f:_filters)
												fileChooser.getExtensionFilters().add(f.extensionFilter);
											
											if(_allowAllExtensions)
												fileChooser.getExtensionFilters().add(new ExtensionFilter("All Files (*.*)", "*.*"));
											
											if(_defaultName!=null)
												fileChooser.setInitialFileName(_defaultName);
											
											try
											{
												if(_type==DialogType.OPEN_FILE)
													selectedFile.put(Optional.ofNullable(fileChooser.showOpenDialog(s)));
												else
													selectedFile.put(Optional.ofNullable(fileChooser.showSaveDialog(s)));
											}
											catch(IllegalArgumentException _iae)
											{
												fileChooser.setInitialDirectory(null);
												if(_type==DialogType.OPEN_FILE)
													selectedFile.put(Optional.ofNullable(fileChooser.showOpenDialog(s)));
												else
													selectedFile.put(Optional.ofNullable(fileChooser.showSaveDialog(s)));
											}
											
											break;
										case SELECT_DIRECTORY:
											DirectoryChooser dirChooser=new DirectoryChooser();
											dirChooser.setTitle(_title);
											if(_directory!=null)
												dirChooser.setInitialDirectory(new File(_directory));
											
											try
											{
												selectedFile.add(Optional.ofNullable(dirChooser.showDialog(s)));
											}
											catch(IllegalArgumentException _iae)
											{
												dirChooser.setInitialDirectory(null);
												selectedFile.add(Optional.ofNullable(dirChooser.showDialog(s)));
											}
											break;
										default:
											throw new IllegalArgumentException("Illegal type: "+_type);
									}
									
									s.close();
								}
								catch (InterruptedException _e)
								{
									Telemetry.trackException(_e);
								}
								finally
								{
									SwingUtilities.invokeLater(new Runnable()
									{
										@Override
										public void run()
										{
											modalBlocker.setVisible(false);
											modalBlocker.dispose();
										}
									});
								}
							}
						});
					}
				});
				
				modalBlocker.setVisible(true);
				
				Optional<File> of=selectedFile.peek();
				File f=of==null ? null : of.orElse(null);
				if(f==null)
					return null;
				
				switch(_type)
				{
					case OPEN_FILE:
						if(f.isFile() && f.exists())
							return f;
						else
							return null;

					case SAVE_FILE:
						if(f.isDirectory())
							return null;
						
						if(!f.exists())
							return f;
						
		                switch (JOptionPane.showConfirmDialog(MainFrame.SINGLETON,
		                        "This file exists already, overwrite?", "Overwrite existing file",
		                        JOptionPane.YES_NO_CANCEL_OPTION))
		                {
			                case JOptionPane.YES_OPTION:
			                    return f;
			                case JOptionPane.CANCEL_OPTION:
			                    return null;
			                case JOptionPane.NO_OPTION:
			                	return showFileDialog(_type, _title, f.getParent(), _allowAllExtensions, f.getName(), _filters);
		                    default:
		                    	throw new RuntimeException();
		                }
						
					default:
						throw new RuntimeException();
				}
			}
			finally
			{
				MainFrame.SINGLETON.stopWaitCursor();
			}
    	
        try
        {
            JFileChooser instance=fileChooser.take();
            fileChooser.add(instance);
            
            instance.setDialogTitle(_title);
            instance.setFileHidingEnabled(false);
            instance.setMultiSelectionEnabled(false);
            
        	instance.setSelectedFile(_defaultName==null ? null:new File(_defaultName));
            instance.setCurrentDirectory(_directory==null ? null:new File(_directory));
            

            instance.resetChoosableFileFilters();
            switch(_type)
            {
            	case OPEN_FILE:
                    instance.setDialogType(JFileChooser.OPEN_DIALOG);
                    instance.setAcceptAllFileFilterUsed(_allowAllExtensions);
                    instance.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    for(FileFilter f:_filters)
                    	instance.addChoosableFileFilter(f);
                    if(_filters.length>0)
                    	instance.setFileFilter(_filters[0]);
                    if(_defaultName!=null)
        	            for(FileFilter f:_filters)
        	            	 if(f.accept(new File(_defaultName)))
        	            	 {
        	            		 instance.setFileFilter(f);
        	            		 break;
        	            	 }
                    if(instance.showOpenDialog(MainFrame.SINGLETON.MAIN_PANEL)==JFileChooser.APPROVE_OPTION)
                    {
                    	File f=instance.getSelectedFile();
    					if(f.isFile() && f.exists())
    						return f;
    					else
    						return null;
                    }
                    else
                    	return null;

            	case SAVE_FILE:
                    instance.setDialogType(JFileChooser.SAVE_DIALOG);
                    instance.setAcceptAllFileFilterUsed(false);
                    instance.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    for(FileFilter f:_filters)
                    	instance.addChoosableFileFilter(f);
                    if(_filters.length>0)
                    	instance.setFileFilter(_filters[0]);
                    if(_defaultName!=null)
        	            for(FileFilter f:_filters)
        	            	 if(f.accept(new File(_defaultName)))
        	            	 {
        	            		 instance.setFileFilter(f);
        	            		 break;
        	            	 }
                    if(instance.showSaveDialog(MainFrame.SINGLETON.MAIN_PANEL)==JFileChooser.APPROVE_OPTION)
                    {
                    	FileFilter selectedFilter = instance.getFileFilter();
                    	File f=instance.getSelectedFile();
                    	
                    	if(!selectedFilter.accept(f))
                    	{
                    		if(selectedFilter instanceof PredefinedFileFilter)
							{
                    			PredefinedFileFilter filter = (PredefinedFileFilter)selectedFilter;
								f = new File(f.getPath() + filter.getDefaultExtension());
							}
                    	}
                    	
    					if(f.isDirectory())
    						return null;
    					
    					if(!f.exists())
    						return f;
    					
    	                switch (JOptionPane.showConfirmDialog(MainFrame.SINGLETON,
								"This file exists already, overwrite?", "Overwrite existing file",
    	                        JOptionPane.YES_NO_CANCEL_OPTION))
    	                {
    		                case JOptionPane.YES_OPTION:
    		                    return f;
    		                case JOptionPane.CANCEL_OPTION:
    		                    return null;
    		                case JOptionPane.NO_OPTION:
    		                	return showFileDialog(_type, _title, f.getParent(), _allowAllExtensions, f.getName(), _filters);
    	                    default:
    	                    	throw new RuntimeException();
    	                }
                    }
                    else
                    	return null;

            	case SELECT_DIRECTORY:
                    instance.setDialogType(JFileChooser.OPEN_DIALOG);
                    instance.setAcceptAllFileFilterUsed(true);
                    instance.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    if(instance.showOpenDialog(MainFrame.SINGLETON.MAIN_PANEL)==JFileChooser.APPROVE_OPTION)
                    {
                    	File f=instance.getSelectedFile();
                    	if(f.isDirectory() && f.exists())
                    		return f;
                    	else
                    		return null;
                    }
                    else
                    	return null;
                    
        		default:
        			throw new RuntimeException();
            }
        }
        catch(InterruptedException e)
        {
        	Telemetry.trackException(e);
            return null;
        }
    }
    
    public static void initFileChooserAsync()
    {
    	fileChooser=new LinkedBlockingQueue<>();
        Thread t=new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                fileChooser.add(new JFileChooser());
                
                new FileChooser();
            }
        });
        
        t.setDaemon(true);
        t.start();
    }
    
	@Nullable
	public static String loadFile(String _resourcePath)
	{
		StringBuilder contents = new StringBuilder();
		String line;

		try (BufferedReader br = new BufferedReader(new InputStreamReader(
				MainPanel.class.getResourceAsStream(_resourcePath), StandardCharsets.UTF_8)))
		{
			while ((line = br.readLine()) != null)
				contents.append(line).append("\n");
			
			return contents.toString();
		}
		catch (IOException e)
		{
			Telemetry.trackException(e);
		}
		return null;
	}
}
