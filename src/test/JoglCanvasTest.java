package test;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Label;
import java.awt.Panel;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;

/**
 * @author francesco
 */
public class JoglCanvasTest extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5284179727938901723L;

	public static void main(String... args) {
		JoglCanvasTest frame = new JoglCanvasTest(
				"Splitting Swing and AWT components");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setSize(500, 400);
		frame.setVisible(true);
	}

	public JoglCanvasTest(String title) {
		super(title);
		initialize();
	}

	private void initialize() {
// add a menu to the frame
JMenuBar menu = new JMenuBar();
JMenu fileMenu = new JMenu("File");
JMenuItem fileItem1 = new JMenuItem("Item1");
JMenuItem fileItem2 = new JMenuItem("Item2");
fileMenu.add(fileItem1);
fileMenu.add(fileItem2);
menu.add(fileMenu);
this.setJMenuBar(menu);
// create red AWT Panel
Panel awtPanel = new Panel();
Label awtlabel = new Label("Questo è un java.awt.Panel");
awtPanel.add(awtlabel);
awtPanel.setBackground(Color.red.darker());
awtPanel.setMinimumSize(new Dimension(0,0));
final GLCanvas canvas1 = new GLCanvas();
canvas1.setMinimumSize(new Dimension());
final GLCanvas canvas2 = new GLCanvas();
canvas2.setMinimumSize(new Dimension());
canvas1.addGLEventListener(new GLEventListener() {
	
	@Override
	public void reshape(GLAutoDrawable d, int arg1, int arg2, int arg3,
			int arg4) {
		GL2 gl = d.getGL().getGL2();
		gl.glClearColorIi(255, 0, 0, 255);
		canvas1.repaint();
	}
	
	@Override
	public void init(GLAutoDrawable arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void dispose(GLAutoDrawable arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void display(GLAutoDrawable arg0) {
		// TODO Auto-generated method stub
		
	}
});

canvas2.addGLEventListener(new GLEventListener() {
	
	@Override
	public void reshape(GLAutoDrawable d, int arg1, int arg2, int arg3,
			int arg4) {
		GL2 gl = d.getGL().getGL2();
		gl.glClearColorIi(0, 255, 0, 255);
		canvas2.repaint();
	}
	
	@Override
	public void init(GLAutoDrawable arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void dispose(GLAutoDrawable arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void display(GLAutoDrawable arg0) {
		// TODO Auto-generated method stub
		
	}
});
// create blue Swing JPanel
JPanel swingPanel = new JPanel(new BorderLayout());
JLabel swinglabel = new JLabel("Questo è un javax.swing.JPanel");
canvas1.setMinimumSize(new Dimension(200, 200));
canvas1.setPreferredSize(new Dimension(200, 200));
swingPanel.add(canvas1, BorderLayout.NORTH);
swingPanel.setBackground(Color.blue);
swingPanel.setMinimumSize(new Dimension());
// create vertical JSplitter for red and blue panels
JSplitPane splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, swingPanel, canvas2);
splitter.setDividerLocation(0.5);
splitter.setResizeWeight(0.5);
splitter.setContinuousLayout(true);
splitter.setOneTouchExpandable(false);
this.add(splitter, BorderLayout.CENTER);
}
}