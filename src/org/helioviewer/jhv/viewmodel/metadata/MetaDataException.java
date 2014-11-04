package org.helioviewer.jhv.viewmodel.metadata;

public class MetaDataException extends RuntimeException{
  private static final long serialVersionUID=1489762423742402867L;
  public MetaDataException() { super(); }
	  public MetaDataException(String message) { super(message); }
	  public MetaDataException(String message, Throwable cause) { super(message, cause); }
	  public MetaDataException(Throwable cause) { super(cause); }
	}
