package org.helioviewer.jhv.base;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MultiClickAdapter extends MouseAdapter implements ActionListener
{
    @Nullable MouseEvent lastEvent;
    Timer timer;

    public MultiClickAdapter(int delay)
    {
        timer = new Timer( delay, this);
    }

    public void mouseClicked (@Nullable MouseEvent e)
    {
		if(e==null)
			return;
		
        if (e.getClickCount() > 2)
        	return;

        lastEvent = e;

        if (timer.isRunning())
        {
            timer.stop();
            doubleClick( lastEvent );
        }
        else
            timer.restart();
    }

    public void actionPerformed(@Nullable ActionEvent e)
    {
        timer.stop();
        singleClick( lastEvent );
    }

    public void singleClick(@Nullable MouseEvent e) {}
    public void doubleClick(@Nullable MouseEvent e) {}
}