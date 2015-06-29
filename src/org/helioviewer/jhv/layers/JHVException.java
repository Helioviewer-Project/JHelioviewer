package org.helioviewer.jhv.layers;

public class JHVException {

	public static class LayerException extends Exception {
		public LayerException() {super();}
		public LayerException(String message) {super(message);}
		public LayerException(String message, Throwable cause) {super(message, cause);}
		public LayerException(Throwable cause) {super(cause);}
	}
	
	public static class TextureException extends Exception{
		public TextureException() {super();}
		public TextureException(String message) {super(message);}
		public TextureException(String message, Throwable cause) {super(message, cause);}
		public TextureException(Throwable cause) {super(cause);}		
	}
}
