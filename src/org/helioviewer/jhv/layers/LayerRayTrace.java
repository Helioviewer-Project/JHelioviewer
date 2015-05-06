package org.helioviewer.jhv.layers;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.opengl.raytrace.RayTrace;
import org.helioviewer.jhv.viewmodel.view.opengl.CompenentView;

public class LayerRayTrace{
	private RayTrace rayTrace;
	
	private Rectangle rectangle = null;
	
	private final int MAX_X_POINTS = 11;
	private final int MAX_Y_POINTS = 11;
	
	private JFrame frame = new JFrame();
	private JPanel contentPanel = new JPanel();
	private LayerInterface layer;
	
	public LayerRayTrace(LayerInterface layer) {
		this.layer = layer;
		rayTrace = new RayTrace();
		contentPanel.setBackground(Color.BLACK);
		frame.setContentPane(contentPanel);
		frame.setBounds(50, 50, 640, 480);
	}
	
	public ImageRegion getCurrentRegion(CompenentView compenentView){
		contentPanel.removeAll();
		contentPanel.setLayout(null);
		double partOfWidth = GuiState3DWCS.mainComponentView.getComponent().getWidth() / (double)(MAX_X_POINTS-1);
		double partOfHeight = GuiState3DWCS.mainComponentView.getComponent().getHeight() / (double)(MAX_Y_POINTS-1);
		
		double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
		
		for (int i = 0; i < MAX_X_POINTS; i++){
			for (int j = 0; j < MAX_Y_POINTS; j++){
				Vector2d imagePoint = rayTrace.castTexturepos((int)(i * partOfWidth), (int)(j * partOfHeight), layer.getMetaData(), compenentView);
				if (imagePoint != null){
				
				//JPanel panel = new JPanel();
				//panel.setBackground(Color.YELLOW);

				minX = Math.min(minX, imagePoint.x);
				maxX = Math.max(maxX, imagePoint.x);
				minY = Math.min(minY, imagePoint.y);
				maxY = Math.max(maxY, imagePoint.y);
				
				//panel.setBounds((int) (imagePoint.x * contentPanel.getWidth()) - 3,(int) (imagePoint.y * contentPanel.getHeight()) - 3, 5, 5);
				//contentPanel.add(panel);
				}
			}
		}
		
		Rectangle2D rectangle = new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);
		ImageRegion imageRegion = new ImageRegion(layer.getTime());
		imageRegion.setImageData(rectangle);
		imageRegion.calculateScaleFactor(layer, compenentView);
		return imageRegion;
		//frame.repaint();
		//frame.setVisible(true);
	}	
}