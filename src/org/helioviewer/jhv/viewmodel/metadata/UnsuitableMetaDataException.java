package org.helioviewer.jhv.viewmodel.metadata;

class UnsuitableMetaDataException extends RuntimeException
{
	private static final long serialVersionUID=1489762423742402867L;
	public UnsuitableMetaDataException() { super(); }
	public UnsuitableMetaDataException(String message) { super(message); }
	public UnsuitableMetaDataException(String message, Throwable cause) { super(message, cause); }
	public UnsuitableMetaDataException(Throwable cause) { super(cause); }
}
