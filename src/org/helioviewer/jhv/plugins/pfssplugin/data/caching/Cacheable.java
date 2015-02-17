package org.helioviewer.jhv.plugins.pfssplugin.data.caching;

import org.helioviewer.jhv.plugins.pfssplugin.data.FileDescriptor;

/**
 * Interface for Caching implementation.
 * 
 * @author Jonas Schwammberger
 */
public interface Cacheable {
	public FileDescriptor getDescriptor();
}
