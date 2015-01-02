package org.helioviewer.gl3d.plugin.pfss.data.caching;

import org.helioviewer.gl3d.plugin.pfss.data.FileDescriptor;

/**
 * Chacheable interface. The implementers of this interface can be cached.
 * @author Jonas Schwammberger
 */
public interface Cacheable {
	public FileDescriptor getDescriptor();
}
