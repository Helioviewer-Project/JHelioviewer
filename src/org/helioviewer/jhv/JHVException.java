package org.helioviewer.jhv;

public class JHVException {
	public static class TextureException extends Exception{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1525619639389031761L;
		public TextureException() {super();}
		public TextureException(String message) {super(message);}
		public TextureException(String message, Throwable cause) {super(message, cause);}
		public TextureException(Throwable cause) {super(cause);}		
	}
	
	public static class MetaDataException extends Exception{
		private static final long serialVersionUID=1489762423742402867L;
		public MetaDataException() { super(); }
		public MetaDataException(String message) { super(message); }
		public MetaDataException(String message, Throwable cause) { super(message, cause); }
		public MetaDataException(Throwable cause) { super(cause); }
	}
	
}
