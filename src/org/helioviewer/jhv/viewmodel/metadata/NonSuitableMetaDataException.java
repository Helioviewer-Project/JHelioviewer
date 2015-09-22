package org.helioviewer.jhv.viewmodel.metadata;

class NonSuitableMetaDataException extends RuntimeException{
  private static final long serialVersionUID=1489762423742402867L;
  public NonSuitableMetaDataException() { super(); }
	  public NonSuitableMetaDataException(String message) { super(message); }
	  public NonSuitableMetaDataException(String message, Throwable cause) { super(message, cause); }
	  public NonSuitableMetaDataException(Throwable cause) { super(cause); }
	}
