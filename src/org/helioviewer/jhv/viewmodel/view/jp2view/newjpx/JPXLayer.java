package org.helioviewer.jhv.viewmodel.view.jp2view.newjpx;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.helioviewer.jhv.JHelioviewer;
import org.helioviewer.jhv.base.FileUtils;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2Image;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet.ResolutionLevel;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.SubImage;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.http.HTTPRequest.Method;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPQuery;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPRequest;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPSocket;

public class JPXLayer {
	private JPIPSocket socket;
	private JP2Image jp2Image;

	public enum LAYER_QUALITY{
		NEXT_POSIBILE, MAX;
	}
	
	public JPXLayer(JP2Image jp2Image) {
		this.jp2Image = jp2Image;
	}
	
	
	public static void main(String[] args) {
	    
	    System.loadLibrary("msvcr120");
        System.loadLibrary("kdu_v75R");
	    System.loadLibrary("kdu_a75R");
	    System.loadLibrary("kdu_jni");
	    
	    //System.load("resources/libs/windows/64/kdu_jni.dll");
	    
		loadLibraries();

		/*String os = System.getProperty("os.name");
		String arch = System.getProperty("os.arch");
		Path tmpLibDir = null;
		
		try {
			tmpLibDir = Files.createTempDirectory("jhv-libs");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		tmpLibDir.toFile().deleteOnExit();
		String directory = "/libs/mac/";
		System.out.println(tmpLibDir);
		String property = System.getProperty("java.library.path");
		System.out.println(property);
				
		//System.load("/Users/binchu/Documents/FHNW/JHelioviewer/libkdu_jni.jnilib");
		File f = new File("/var/folders/+n/+nfb8NHsHiSpEh6AHMCyvE+++TI/-Tmp-/libjogl.jnilib");
		System.out.println(f.toString());

		System.loadLibrary("kdu_jni");
		//System.load("/Users/binchu/Documents/FHNW/JHelioviewer/libkdu_jni.jnilib");
		//loadLibraries();*/

		URI uri;
		try {
			uri = new URI("jpip://helioviewer.org:8090/AIA/2015/02/23/171/2015_02_23__15_42_59_34__SDO_AIA_AIA_171.jp2");
			URI downloadURI = new URI("http://helioviewer.org/api/index.php?action=getJP2Image&observatory=SDO&instrument=AIA&detector=AIA&measurement=171&date=2015-02-23T16:07:26Z&json=true");
			
			NewReader newReader = new NewReader(uri);
			
			//Subi
			SubImage _roi = new SubImage(0, 0, 4096, 4096);
			ResolutionSet resolutionSet = new ResolutionSet(8);
			resolutionSet.addResolutionLevel(7, new Rectangle(32, 32));
			resolutionSet.addResolutionLevel(6, new Rectangle(64, 64));
			resolutionSet.addResolutionLevel(5, new Rectangle(128, 128));
			resolutionSet.addResolutionLevel(4, new Rectangle(256, 256));
			resolutionSet.addResolutionLevel(3, new Rectangle(512, 512));
			resolutionSet.addResolutionLevel(2, new Rectangle(1024, 1024));
			resolutionSet.addResolutionLevel(1, new Rectangle(2048, 2048));
			resolutionSet.addResolutionLevel(0, new Rectangle(4096, 4096));
			ResolutionLevel _resolution = resolutionSet.getResolutionLevel(0);
			System.out.println("res : " + _resolution);
			JPIPQuery query = newReader.createQuery(new JP2ImageParameter(_roi, _resolution, 0, 0), 0, 0);
			System.out.println("query : " + query);
			JPIPRequest request = new JPIPRequest(Method.GET, query);
			newReader.addRequest(request);
			/*
	        JP2Image jp2Image = new JP2Image(uri, downloadURI);
			NewReader newReader = new NewReader(jp2Image);
			newReader.openSocket();
			*/
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void loadLibraries() {

		String os = System.getProperty("os.name");
		String arch = System.getProperty("os.arch");
		Path tmpLibDir;
		try {
			tmpLibDir = Files.createTempDirectory("jhv-libs");
			tmpLibDir.toFile().deleteOnExit();

			String directory = "/libs/";
			if (os != null && arch != null) {
				os = os.toLowerCase();
				arch = arch.toLowerCase();
				if (os.indexOf("windows") != -1) {
					directory += "windows/";
					if (arch.indexOf("64") != -1) {
						directory += "64/";
						loadJNILibary(tmpLibDir, directory, "msvcr120.dll", false);
						loadJNILibary(tmpLibDir, directory, "kdu_v75R.dll", false);
						loadJNILibary(tmpLibDir, directory, "kdu_a75R.dll", false);
						loadJNILibary(tmpLibDir, directory, "kdu_jni.dll", true);
						loadExecuteLibary(tmpLibDir, directory,
								"cgc-windows-x86-64.exe", "cgc");
					} else if (arch.indexOf("86") != -1) {
					    throw new RuntimeException("No x86 support");
					} else {
						System.err.println(">> Platform > Could not determine platform. OS: "
                        + os + " - arch: " + arch);
					}

				} else if (os.indexOf("linux") != -1) {
					directory += "linux/";
					if (arch.indexOf("64") != -1) {
						directory += "64/";
						loadJNILibary(tmpLibDir, directory,
								"libkdu_jni-linux-x86-64-glibc-2-7.so", true);
						loadExecuteLibary(tmpLibDir, directory,
								"cgc-linux-x86-64", "cgc");
					} else if (arch.indexOf("86") != -1) {
						directory += "32/";
						loadJNILibary(tmpLibDir, directory,
								"libkdu_jni-linux-x86-32-glibc-2-7.so", true);
						loadExecuteLibary(tmpLibDir, directory,
								"cgc-linux-x86-32", "cgc");
					} else {
						System.err.println(">> Platform > Could not determine platform. OS: "
                        + os + " - arch: " + arch);
					}
				} else if (os.indexOf("mac os x") != -1) {
					directory += "mac/";
					loadJNILibary(tmpLibDir, directory, "libkdu_v75R.dylib", false);
					loadJNILibary(tmpLibDir, directory, "libkdu_a75R.dylib", false);
					loadJNILibary(tmpLibDir, directory,
							"libkdu_jni.jnilib", true);
					loadExecuteLibary(tmpLibDir, directory, "cgc-mac", "cgc");
				} else {
					System.err.println(">> Platform > Could not determine platform. OS: "
                    + os + " - arch: " + arch);
				}
			} else {
				System.err.println(">> Platform > Could not determine platform. OS: "
                + os + " - arch: " + arch);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void loadJNILibary(Path tmpPath, String directory,
			String name, boolean load) {
		InputStream in = JHelioviewer.class.getResourceAsStream(directory
				+ name);
		byte[] buffer = new byte[1024];
		int read = -1;
		File tmp = new File(tmpPath.toFile(), name);
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(tmp);
			while ((read = in.read(buffer)) != -1) {
				fos.write(buffer, 0, read);
			}
			fos.close();
			in.close();
			if (load) System.load(tmp.getAbsolutePath());
			tmp.deleteOnExit();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void loadExecuteLibary(Path tmpPath, String directory,
			String name, String executableName) {
		InputStream in = JHelioviewer.class.getResourceAsStream(directory
				+ name);
		byte[] buffer = new byte[1024];
		int read = -1;
		File tmp = new File(tmpPath.toFile(), name);
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(tmp);
			while ((read = in.read(buffer)) != -1) {
				fos.write(buffer, 0, read);
			}
			fos.close();
			in.close();
			tmp.setExecutable(true);
			FileUtils.registerExecutable(executableName, tmp.getAbsolutePath());
			tmp.deleteOnExit();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
