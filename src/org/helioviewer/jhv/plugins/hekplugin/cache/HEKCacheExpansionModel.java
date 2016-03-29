package org.helioviewer.jhv.plugins.hekplugin.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HEKCacheExpansionModel {
    HEKCache cache;
    HEKCacheModel cacheModel;
    private HashMap<HEKPath, Boolean> expansionState = new HashMap<>();

    public HEKCacheExpansionModel(HEKCache cache) {
        this.cache = cache;
        this.cacheModel = cache.getModel();
    }

    public List<HEKPath> getExpandedPaths(boolean type) {

        // td Malte Nuhn - Implement the CacheModelLock

    	List<HEKPath> result = new ArrayList<>();

        result.add(new HEKPath(this.cache));

        for (HEKPath p : this.expansionState.keySet()) {
            if (expansionState.get(p) == type) {
                result.add(p);
            }
        }
        return result;
    }

    public void setExpandedState(HEKPath p, boolean b, boolean overwrite) {

        // td Malte Nuhn - Implement the CacheModelLock
        if (!expansionState.containsKey(p) || overwrite) {
            expansionState.put(p, b);
        }

    }

    public boolean getExpansionState(HEKPath p) {

        // td Malte Nuhn - Implement the CacheModelLock

        if (!expansionState.containsKey(p)) {
            return false;
        } else {
            return expansionState.get(p);
        }
    }

    /**
     * Expands the given number of sublevel starting from the given node
     * 
     * @param node
     * @param level
     */
    private void expandToLevel(HEKPath node, int level, boolean overwrite, boolean collapsebelow) {
        if (level < 0)
            return;
        List<HEKPath> children = cacheModel.getChildren(node, true);
        for (HEKPath child : children) {
            boolean curExpanded = level > 0; // expand if level > 0, else
                                             // collapse
            boolean curOverwrite = overwrite || !curExpanded; // FORCE collapse!
            setExpandedState(child, curExpanded, curOverwrite);
            // System.out.println("Setting " + child + " to " + curExpanded);

            expandToLevel(child, level - 1, overwrite, collapsebelow);
        }
    }

    /**
     * Expand the tree in the first levels
     * 
     * @param level
     *            - number of levels to expand
     */
    public void expandToLevel(int level, boolean overwrite, boolean collapsebelow) {
        expandToLevel(cacheModel.getRoot(), level, overwrite, collapsebelow);
    }
}
