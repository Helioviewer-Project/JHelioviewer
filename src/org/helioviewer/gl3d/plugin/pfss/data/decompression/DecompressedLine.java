package org.helioviewer.gl3d.plugin.pfss.data.decompression;

import java.util.ArrayList;

import org.helioviewer.base.physics.Constants;

/**
 * Immutable Class Representing a Decompressed Fieldline
 * @author Jonas Schwammberger
 *
 */
public class DecompressedLine {
	private final ArrayList<DecompressedPoint> points;
	private final LineType type;
	
	public DecompressedLine(ArrayList<DecompressedPoint> points, LineType type) {
		super();
		this.points = points;
		this.type = type;
	}
	
	public DecompressedLine(IntermediateLineData data) {
		points = new ArrayList<>(data.size);
		for(int i = 0; i < data.size;i++) {
			points.add(new DecompressedPoint(data.channels[0][i], data.channels[1][i], data.channels[2][i]));
		}
		
		//Determine Line Type
		double mag0 = points.get(0).magnitude();
		if(mag0 < Constants.SunRadius*1.05) {
			double mag1 = points.get(data.size-1).magnitude();
			if(mag1 > Constants.SunRadius*1.05) {
				this.type = LineType.SUN_TO_OUTSIDE;
			} else {
				this.type = LineType.SUN_TO_SUN;
			}
		}
		else {
			this.type = LineType.OUTSIDE_TO_SUN;

		}
	}

	/**
	 * @return the points
	 */
	public DecompressedPoint getPoint(int index) {
		return points.get(index);
	}
	
	/**
	 * 
	 * @return number of points
	 */
	public int getSize() {
		return points.size();
	}

	/**
	 * @return the type
	 */
	public LineType getType() {
		return type;
	}
}
