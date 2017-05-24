package org.helioviewer.gl3d.plugin.pfss.data.caching;

import org.helioviewer.gl3d.plugin.pfss.data.FileDescriptor;

/**
 * Interface for Caching implementation.
 * 
 * @author Jonas Schwammberger
 */
public interface Cacheable {
	public FileDescriptor getDescriptor();
}
