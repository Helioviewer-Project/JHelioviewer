package org.helioviewer.jhv;

public class JHVException {
	public static class TextureException extends Exception{
		private static final long serialVersionUID = 1525619639389031761L;
		public TextureException() {super();}
		public TextureException(String message) {super(message);}
		public TextureException(String message, Throwable cause) {super(message, cause);}
		public TextureException(Throwable cause) {super(cause);}		
	}
}
