package org.helioviewer.jhv.base;

import java.io.InputStream;
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
        if(PREF_NODE.get("UUID",null)==null)
        {
            PREF_NODE.put("UUID",UUID.randomUUID().toString());
            try
            {
                PREF_NODE.flush();
            }
            catch(BackingStoreException e)
            {
            	e.printStackTrace();
            }
        }
    }
    

    private Settings()
    {
    }

    public static void load()
    {
        DEFAULT_PROPERTIES.clear();
        try (InputStream defaultPropStream = Settings.class.getResourceAsStream("/settings/defaults.properties"))
        {
            DEFAULT_PROPERTIES.load(defaultPropStream);
            DEFAULT_PROPERTIES.setProperty("default.local.path",Directories.REMOTEFILES.getPath());
            
            defaultPropStream.close();
            System.out.println(">> Settings.load() > Load default system settings: " + DEFAULT_PROPERTIES.toString());
        }
        catch (Exception ex)
        {
            System.err.println(">> Settings.load(boolean) > Could not load settings");
            Telemetry.trackException(ex);
        }
    }

    public static void setBoolean(String _key, boolean _val)
    {
    	setString(_key,_val?"1":"0");
    }
    
    public static boolean getBoolean(String _key)
    {
    	return "1".equals(getString(_key));
    }
    
	public static void setString(String _key, String _val)
    {
        if (_val.equals(getString(_key)))
            return;
        
        PREF_NODE.put(_key,_val);
        
        synchronized(syncObj)
        {
            if(saveThread!=null)
                saveThread.interrupt();
            else
            {
                saveThread=new Thread(new Runnable()
                {
                    @Override
                    public void run()
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
                    }
                });
                saveThread.start();
            }
        }
    }
    
    //used to coordinate delayed flushing
    private final static Object syncObj=new Object();
    private static @Nullable Thread saveThread;
    
    public static @Nullable String getString(@Nonnull String _key)
    {
        return PREF_NODE.get(_key,DEFAULT_PROPERTIES.getProperty(_key));
    }
}
