package org.helioviewer.jhv.base;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.annotation.Nullable;
import javax.swing.Timer;

public class MultiClickListener extends MouseAdapter implements ActionListener
{
    MouseEvent lastEvent;
    Timer timer;

    public MultiClickListener(int delay)
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

    public void singleClick(MouseEvent e) {}
    public void doubleClick(MouseEvent e) {}
}