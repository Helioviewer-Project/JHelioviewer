package org.helioviewer.jhv.viewmodel.view.jp2view.newjpx;

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
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPSocket;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_KduException;

public class JPXLayer {
	private JPIPSocket socket;
	private JP2Image jp2Image;

	public enum LAYER_QUALITY{
		NEXT_POSIBILE, MAX;
	}
	
	public JPXLayer(JP2Image jp2Image) {
		this.jp2Image = jp2Image;
	}
	
	public void openSocket(){
		try {
            socket = new JPIPSocket();
            socket.connect(jp2Image.getURI());
            
            /*
            if (!parentImageRef.isMultiFrame()) {
                if(Thread.currentThread().isInterrupted())
                    return;
                
                KakaduUtils.updateServerCacheModel(socket, cacheRef, true);
            }
            */

        } catch (IOException e) {
            e.printStackTrace();
            
            try {
                socket.close();
            } catch (IOException ioe) {
                System.err.println(">> J2KReader.run() > Error closing socket.");
                ioe.printStackTrace();
            }
            
            if(Thread.currentThread().isInterrupted())
                return;

            if(Thread.currentThread().isInterrupted())
                return;

        }

	}
	
	public static void main(String[] args) {
		loadLibraries();
		URI uri;
		try {
			uri = new URI("jpip://helioviewer.org:8090/AIA/2015/02/23/171/2015_02_23__15_42_59_34__SDO_AIA_AIA_171.jp2");
			URI downloadURI = new URI("http://helioviewer.org/api/index.php?action=getJP2Image&observatory=SDO&instrument=AIA&detector=AIA&measurement=171&date=2015-02-23T16:07:26Z&json=true");
			

	        JP2Image jp2Image = new JP2Image(uri, downloadURI);

		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JHV_KduException e) {
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
						//loadJNILibary(tmpLibDir, directory, "msvcr100.dll");
						loadJNILibary(tmpLibDir, directory, "kdu_v63R.dll");
						loadJNILibary(tmpLibDir, directory, "kdu_a63R.dll");
						loadJNILibary(tmpLibDir, directory, "kdu_jni.dll");
						loadExecuteLibary(tmpLibDir, directory,
								"cgc-windows-x86-64.exe", "cgc");
					} else if (arch.indexOf("86") != -1) {
						directory += "32/";
						//loadJNILibary(tmpLibDir, directory, "msvcr100.dll");
						loadJNILibary(tmpLibDir, directory, "kdu_v63R.dll");
						loadJNILibary(tmpLibDir, directory, "kdu_a63R.dll");
						loadJNILibary(tmpLibDir, directory, "kdu_jni.dll");
						loadExecuteLibary(tmpLibDir, directory,
								"cgc-windows-x86-32.exe", "cgc");
					} else {
						System.err.println(">> Platform > Could not determine platform. OS: "
                        + os + " - arch: " + arch);
					}

				} else if (os.indexOf("linux") != -1) {
					directory += "linux/";
					if (arch.indexOf("64") != -1) {
						directory += "64/";
						loadJNILibary(tmpLibDir, directory,
								"libkdu_jni-linux-x86-64-glibc-2-7.so");
						loadExecuteLibary(tmpLibDir, directory,
								"cgc-linux-x86-64", "cgc");
					} else if (arch.indexOf("86") != -1) {
						directory += "32/";
						loadJNILibary(tmpLibDir, directory,
								"libkdu_jni-linux-x86-32-glibc-2-7.so");
						loadExecuteLibary(tmpLibDir, directory,
								"cgc-linux-x86-32", "cgc");
					} else {
						System.err.println(">> Platform > Could not determine platform. OS: "
                        + os + " - arch: " + arch);
					}
				} else if (os.indexOf("mac os x") != -1) {
					directory += "mac/";
					loadJNILibary(tmpLibDir, directory,
							"libkdu_jni-mac-x86-64.jnilib");
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
			String name) {
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

			System.load(tmp.getAbsolutePath());
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
