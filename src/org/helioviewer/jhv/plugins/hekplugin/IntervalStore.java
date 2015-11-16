package org.helioviewer.jhv.plugins.hekplugin;

import java.util.*;
import java.util.Map.Entry;

/**
 * Relationship: Interval <--> Items during that Interval
 * 
 */
public class IntervalStore<TimeFormat extends Comparable<TimeFormat>, ItemFormat extends IntervalComparison<TimeFormat>>
{
	/**
	 * The interval represents the Interval which returned the data
	 */
	private HashMap<Interval<TimeFormat>, IntervalContainer<TimeFormat, ItemFormat>> data = new HashMap<>();

	public IntervalStore()
	{
	}

	public IntervalStore(Interval<TimeFormat> interval)
	{
		data.put(interval, new IntervalContainer<TimeFormat, ItemFormat>());
	}

	/**
	 * Add/Merge the set of intervals given
	 * 
	 * @param data
	 *            - intervals to be added/merged
	 */
	public void add(HashMap<Interval<TimeFormat>, IntervalContainer<TimeFormat, ItemFormat>> data)
	{
		for (Entry<Interval<TimeFormat>, IntervalContainer<TimeFormat, ItemFormat>> iter : data.entrySet())
			add(iter.getKey(), iter.getValue());
	}

	/**
	 * Add/Merge the interval given
	 * 
	 * @param newItems
	 *            - intervals to be added/merged
	 */
	public boolean add(Interval<TimeFormat> newInterval, IntervalContainer<TimeFormat, ItemFormat> newIntervalContainer)
	{
		boolean merged = false;
		int newItems = newIntervalContainer.getItems().size();

		List<Interval<TimeFormat>> overlappingIntervals = this.getOverlappingIntervals(newInterval);

		// Melt new interval with all existing ones
		for (Interval<TimeFormat> overlappingInterval : overlappingIntervals)
		{
			newInterval = newInterval.expand(overlappingInterval);
			merged = true;

			// move items from old interval to the expanded one
			IntervalContainer<TimeFormat, ItemFormat> toAdd = this.data.get(overlappingInterval);
			newIntervalContainer.downloadableEvents += toAdd.getDownloadableEvents();

			for (ItemFormat item : toAdd.getItems())
			{
				if (!newIntervalContainer.getItems().contains(item))
				{
					newIntervalContainer.getItems().add(item);
				}
				else
				{
					// already contains
				}
			}

			// move over downloadable events

			this.data.remove(overlappingInterval);
		}

		// Log.info("Added "+ newItems + " new Items to container with " +
		// newIntervalContainer.downloadableEvents);
		newIntervalContainer.downloadableEvents -= newItems;

		// if we have no more downloadable events, the interval is not partial
		// anymore
		if (newItems > 0 && newIntervalContainer.downloadableEvents == 0)
			newIntervalContainer.setPartial(false);

		// Log.info("New Result of downloadable Paths is " +
		// newIntervalContainer.downloadableEvents);

		// and finally store the new interval
		data.put(newInterval, newIntervalContainer);

		// loop to make sure that all events are registered to all possible
		// buckets

		for (Interval<TimeFormat> curInterval : this.data.keySet())
		{

			IntervalContainer<TimeFormat, ItemFormat> curContainer = this.data.get(curInterval);

			// this is currently not the interval we just created
			if (!curInterval.equals(newInterval))
				// loop over all items
				for (ItemFormat curItem : curContainer.getItems())
					// if it overlaps the newInterval, add the item there, too
					if (curItem.overlaps(newInterval) && !newIntervalContainer.getItems().contains(curItem))
						newIntervalContainer.getItems().add(curItem);
		}

		return merged;

	}

	/**
	 * Check whether any of the sets intervals overlaps the given interval
	 * 
	 * @param interval
	 * @return list of overlapping intervals
	 */
	public List<Interval<TimeFormat>> getOverlappingIntervals(Interval<TimeFormat> interval)
	{
		ArrayList<Interval<TimeFormat>> result = new ArrayList<>();

		for (Interval<TimeFormat> key : data.keySet())
			if (key.overlaps(interval) || key.equals(interval))
				result.add(key);

		return result;
	}

	/**
	 * Check whether any of the sets intervals covers the given interval
	 * 
	 * @param interval
	 * @return list of overlapping intervals
	 */
	public List<Interval<TimeFormat>> getCoveringIntervals(Interval<TimeFormat> interval)
	{
		ArrayList<Interval<TimeFormat>> result = new ArrayList<>();

		for (Interval<TimeFormat> key : this.data.keySet())
			if (key.containsInclusive(interval) || key.equals(interval))
				result.add(key);
		
		return result;
	}

	/**
	 * Return the set of intervals which is to be requested in order to have the
	 * given interval cached, too
	 * 
	 * @param interval
	 * @return intervals needed
	 */
	public List<Interval<TimeFormat>> needed(Interval<TimeFormat> interval)
	{
		// linked list needed
		List<Interval<TimeFormat>> result = new ArrayList<>();

		result.add(interval);

		// loop over the intervals in this cache
		for (Interval<TimeFormat> curStoreInterval : this.data.keySet())
		{
			IntervalContainer<TimeFormat, ItemFormat> curStoreContainer = this.data.get(curStoreInterval);

			// partially downloaded containers do not count
			if (curStoreContainer.isPartial())
				continue;

			// loop over the intervals in the current result set

			// T O D O ? Iterator<SimpleInterval<TimeFormat>> checkIter =
			// checkInter.iterator();

			// iterate over the list -
			ListIterator<Interval<TimeFormat>> resultIntervalsIterator = result.listIterator();

			while (resultIntervalsIterator.hasNext())
			{
				Interval<TimeFormat> curResultInterval = resultIntervalsIterator.next();

				// replace currently stored (overlapping) result interval by an
				// excluded one
				// we only remove those intervals that have already been loaded
				if (curStoreInterval.overlaps(curResultInterval) || curStoreInterval.equals(curResultInterval))
				{
					resultIntervalsIterator.remove(); // result.remove(checkInterval);
					List<Interval<TimeFormat>> toAdd = curResultInterval.exclude(curStoreInterval);
					for (Interval<TimeFormat> add : toAdd)
						resultIntervalsIterator.add(add);
				}

			}

		}

		return result;
	}

	/**
	 * Return the set of intervals which is to be requested in order to have the
	 * given intervals(!) cached, too
	 * 
	 * The Items of the given List need to be pairwise non overlapping
	 * 
	 * @param requestIntervals
	 * @return intervals needed
	 */
	public List<Interval<TimeFormat>> needed(List<Interval<TimeFormat>> requestIntervals)
	{
		List<Interval<TimeFormat>> result = new ArrayList<>();
		for (Interval<TimeFormat> requestInterval : requestIntervals)
		{
			List<Interval<TimeFormat>> neededIntervals = this.needed(requestInterval);
			result.addAll(neededIntervals);
		}
		return result;
	}

	/**
	 * Return a String representation, e.g. "[[A,B),[C,D)]"
	 */
	public String toString()
	{
		StringBuilder result = new StringBuilder("IntervalStore [ ");
		boolean added = false;

		// loop over the intervals in this cache
		for (Interval<TimeFormat> curInterval : this.data.keySet())
		{
			added = true;
			result.append(curInterval.toString());
			result.append(" : ");
			result.append((this.getItem(curInterval)));
			result.append(", ");
		}

		if (added)
			result.delete(result.length() - 1, result.length());

		result.append("]");

		return result.toString();
	}

	/**
	 * Check whether this set of intervals is equal to the given set of
	 * intervals
	 * 
	 * @param other
	 * @return true if both sets of intervals are equal
	 */
	public boolean equals(Object o)
	{
		if (o == null)
			return false;

		if (!(o instanceof IntervalStore<?, ?>))
			return false;

		@SuppressWarnings("unchecked")
		IntervalStore<TimeFormat, ItemFormat> other = (IntervalStore<TimeFormat, ItemFormat>) o;

		if (data.size() != other.data.size())
			return false;

		for (Interval<TimeFormat> key : data.keySet())
			if (!data.get(key).equals(other.data.get(key)))
				return false;
		
		return true;
	}

	/*
	 * // REMOVE ALL DEGENERATED BUCKETS public void cleanup() {
	 * Iterator<IntervalBucket<TimeFormat,ItemFormat>> iter =
	 * intervals.iterator(); while (iter.hasNext()) { IntervalBucket<TimeFormat,
	 * ItemFormat> i = iter.next(); if (i.getDuration().degenerated())
	 * iter.remove(); } }
	 */

	public boolean isEmpty()
	{
		return this.data.isEmpty();
	}

	/**
	 * interval addresses the requested interval, not the length of the event
	 */
	public void addEvent(Interval<TimeFormat> interval, ItemFormat newEvent)
	{
		// System.out.println("Adding " + newEvent + " to " + interval);
		if (!this.data.containsKey(interval))
		{
			this.data.put(interval, new IntervalContainer<TimeFormat, ItemFormat>());
		}

		IntervalContainer<TimeFormat, ItemFormat> curContainer = this.data.get(interval);

		// do not add the event if it is already in there
		if (newEvent != null && !curContainer.getItems().contains(newEvent))
		{
			curContainer.getItems().add(newEvent);
		}

	}

	/**
	 * Checks whether the given interval has already been requested and stored
	 * 
	 * @param request
	 *            - EXACT interval to be asked for
	 * @return true if the given interval has already been requested and stored
	 */
	public boolean contains(Interval<TimeFormat> request)
	{
		return this.data.containsKey(request);
	}

	/**
	 * Check whether the given item has been stored in the given request
	 * interval
	 * 
	 * @param request
	 *            - EXACT interval in which the event might have been stored
	 * @param hek
	 *            - event to be asked for
	 * @return true if the given item has been stored in the given request
	 *         interval
	 */
	public boolean contains(Interval<TimeFormat> request, ItemFormat hek)
	{
		if (!data.containsKey(request))
			return false;
		return data.get(request).getItems().contains(hek);
	}

	/**
	 * Check whether the given event is registered somewhere in this store
	 * 
	 * @param item
	 *            - item to be checked
	 * @return true if the given event is registered somewhere in this store
	 */
	public boolean contains(ItemFormat item)
	{
		return findItem(item).size() > 0;
	}

	/**
	 * Return all intervals in which the given event is registered
	 * 
	 * @param item
	 *            - event to be asked for
	 * @return - list of intervals
	 */
	public List<Interval<TimeFormat>> findItem(ItemFormat item)
	{
		List<Interval<TimeFormat>> result = new ArrayList<>();

		for (Interval<TimeFormat> interval : this.data.keySet())
		{
			if (this.data.get(interval).getItems().contains(item))
			{
				result.add(interval);
			}

		}

		return result;

	}

	/**
	 * Return the IntervalContainer for the given Interval - no overlap checks
	 * are performed
	 * 
	 * @param interval
	 * @return
	 */
	public IntervalContainer<TimeFormat, ItemFormat> getItem(Interval<TimeFormat> interval)
	{
		return this.data.get(interval);
	}

	public Set<Interval<TimeFormat>> getKeys()
	{
		return this.data.keySet();
	}

	/**
	 * This IntervalCache has a couple of Request Buckets!
	 */
	public List<Interval<TimeFormat>> getIntervals()
	{
		List<Interval<TimeFormat>> result = new ArrayList<>();
		for (Interval<TimeFormat> timeFormatInterval : this.data.keySet())
		{
			result.add(timeFormatInterval);
		}
		return result;
	}

	public void removeInterval(Interval<TimeFormat> interval)
	{
		this.data.remove(interval);
	}

}
