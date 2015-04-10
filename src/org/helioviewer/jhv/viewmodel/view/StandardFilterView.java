package org.helioviewer.jhv.viewmodel.view;

import org.helioviewer.jhv.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.jhv.viewmodel.changeevent.FilterChangedReason;
import org.helioviewer.jhv.viewmodel.changeevent.RegionChangedReason;
import org.helioviewer.jhv.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.jhv.viewmodel.changeevent.ViewChainChangedReason;
import org.helioviewer.jhv.viewmodel.filter.AbstractFilter;
import org.helioviewer.jhv.viewmodel.filter.Filter;
import org.helioviewer.jhv.viewmodel.filter.FilterListener;
import org.helioviewer.jhv.viewmodel.filter.MetaDataFilter;
import org.helioviewer.jhv.viewmodel.filter.RegionFilter;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;

/**
 * Implementation of FilterView, providing the capability to apply filters on
 * the image.
 * 
 * <p>
 * This view allows to filter the image data by using varies filters. Every time
 * the image data changes, the view calls the filter to calculate the new image
 * data. Apart from that, it feeds the filter with all other informations to do
 * its job, such as the current region, meta data or the full image.
 * 
 * <p>
 * For further information on how to use filters, see
 * {@link org.helioviewer.viewmodel.filter}
 * 
 * @author Ludwig Schmidt
 * @author Markus Langenberg
 * 
 */
public class StandardFilterView extends AbstractBasicView implements FilterView, SubimageDataView, ViewListener, FilterListener {

    protected Filter filter;

    protected ImageData filteredData;
    protected RegionView regionView;
    protected MetaDataView metaDataView;
    protected SubimageDataView subimageDataView;

    /**
     * {@inheritDoc}
     */
    public Filter getFilter() {
        return filter;
    }

    /**
     * {@inheritDoc}
     */
    public void setFilter(Filter f) {
        if (filter != null && (filter instanceof AbstractFilter)) {
            ((AbstractFilter) filter).removeFilterListener(this);
        }

        filter = f;

        if (filter != null && (filter instanceof AbstractFilter)) {
            ((AbstractFilter) filter).addFilterListener(this);
        }

        refilter();

        // join change reasons to a change event
        ChangeEvent event = new ChangeEvent();

        event.addReason(new FilterChangedReason(this, filter));
        event.addReason(new SubImageDataChangedReason(this));

        notifyViewListeners(event);
    }

    /**
     * {@inheritDoc}
     */
    public ImageData getImageData() {
        if (subimageDataView != null) {
            return subimageDataView.getImageData();
        } else
            return null;
    }

    /**
     * Prepares the actual filter process.
     * 
     * This function feeds the filter with all the additional informations it
     * needs to do its job, such as the region, meta data and the full image.
     */
    protected void refilterPrepare() {
        if (filter instanceof RegionFilter && regionView != null) {
            ((RegionFilter) filter).setRegion(regionView.getLastDecodedRegion());
        }
        if (filter instanceof MetaDataFilter && metaDataView != null) {
            ((MetaDataFilter) filter).setMetaData(metaDataView.getMetaData());
        }
    }

    /**
     * Refilters the image.
     * 
     * Calls the filter and fires a ChangeEvent afterwards.
     */
    protected void refilter() {
        if (filter != null && view != null) {
            refilterPrepare();

            if (subimageDataView != null) {
                filteredData = subimageDataView.getImageData();
            }
        } else {
            filteredData = null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * In this case, refilters the image, if there is one.
     */
    protected void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {

        updatePrecomputedViews();

        if (subimageDataView != null) {
            refilter();
            changeEvent.addReason(new SubImageDataChangedReason(this));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * In case the image data has changed, applies the filter.
     */
    public void viewChanged(View sender, ChangeEvent aEvent) {

        if (aEvent.reasonOccurred(ViewChainChangedReason.class)) {
            updatePrecomputedViews();
            refilter();
        } else if (aEvent.reasonOccurred(RegionChangedReason.class) || aEvent.reasonOccurred(SubImageDataChangedReason.class)) {
            refilter();
        }

        notifyViewListeners(aEvent);
    }

    /**
     * {@inheritDoc}
     */
    public void filterChanged(Filter f) {
        refilter();

        ChangeEvent event = new ChangeEvent();

        event.addReason(new FilterChangedReason(this, filter));
        event.addReason(new SubImageDataChangedReason(this));

        notifyViewListeners(event);
    }

    /**
     * Updates the precomputed results for different view adapters.
     * 
     * This adapters are precomputed to avoid unnecessary overhead appearing
     * when doing this every frame.
     */
    protected void updatePrecomputedViews() {
        regionView = ViewHelper.getViewAdapter(view, RegionView.class);
        metaDataView = ViewHelper.getViewAdapter(view, MetaDataView.class);
        subimageDataView = ViewHelper.getViewAdapter(view, SubimageDataView.class);
    }
}
