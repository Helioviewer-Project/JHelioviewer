package org.helioviewer.jhv.opengl.scenegraph;

import java.nio.Buffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.math.Vector4d;

import com.jogamp.common.nio.Buffers;


/**
 * A {@link GL3DBuffer} is a buffer object on the graphic card. Buffer objects
 * are used to store vertex positions, colors, normals, texture coordinates and
 * indices. The factory methods should be used to create a buffer object.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DBuffer {
    protected int id;
    protected GL3DBufferType type;
    protected GL3DBufferAttribute attribute;
    protected GL3DBufferDataType dataType;

    // Number of coordinates per element;
    protected int elementSize;

    protected int numberOfElements;

    // Offset between 2 elements within the array
    private int stride = 0;

    private int offset = 0;

    private Buffer data;

    private boolean isInUse = true;
    
    
    
    public static GL3DBuffer createIndexBuffer(GL3DState state, int[] indices) {
        IntBuffer buffer = IntBuffer.wrap(indices);

        return new GL3DBuffer(state, GL3DBufferType.ELEMENT, GL3DBufferAttribute.NONE, buffer, GL3DBufferDataType.UNSIGNED_INT, 1);
    }

    public static GL3DBuffer createIndexBuffer(GL3DState state, List<Integer> indices) {
        IntBuffer buffer = IntBuffer.allocate(indices.size());
        for (Integer i : indices) {
            buffer.put(i);
        }
        buffer.flip();

        return new GL3DBuffer(state, GL3DBufferType.ELEMENT, GL3DBufferAttribute.NONE, buffer, GL3DBufferDataType.UNSIGNED_INT, 1);
    }

    public static GL3DBuffer createColorBuffer(GL3DState state, List<Vector4d> colors) {
        DoubleBuffer buffer = DoubleBuffer.allocate(colors.size() * 4);
        for (Vector4d color : colors) {
            buffer.put(color.x);
            buffer.put(color.y);
            buffer.put(color.z);
            buffer.put(color.w);
        }
        buffer.flip();

        return new GL3DBuffer(state, GL3DBufferType.ARRAY, GL3DBufferAttribute.COLOR, buffer, 4);
    }

    public static GL3DBuffer create2DTextureCoordinateBuffer(GL3DState state, List<Vector2d> texCoords) {
        DoubleBuffer buffer = DoubleBuffer.allocate(texCoords.size() * 2);
        for (Vector2d coord : texCoords) {
        	buffer.put(coord.x);
            buffer.put(coord.y);
        }
        buffer.flip();

        return new GL3DBuffer(state, GL3DBufferType.ARRAY, GL3DBufferAttribute.TEXTURE, buffer, 2);
    }

    public static GL3DBuffer createPositionBuffer(GL3DState state, List<Vector3d> vertices) {
        DoubleBuffer buffer = DoubleBuffer.allocate(vertices.size() * 3);
        for (Vector3d vertex : vertices) {
            buffer.put(vertex.x);
            buffer.put(vertex.y);
            buffer.put(vertex.z);
        }
        buffer.flip();

        return new GL3DBuffer(state, GL3DBufferType.ARRAY, GL3DBufferAttribute.VERTEX, buffer, 3);
    }

    public static GL3DBuffer createPositionBuffer(GL3DState state, Vector3d[] vertices) {
        DoubleBuffer buffer = DoubleBuffer.allocate(vertices.length * 3);
        for (Vector3d vertex : vertices) {
            buffer.put(vertex.x);
            buffer.put(vertex.y);
            buffer.put(vertex.z);
        }
        buffer.flip();

        return new GL3DBuffer(state, GL3DBufferType.ARRAY, GL3DBufferAttribute.VERTEX, buffer, 3);
    }

    public static GL3DBuffer createNormalBuffer(GL3DState state, List<Vector3d> normals) {
        DoubleBuffer buffer = DoubleBuffer.allocate(normals.size() * 3);
        for (Vector3d normal : normals) {
            buffer.put(normal.x);
            buffer.put(normal.y);
            buffer.put(normal.z);
        }
        buffer.flip();

        return new GL3DBuffer(state, GL3DBufferType.ARRAY, GL3DBufferAttribute.NORMAL, buffer, 3);
    }

    private GL3DBuffer(GL3DState state, GL3DBufferType type, GL3DBufferAttribute attribute, IntBuffer data, int elementSize) {
        this(state, type, attribute, data, GL3DBufferDataType.INT, elementSize);
    }

    private GL3DBuffer(GL3DState state, GL3DBufferType type, GL3DBufferAttribute attribute, DoubleBuffer data, int elementSize) {
        this(state, type, attribute, data, GL3DBufferDataType.DOUBLE, elementSize);
    }

    private GL3DBuffer(GL3DState state, GL3DBufferType type, GL3DBufferAttribute attribute, FloatBuffer data, int elementSize) {
        this(state, type, attribute, data, GL3DBufferDataType.FLOAT, elementSize);
    }

    private GL3DBuffer(GL3DState state, GL3DBufferType type, GL3DBufferAttribute attribute, Buffer data, GL3DBufferDataType dataType, int elementSize) {
        this.dataType = dataType;
        this.type = type;
        this.attribute = attribute;
        this.data = data;
        this.elementSize = elementSize;
        this.stride = elementSize;
        this.numberOfElements = data.capacity() / this.elementSize;

        this.isInUse = this.numberOfElements > 0;
        // this.isInUse = true;

        if (this.isInUse) {
            this.generate(state);
            this.bufferData(state);
        }
    }

    private void generate(GL3DState state) {
        int[] tmpId = new int[1];
        state.gl.glGenBuffers(1, tmpId, 0);
        this.id = tmpId[0];
    }

    public void delete(GL3DState state) {
        this.data.clear();
        this.data = null;
        state.gl.glDeleteBuffers(1, new int[] { this.id }, 0);
    }

    private void bufferData(GL3DState state) {
        state.gl.glBindBuffer(this.type.id, this.id);
        state.gl.glBufferData(this.type.id, this.data.capacity() * this.dataType.size, data, GL.GL_DYNAMIC_DRAW);
    }

    public void enable(GL3DState state) {
        if (this.isInUse) {
            // Index Buffer does not need to be enabled
            if (this.attribute != GL3DBufferAttribute.NONE) {
                state.gl.glEnableClientState(this.attribute.id);
            }

            state.gl.glBindBuffer(this.type.id, this.id);

            switch (this.attribute) {
            case VERTEX:
                state.gl.glVertexPointer(this.elementSize, this.dataType.id, this.stride * this.dataType.size, this.offset);
                // Log.debug("GL3DBuffer.enable Vertex   id="+this.id);
                break;
            case NORMAL:
                state.gl.glNormalPointer(this.dataType.id, this.stride * this.dataType.size, this.offset);
                // Log.debug("GL3DBuffer.enable Normal   id="+this.id);
                break;
            case COLOR:
                state.gl.glColorPointer(this.elementSize, this.dataType.id, this.stride * this.dataType.size, this.offset);
                // Log.debug("GL3DBuffer.enable Color    id="+this.id);
                break;
            case TEXTURE:
            	state.gl.glTexCoordPointer(this.elementSize, this.dataType.id, this.stride * this.dataType.size, this.offset);
                // Log.debug("GL3DBuffer.enable TexCoord id="+this.id);
                break;
            case NONE:
                // Log.debug("GL3DBuffer.enable Element  id="+this.id+" (No Pointer)");
            }
            // Log.debug("GL3DBuffer.enable id="+this.id);
        }
    }

    public void disable(GL3DState state) {
        if (this.isInUse) {
            if (this.attribute != GL3DBufferAttribute.NONE) {
                state.gl.glDisableClientState(this.attribute.id);
            }
            state.gl.glBindBuffer(this.type.id, 0);
            // Log.debug("GL3DBuffer.disable          id="+this.id);
        }
    }

    public enum GL3DBufferDataType {
        FLOAT(GL2.GL_FLOAT, Buffers.SIZEOF_FLOAT), DOUBLE(GL2.GL_DOUBLE, Buffers.SIZEOF_DOUBLE), INT(GL2.GL_INT, Buffers.SIZEOF_INT), UNSIGNED_INT(GL2.GL_UNSIGNED_INT, Buffers.SIZEOF_INT);

        public int size;
        public int id;

        private GL3DBufferDataType(int id, int size) {
            this.size = size;
            this.id = id;
        }
    }

    public enum GL3DBufferType {
        ARRAY(GL.GL_ARRAY_BUFFER), ELEMENT(GL.GL_ELEMENT_ARRAY_BUFFER);

        public int id;

        private GL3DBufferType(int id) {
            this.id = id;
        }
    }

    public enum GL3DBufferAttribute {
        VERTEX(GL2.GL_VERTEX_ARRAY), COLOR(GL2.GL_COLOR_ARRAY), NORMAL(GL2.GL_NORMAL_ARRAY), TEXTURE(GL2.GL_TEXTURE_COORD_ARRAY), NONE(-1);

        public int id;

        private GL3DBufferAttribute(int id) {
            this.id = id;
        }
    }
}
