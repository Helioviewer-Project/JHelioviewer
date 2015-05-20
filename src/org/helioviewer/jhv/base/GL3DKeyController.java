package org.helioviewer.jhv.base;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This singleton receives all KeyEvents by the
 * {@link GL3DCameraMouseController}. Register a GL3DKeyListener to be informed
 * about KeyEvents.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DKeyController extends KeyAdapter {
    private static final GL3DKeyController SINGLETON = new GL3DKeyController();

    private HashMap<Integer, List<GL3DKeyListener>> listenerMap = new HashMap<Integer, List<GL3DKeyListener>>();

    private GL3DKeyController() {
    }

    public static GL3DKeyController getInstance() {
        return SINGLETON;
    }

    public void keyPressed(KeyEvent e) {
        if (listenerMap.containsKey(e.getKeyCode())) {
            List<GL3DKeyListener> listeners = listenerMap.get(e.getKeyCode());
            for (GL3DKeyListener l : listeners) {
                l.keyHit(e);
            }
        }
    }

    /**
     * Use the java.awt.event.KeyEvent constants as Keys
     * 
     * @param listener
     * @param key
     */
    public void addListener(GL3DKeyListener listener, Integer key) {
        if (!listenerMap.containsKey(key)) {
            listenerMap.put(key, new ArrayList<GL3DKeyListener>());
        }
        listenerMap.get(key).add(listener);
    }

    public static interface GL3DKeyListener {

        public void keyHit(KeyEvent e);
    }
}
