package org.helioviewer.jhv.opengl.model;

import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.math.Matrix4d;
import org.helioviewer.jhv.base.math.Quaternion3d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.math.Vector4d;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.base.wcs.CoordinateConversion;
import org.helioviewer.jhv.base.wcs.CoordinateSystem;
import org.helioviewer.jhv.base.wcs.CoordinateSystemChangeListener;
import org.helioviewer.jhv.base.wcs.CoordinateVector;
import org.helioviewer.jhv.internal_plugins.filter.opacity.OpacityFilter;
import org.helioviewer.jhv.opengl.camera.GL3DCamera;
import org.helioviewer.jhv.opengl.camera.GL3DCameraListener;
import org.helioviewer.jhv.opengl.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.jhv.opengl.scenegraph.GL3DGroup;
import org.helioviewer.jhv.opengl.scenegraph.GL3DMesh;
import org.helioviewer.jhv.opengl.scenegraph.GL3DNode;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.opengl.scenegraph.rt.GL3DRay;
import org.helioviewer.jhv.opengl.scenegraph.rt.GL3DRayTracer;
import org.helioviewer.jhv.opengl.shader.GL3DImageCoronaFragmentShaderProgram;
import org.helioviewer.jhv.opengl.shader.GL3DImageFragmentShaderProgram;
import org.helioviewer.jhv.opengl.shader.GL3DImageVertexShaderProgram;
import org.helioviewer.jhv.opengl.shader.GL3DShaderFactory;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.region.Region;
import org.helioviewer.jhv.viewmodel.region.StaticRegion;
import org.helioviewer.jhv.viewmodel.view.MetaDataView;
import org.helioviewer.jhv.viewmodel.view.RegionView;
import org.helioviewer.jhv.viewmodel.view.StandardFilterView;
import org.helioviewer.jhv.viewmodel.view.opengl.GL3DCoordinateSystemView;
import org.helioviewer.jhv.viewmodel.view.opengl.GL3DImageTextureView;
import org.helioviewer.jhv.viewmodel.view.opengl.GL3DView;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLVertexShaderProgram;

/**
 * This is the scene graph equivalent of an image layer sub view chain attached
 * to the GL3DLayeredView. It represents exactly one image layer in the view
 * chain
 * 
 * @author Simon Sp���rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DImageLayer extends GL3DGroup implements CoordinateSystemChangeListener, GL3DCameraListener {
	private static int nextLayerId = 0;
	private final int layerId;
	private Vector3d direction = new Vector3d(0, 0, 1);

	private GL3DImageSphere sphere;
	private GL3DImageCorona corona;
	private GL3DCircle circle;

	public int getLayerId() {
		return layerId;
	}

	protected GL3DView mainLayerView;
	protected GL3DImageTextureView imageTextureView;
	protected GL3DCoordinateSystemView coordinateSystemView;
	protected MetaDataView metaDataView;
	protected RegionView regionView;
	protected GL3DImageLayers layerGroup;

	protected GL3DNode accellerationShape;

	protected boolean doUpdateROI = true;

	private double lastViewAngle = 0.0;

	protected GL3DImageCoronaFragmentShaderProgram fragmentShader = null;
	protected GL3DImageFragmentShaderProgram sphereFragmentShader = null;

    public void coordinateSystemChanged(CoordinateSystem coordinateSystem) {
        this.markAsChanged();
    }

    public void updateMatrix(GL3DState state) {
        this.updateOrientation(state);
        super.updateMatrix(state);
    }

    private void updateOrientation(GL3DState state) {
        // Transform Orientation to Viewspace
        CoordinateVector orientationVector = getOrientation();
        CoordinateConversion toViewSpace = getCoordinateSystem().getConversion(state.activeCamera.getViewSpaceCoordinateSystem());
        
        Vector3d orientation = toViewSpace.convert(orientationVector).toVector3d().normalize();
        
        this.mv.set(Matrix4d.identity());
        
        
        if (!orientation.equals(new Vector3d(0, 0, 1))) {
            Vector3d orientationXY = new Vector3d(orientation.x, orientation.y, 0);
            double theta = Math.asin(orientationXY.y);
            Matrix4d thetaRotation = Matrix4d.rotation(theta, new Vector3d(1, 0, 0));
            // Log.debug("GL3DOrientedGroup: Rotating Theta "+theta);
            this.mv.multiply(thetaRotation);
        }
        
        //if (!(orientation.equals(new Vector3d(0, 1, 0)))) {
            Vector3d orientationXZ = new Vector3d(orientation.x, 0, orientation.z);
            double phi = Math.acos(orientationXZ.z);
            if (orientationXZ.x < 0) {
                phi = 0 - phi;
            }
            phi += 0.7;
            // Log.debug("GL3DOrientedGroup: Rotating Phi "+phi);
            Matrix4d phiRotation = Matrix4d.rotation(phi, new Vector3d(0, 1, 0));
            this.mv.multiply(phiRotation);
        //}
        
    }
    
	public GL3DImageLayer(String name, GL3DView mainLayerView) {
		super(name);
		((OpacityFilter)mainLayerView.getAdapter(StandardFilterView.class).getFilter()).setImageLayer(this);
		layerId = nextLayerId++;

		this.mainLayerView = mainLayerView;
		if (this.mainLayerView == null) {
			throw new NullPointerException(
					"Cannot create GL3DImageLayer from null Layer");
		}

		this.imageTextureView = this.mainLayerView
				.getAdapter(GL3DImageTextureView.class);
		if (this.imageTextureView == null) {
			throw new IllegalStateException(
					"Cannot create GL3DImageLayer when no GL3DImageTextureView is present in Layer");
		}

		this.coordinateSystemView = this.mainLayerView
				.getAdapter(GL3DCoordinateSystemView.class);
		if (this.coordinateSystemView == null) {
			throw new IllegalStateException(
					"Cannot create GL3DImageLayer when no GL3DCoordinateSystemView is present in Layer");
		}

		this.metaDataView = this.mainLayerView.getAdapter(MetaDataView.class);
		if (this.metaDataView == null) {
			throw new IllegalStateException(
					"Cannot create GL3DImageLayer when no MetaDataView is present in Layer");
		}
		this.regionView = this.mainLayerView.getAdapter(RegionView.class);
		if (this.regionView == null) {
			throw new IllegalStateException(
					"Cannot create GL3DImageLayer when no RegionView is present in Layer");
		}

		getCoordinateSystem().addListener(this);
		this.doUpdateROI = true;
		this.markAsChanged();
	}

	public void shapeInit(GL3DState state) {
		this.createImageMeshNodes(state.gl);

		CoordinateVector orientationVector = this.getOrientation();
		CoordinateConversion toViewSpace = this.getCoordinateSystem()
				.getConversion(
						state.activeCamera.getViewSpaceCoordinateSystem());
		Vector3d orientation = toViewSpace.convert(orientationVector).toVector3d().normalize();
		double phi = 0.0;
		if (!(orientation.equals(new Vector3d(0, 1, 0)))) {
			Vector3d orientationXZ = new Vector3d(orientation.x, 0,
					orientation.z);
			phi = Math.acos(orientationXZ.z);
			if (orientationXZ.x < 0) {
				phi = 0 - phi;
			}
		}
		this.accellerationShape = new GL3DHitReferenceShape(true, phi);
		this.addNode(this.accellerationShape);

        getCoordinateSystem().addListener(this);
        super.shapeInit(state);

		this.doUpdateROI = true;
		this.markAsChanged();
		Quaternion3d phiRotation = Quaternion3d.createRotation(2 * Math.PI - phi,
				new Vector3d(0, 1, 0));
		state.activeCamera.getRotation().set(phiRotation);
		state.activeCamera.updateCameraTransformation();
		updateROI(state.activeCamera);
	}

	protected void createImageMeshNodes(GL gl) {

		GL3DImageVertexShaderProgram vertex = new GL3DImageVertexShaderProgram();
		GLVertexShaderProgram vertexShader = GL3DShaderFactory
				.createVertexShaderProgram(gl, vertex);
		this.imageTextureView.setVertexShader(vertex);
		this.imageTextureView.metadata = this.metaDataView.getMetaData();

		double xOffset = (this.imageTextureView.metadata
        .getPhysicalUpperRight().x + this.imageTextureView.metadata
                .getPhysicalLowerLeft().x)
				/ (2.0 * this.imageTextureView.metadata.getPhysicalImageWidth());
		double yOffset = (this.imageTextureView.metadata
        .getPhysicalUpperRight().y + this.imageTextureView.metadata
                .getPhysicalLowerLeft().y)
				/ (2.0 * this.imageTextureView.metadata
						.getPhysicalImageHeight());
		vertex.setDefaultOffset((float) xOffset, (float) yOffset);

		MetaData metadata = this.imageTextureView.metadata;

		if (metadata.hasSphere()) {
			this.sphereFragmentShader = new GL3DImageFragmentShaderProgram();
			GLFragmentShaderProgram sphereFragmentShader = GL3DShaderFactory
					.createFragmentShaderProgram(gl, this.sphereFragmentShader);
			sphere = new GL3DImageSphere(imageTextureView, vertexShader,
					sphereFragmentShader, this);
			circle = new GL3DCircle(Constants.SUN_RADIUS, new Vector4d(0.5f,
					0.5f, 0.5f, 1.0f), "Circle", this);
			this.sphereFragmentShader
					.setCutOffRadius((float) (Constants.SUN_RADIUS / this.imageTextureView.metadata
							.getPhysicalImageWidth()));
			this.addNode(circle);
			this.addNode(sphere);
		}
		if (metadata.hasCorona()) {
			this.fragmentShader = new GL3DImageCoronaFragmentShaderProgram();
			GLFragmentShaderProgram coronaFragmentShader = GL3DShaderFactory
					.createFragmentShaderProgram(gl, fragmentShader);
			corona = new GL3DImageCorona(imageTextureView, vertexShader,
					coronaFragmentShader, this);
			this.fragmentShader
					.setCutOffRadius((float) (Constants.SUN_RADIUS / this.imageTextureView.metadata
							.getPhysicalImageWidth()));
			this.fragmentShader.setDefaultOffset(metadata.getSunPixelPosition().x / metadata.getResolution().getX() - xOffset,
					metadata.getSunPixelPosition().y
							/ metadata.getResolution().getY() - yOffset);
			this.addNode(corona);
		}
	}

	protected GL3DImageMesh getImageCorona() {
		return this.corona;
	}

	protected GL3DImageMesh getImageSphere() {
		return this.sphere;
	}

	protected GL3DMesh getCircle() {
		return this.circle;
	}

	public void shapeUpdate(GL3DState state) {
		super.shapeUpdate(state);
		if (doUpdateROI) {
			this.updateROI(state.activeCamera);
			doUpdateROI = false;
			this.accellerationShape.setUnchanged();
		}
	}

	public void cameraMoved(GL3DCamera camera) {
		doUpdateROI = true;
		if (this.accellerationShape != null)
			this.accellerationShape.markAsChanged();

		cameraMoving(camera);
	}

	public double getLastViewAngle() {
		return lastViewAngle;
	}

	public void cameraMoving(GL3DCamera camera) {
		Matrix4d camTrans = camera.getRotation().toMatrix().inverse();
		Vector3d camDirection = new Vector3d(0, 0, 1);
		camDirection = camTrans.multiply(camDirection).normalize();

		double angle = (Math.acos(camDirection.dot(direction)) / Math.PI * 180.0);
		double maxAngle = 60;
		double minAngle = 30;
		if (angle != lastViewAngle) {
			lastViewAngle = angle;
			float alpha = (float) ((Math.abs(90 - lastViewAngle) - minAngle) / (maxAngle - minAngle));
			if (this.fragmentShader != null)
				this.fragmentShader.changeAlpha(alpha);
		}
	}

	public Vector3d getLayerDirection() {
		return direction;
	}

	public void setLayerDirection(Vector3d direction) {
		this.direction = direction;
	}

	public CoordinateSystem getCoordinateSystem() {
		return this.coordinateSystemView.getCoordinateSystem();
	}

	public CoordinateVector getOrientation() {
		// Log.debug("GL3DImageLayer: Orientation: "+this.coordinateSystemView.getOrientation());
		return this.coordinateSystemView.getOrientation();
	}

	private void updateROI(GL3DCamera activeCamera) {
		MetaData metaData = metaDataView.getMetaData();

		if (metaData == null) {
			// No Image Data found
			return;
		}

		GL3DRayTracer rayTracer = new GL3DRayTracer(this.accellerationShape,
				activeCamera);

		// Shoot Rays in the corners of the viewport
		int width = (int) activeCamera.getWidth();
		int height = (int) activeCamera.getHeight();
		List<GL3DRay> regionTestRays = new ArrayList<GL3DRay>();
		
		for (int i = 0; i <= 10; i++) {
			for (int j = 0; j <= 10; j++) {

				regionTestRays.add(rayTracer.cast(i * (width / 10), j
						* (height / 10)));
			}
		}

		double minPhysicalX = Double.MAX_VALUE;
		double minPhysicalY = Double.MAX_VALUE;
		double minPhysicalZ = Double.MAX_VALUE;
		double maxPhysicalX = -Double.MAX_VALUE;
		double maxPhysicalY = -Double.MAX_VALUE;
		double maxPhysicalZ = -Double.MAX_VALUE;

		CoordinateVector orientationVector = this.getOrientation();
		CoordinateConversion toViewSpace = this.getCoordinateSystem()
				.getConversion(activeCamera.getViewSpaceCoordinateSystem());

		Vector3d orientation = toViewSpace.convert(orientationVector).toVector3d().normalize();

		Matrix4d phiRotation = null;

		if (!(orientation.equals(new Vector3d(0, 1, 0)))) {
			Vector3d orientationXZ = new Vector3d(orientation.x, 0,
					orientation.z);
			double phi = Math.acos(orientationXZ.z);
			if (orientationXZ.x < 0) {
				phi = 0 - phi;
			}
			phi = 2 * Math.PI - phi;
			phiRotation = Matrix4d.rotation(phi, new Vector3d(0, 1, 0));
		}

		for (GL3DRay ray : regionTestRays) {
			Vector3d hitPoint = ray.getHitPoint();
			if (hitPoint != null) {
				hitPoint = this.wmI.multiply(hitPoint);
				
				double x = phiRotation.m[0] * hitPoint.x + phiRotation.m[4]
						* hitPoint.y + phiRotation.m[8] * hitPoint.z
						+ phiRotation.m[12];
				double y = phiRotation.m[1] * hitPoint.x + phiRotation.m[5]
						* hitPoint.y + phiRotation.m[9] * hitPoint.z
						+ phiRotation.m[13];
				double z = phiRotation.m[2] * hitPoint.x + phiRotation.m[6]
						* hitPoint.y + phiRotation.m[10] * hitPoint.z
						+ phiRotation.m[14];
				
				minPhysicalX = Math.min(minPhysicalX, x);
				minPhysicalY = Math.min(minPhysicalY, y);
				minPhysicalZ = Math.min(minPhysicalZ, z);
				maxPhysicalX = Math.max(maxPhysicalX, x);
				maxPhysicalY = Math.max(maxPhysicalY, y);
				maxPhysicalZ = Math.max(maxPhysicalZ, z);
				// Log.debug("GL3DImageLayer: Hitpoint: "+hitPoint+" - "+ray.isOnSun);
			}
		}
		
		// Restrict maximal region to physically available region
		minPhysicalX = Math.max(minPhysicalX, metaData.getPhysicalLowerLeft().x);
		minPhysicalY = Math.max(minPhysicalY, metaData.getPhysicalLowerLeft().y);
		maxPhysicalX = Math.min(maxPhysicalX, metaData.getPhysicalUpperRight().x);
		maxPhysicalY = Math.min(maxPhysicalY, metaData.getPhysicalUpperRight().y);

		minPhysicalX -= Math.abs(minPhysicalX) * 0.1;
		minPhysicalY -= Math.abs(minPhysicalY) * 0.1;
		maxPhysicalX += Math.abs(maxPhysicalX) * 0.1;
		maxPhysicalY += Math.abs(maxPhysicalY) * 0.1;
		if (minPhysicalX < metaData.getPhysicalLowerLeft().x)
			minPhysicalX = metaData.getPhysicalLowerLeft().x;
		if (minPhysicalY < metaData.getPhysicalLowerLeft().y)
			minPhysicalY = metaData.getPhysicalLowerLeft().y;
		if (maxPhysicalX > metaData.getPhysicalUpperRight().x)
			maxPhysicalX = metaData.getPhysicalUpperRight().x;
		if (maxPhysicalY > metaData.getPhysicalUpperRight().y)
			maxPhysicalY = metaData.getPhysicalUpperRight().y;

		double regionWidth = maxPhysicalX - minPhysicalX;
		double regionHeight = maxPhysicalY - minPhysicalY;

		if (regionWidth > 0 && regionHeight > 0) {
			Region newRegion = StaticRegion.createAdaptedRegion(minPhysicalX,
					minPhysicalY, regionWidth, regionHeight);
			// Log.debug("GL3DImageLayer: '"+getName()+" set its region");
			this.regionView.setRegion(newRegion, null);
		} else if (Double.isInfinite(regionHeight)
				|| Double.isInfinite(regionWidth)) {

		} else {
			Log.error("Illegal Region calculated! " + regionWidth + ":"
					+ regionHeight + ". x = " + minPhysicalX + " - "
					+ maxPhysicalX + ", y = " + minPhysicalY + " - "
					+ maxPhysicalY);
		}

	}

	public void setCoronaVisibility(boolean visible) {
		GL3DNode node = this.first;
		while (node != null) {
			if (node instanceof GL3DImageCorona) {
				node.drawBits.set(Bit.Hidden, !visible);
			}

			node = node.getNext();
		}
	}

	public GL3DImageTextureView getImageTextureView() {
		return this.imageTextureView;
	}

	public void setLayerGroup(GL3DImageLayers layers) {
		layerGroup = layers;
	}

	public GL3DImageLayers getLayerGroup() {
		return layerGroup;
	}

	public GL3DImageFragmentShaderProgram getSphereFragmentShader() {
		return sphereFragmentShader;
	}

}
