package org.helioviewer.jhv.base.downloadmanager;

/**
 * PRIORITY are used for the Downloadmanager
 * The ordinal of the enum would be used to set the priority
 * lower ordinal int = higher priority
 * 
 * @author stefanmeier
 *
 */
public enum DownloadPriority
{
	URGENT, HIGH, MEDIUM, LOW, TIMEDEPEND;
}