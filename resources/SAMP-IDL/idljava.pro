; how to set up IDL-Java Bridge (https://www.harrisgeospatial.com/docs/initializingtheidl-javabridge.html):
; 
; 1. Download JSAMP from http://www.star.bristol.ac.uk/~mbt/jsamp (or Github: https://github.com/mbtaylor/jsamp)
; 2. Go to <IDL_DEFAULT>/resource/bridges/import/java
; 3. Open your config file (.idljavabrc | idljavabrc | idljavabrc.32), and
;    Overwrite the example .jar in the JVM Classpath with the JSAMP .jar
; 4. (re) start IDL
;
; alternatively you can use SETENV to set CLASSPATH to your JSAMP .jar BEFORE creating any java object.
;
; Attention: make sure your IDL uses JRE1.8 or higher, or assemble the used JAR file with the JDK your IDL is running!
; Change your configuration file (see above) accordingly!
; see also: https://www.harrisgeospatial.com/Support/Self-Help-Tools/Help-Articles/Help-Articles-Detail/ArtMID/10220/ArticleID/16290/Changing-the-Java-version-used-with-the-IDL-8-Workbench

; IMPORTANT: this needs to be executed before the IDL-JAVA Bridge is loaded, hence before any java class is invoked!
PRO initialize_IDL_Java_Bridge
  path_to_jsamp = 'C:\Projects\JHV\JHelioviewer\resources\SAMP-IDL\jsamp-1.3.5_signed.jar
  path_to_jsamp = path_to_jsamp + ';C:\Projects\JHV\JHelioviewer\resources\SAMP-IDL\IDL_JSAMP_Bridge.jar'
  setenv, 'CLASSPATH=' + getenv('CLASSPATH') + path_to_jsamp
END

FUNCTION idljava
  initialize_IDL_JAVA_Bridge

  jDCP = OBJ_NEW('IDLjavaObject$Static$DEFAULTCLIENTPROFILE', 'org.astrogrid.samp.client.DefaultClientProfile')
  jSampHub = OBJ_NEW('IDLjavaObject$JAVA_OASC_HUBCONNECTOR', 'org.astrogrid.samp.client.HubConnector', jDCP->getProfile())
  OBJ_DESTROY, jDCP
  
  jSampHub->setActive, 1 ; connect to hub
  
  jMsgHandler = OBJ_NEW('IDLjavaObject$MessageHandler', 'ch.fhnw.jsamp.IDL_MessageHandler', ["jhv.vso.load"])
  jSampHub->addMessageHandler, jMsgHandler
  jSampHub->declareSubscriptions, jSampHub->computeSubscriptions()
  
  return, Hash('hub', jSampHub, 'msgHandler', jMsgHandler)
END

PRO idljava_end, jSampHub, jMsgHandler
  jSampHub->setActive, 0 ; disconnect from hub
  OBJ_DESTROY, jSampHub, jMsgHandler
END
