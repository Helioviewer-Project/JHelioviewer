package org.helioviewer.jhv.plugins.pfssplugin.data.caching;

import org.helioviewer.jhv.plugins.pfssplugin.data.FileDescriptor;

/**
 * Interface for Caching implementation.
 */
public interface Cacheable {
	FileDescriptor getDescriptor();
}
