package org.helioviewer.jhv;

public class JHVException {

	public static class LayerException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = -8857948490108848519L;
		public LayerException() {super();}
		public LayerException(String message) {super(message);}
		public LayerException(String message, Throwable cause) {super(message, cause);}
		public LayerException(Throwable cause) {super(cause);}
	}
	
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
	
	public static class CacheException extends Exception{
		/**
		 * 
		 */
		private static final long serialVersionUID = -8892218353477470218L;
		public CacheException() {super();}
		public CacheException(String message) {super(message);}
		public CacheException(String message, Throwable cause) {super(message, cause);}
		public CacheException(Throwable cause) {super(cause);}		
	}

	public static class LocalFileException extends Exception{
		/**
		 * 
		 */
		private static final long serialVersionUID = 5583436004421680892L;
		public LocalFileException() { super(); }
		public LocalFileException(String message) { super(message); }
		public LocalFileException(String message, Throwable cause) { super(message, cause); }
		public LocalFileException(Throwable cause) { super(cause); }
	}

	public static class MetaDataException extends Exception{
		private static final long serialVersionUID=1489762423742402867L;
		public MetaDataException() { super(); }
		public MetaDataException(String message) { super(message); }
		public MetaDataException(String message, Throwable cause) { super(message, cause); }
		public MetaDataException(Throwable cause) { super(cause); }
	}
	
	public static class TimeLineException extends Exception{
		/**
		 * 
		 */
		private static final long serialVersionUID = -2821652073166401086L;
		public TimeLineException() { super(); }
		public TimeLineException(String message) { super(message); }
		public TimeLineException(String message, Throwable cause) { super(message, cause); }
		public TimeLineException(Throwable cause) { super(cause); }
	}

}
