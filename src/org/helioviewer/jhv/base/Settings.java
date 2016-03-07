package org.helioviewer.jhv.base;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Settings
{
    private static final Properties DEFAULT_PROPERTIES = new Properties();
	private static final Preferences PREF_NODE = Preferences.userRoot().node("jhelioviewer");
    
    static
    {
        if(PREF_NODE.get(StringKey.UUID.key,null)==null)
        {
            PREF_NODE.put(StringKey.UUID.key,UUID.randomUUID().toString());
            try
            {
                PREF_NODE.flush();
            }
            catch(BackingStoreException e)
            {
            	e.printStackTrace();
            }
        }
        
        //load defaults
        DEFAULT_PROPERTIES.clear();
        try (InputStream defaultPropStream = Settings.class.getResourceAsStream("/defaults.properties"))
        {
            DEFAULT_PROPERTIES.load(defaultPropStream);
            
            defaultPropStream.close();
        }
        catch (Exception ex)
        {
        	ex.printStackTrace();
        }
        
        //check whether a default is defined for all known settings
        //check whether duplicate settings keys are defined
        String duplicateKey=null;
        HashSet<String> keys=new HashSet<>();
        for(IntKey k:IntKey.values())
        {
        	try
        	{
        		Integer.parseInt(DEFAULT_PROPERTIES.getProperty(k.key));
        	}
        	catch(NumberFormatException _nfe)
        	{
        		throw new NumberFormatException("Property "+k.key+" should have an integer as default.");
        	}
        	if(!keys.add(k.key))
        		duplicateKey=k.key;
        }
        for(BooleanKey k:BooleanKey.values())
        {
        	try
        	{
	        	int x=Integer.parseInt(DEFAULT_PROPERTIES.getProperty(k.key));
	        	if(x!=0 && x!=1)
	        		throw new NumberFormatException();
        	}
        	catch(NumberFormatException _nfe)
        	{
        		throw new NumberFormatException("Property "+k.key+" should have default of 0 or 1.");
        	}
        	
        	if(!keys.add(k.key))
        		duplicateKey=k.key;
        }
        for(StringKey k:StringKey.values())
        	if(!keys.add(k.key))
        		duplicateKey=k.key;
        if(duplicateKey!=null)
        	throw new RuntimeException("Duplicate key "+duplicateKey);
    }
    
    private Settings()
    {
    }

    public static void resetAllSettings()
    {
    	int prevLicense = Settings.getInt(IntKey.STARTUP_LICENSE_SHOWN);
    	
    	try
    	{
	        PREF_NODE.clear();
    	}
    	catch(BackingStoreException _bse)
    	{
    		Telemetry.trackException(_bse);
    	}
    	
    	Settings.setInt(IntKey.STARTUP_LICENSE_SHOWN, prevLicense);
        delayedFlush();
    }
    
    public static void init()
    {
    	//forces execution of static initializer
    }

	public static void setBoolean(BooleanKey _key, boolean _val)
	{
		setBoolean(_key,null,_val);
	}

    public static void setBoolean(BooleanKey _key, @Nullable String _param, boolean _val)
    {
		setString(_key.key+(_param==null?"":"."+_param),_val?"1":"0");
    }

	public static boolean getBoolean(BooleanKey _key)
	{
		return getBoolean(_key,null);
	}

	public static boolean getDefaultBoolean(BooleanKey _key)
	{
		return getDefaultBoolean(_key,null);
	}

    public static boolean getDefaultBoolean(BooleanKey _key,@Nullable String _param)
    {
        final String v=getDefaultString(_key.key + (_param==null ? "":"."+_param));
        try
        {
            return Integer.parseInt(v)!=0;
        }
        catch(NumberFormatException _nfe)
        {
            Telemetry.trackException(new NumberFormatException("Settings: Cannot parse \""+v+"\" for key \""+_key.key+"."+_param+"\"."));
            return false;
        }
    }
    
    public static boolean getBoolean(BooleanKey _key,@Nullable String _param)
    {
        final String v=getString(_key.key + (_param==null ? "":"."+_param));
        try
        {
            return Integer.parseInt(v)!=0;
        }
        catch(NumberFormatException _nfe)
        {
            Telemetry.trackException(new NumberFormatException("Settings: Cannot parse \""+v+"\" for key \""+_key.key+"."+_param+"\"."));
            return false;
        }
    }
    
    public static void setInt(IntKey _key, int _val)
    {
    	setString(_key.key,Integer.toString(_val));
    }

    public enum BooleanKey
    {
        CACHE_LOADING_CRASHED("cache.loading.crashed"),
        STARTUP_LOADMOVIE("startup.loadmovie"),
		PLUGIN_VISIBLE("plugin.visible"),
		MOVIE_TEXT("export.movie.text"),
		SCREENSHOT_TEXT("export.screenshot.text"),
		STARTUP_3DCAMERA("startup.camera3d");

        String key;
        private BooleanKey(String _key)
        {
            key=_key;
        }
    }

    public enum IntKey
    {
        ADDLAYER_LAST_SOURCEID("addlayer.last.sourceid"),
		MOVIE_IMG_WIDTH("export.movie.width"),
		MOVIE_IMG_HEIGHT("export.movie.height"),
		SCREENSHOT_IMG_WIDTH("export.screenshot.width"),
		SCREENSHOT_IMG_HEIGHT("export.screenshot.height"),
		PREVIEW_TEMPORAL_SUBSAMPLE("preview.temporal.subsample"),
		PREVIEW_SPATIAL_START("preview.spatial.start"),
		JPIP_BATCH_SIZE("jpip.batchsize"), STARTUP_LICENSE_SHOWN("startup.license"),
		CACHE_SIZE("cache.size");

        String key;
        private IntKey(String _key)
        {
            key=_key;
        }
    }

    public enum StringKey
    {
    	STATE_DIRECTORY("state.directory"),
    	MOVIE_EXPORT_DIRECTORY("export.movie.directory"),
    	SCREENSHOT_EXPORT_DIRECTORY("export.screenshot.directory"),
    	MOVIE_DOWNLOAD_PATH("download.movie.directory"),
    	MOVIE_OPEN_PATH("open.movie.directory"),
    	METADATA_EXPORT_DIRECTORY("export.metadata.directory"),
    	UUID("uuid"),
        DATE_FORMAT("date.format"),
        TOOLBAR_DISPLAY("display.toolbar");

        String key;
        private StringKey(String _key)
        {
            key=_key;
        }
    }

    public static int getInt(IntKey _key)
    {
    	final String v=getString(_key.key);
    	try
    	{
    		return Integer.parseInt(v);
    	}
    	catch(NumberFormatException _nfe)
    	{
    		Telemetry.trackException(new NumberFormatException("Settings: Cannot parse \""+v+"\" for key \""+_key+"\"."));
    		return 0;
    	}
    }
    
	public static void setString(StringKey _key, String _val)
	{
		setString(_key.key,_val);
	}
	
	private static void setString(String _key, String _val)
    {
        if (_val.equals(getString(_key)))
            return;
        
        PREF_NODE.put(_key,_val);
        delayedFlush();
    }
	
	public static void syncFlush()
	{
        try
        {
            PREF_NODE.flush();
        }
        catch (Exception ex)
        {
        	Telemetry.trackException(ex);
        }
	}
	
	private static void delayedFlush()
	{
        synchronized(syncObj)
        {
            if(saveThread!=null)
                saveThread.interrupt();
            else
            {
                saveThread=new Thread(() ->
                    {
                        for(;;)
                            try
                            {
                                Thread.sleep(1000);
                                break;
                            }
                            catch(InterruptedException _ie)
                            {
                            }
                        
                        synchronized(syncObj)
                        {
                            saveThread=null;
                        }
                        
                        try
                        {
                            PREF_NODE.flush();
                        }
                        catch (Exception ex)
                        {
                        	Telemetry.trackException(ex);
                        }
                    });
                saveThread.start();
            }
        }
	}
    
    //used to coordinate delayed flushing
    private final static Object syncObj=new Object();
    private static @Nullable Thread saveThread;
    
    private static @Nullable String getString(@Nonnull String _key)
    {
        return PREF_NODE.get(_key,DEFAULT_PROPERTIES.getProperty(_key));
    }
    
    private static @Nullable String getDefaultString(@Nonnull String _key)
    {
        return DEFAULT_PROPERTIES.getProperty(_key);
    }
    
    public static @Nullable String getString(StringKey _key)
    {
        return getString(_key.key);
    }
    
    public static @Nullable String getDefaultString(StringKey _key)
    {
        return getDefaultString(_key.key);
    }
}
