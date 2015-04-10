package org.helioviewer.jhv.opengl.scenegraph;

import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.opengl.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.jhv.opengl.scenegraph.rt.GL3DRay;

/**
 * A {@link GL3DGroup} allows introducing a hierarchy within the scene graph. A group
 * is also a {@link GL3DShape} thus can be moved around in the world space, has its own
 * {@link GL3DDrawBits} and wraps its {@link GL3DAABBox} around its child nodes.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DGroup extends GL3DShape {
    protected GL3DNode first;
    protected GL3DNode last;
    
    private int numberOfNodes;
    
    public GL3DGroup(String name) {
    	super(name);
    }
    
    @Override
    public void shapeInit(GL3DState state) {
        GL3DNode node = this.first;
        while(node!=null) {
            if(node.depth!=0 && node.depth!=this.depth+1) {
                throw new RuntimeException("Group "+this+" is not directed acyclic. There is a loop!");
            }
            node.init(state);
            node = node.next;
        }
    }
    
    @Override
    public void shapeDraw(GL3DState state) {
        GL3DNode node = this.first;
        while(node!=null) {
            node.draw(state);
            node = node.next;
        }        	
    }
    
    @Override
    public boolean shapeHit(GL3DRay ray) {
        GL3DNode current = this.first;
        boolean wasHit = false;
        while(current!=null) {
            if(!(current==ray.getOriginShape())) {
                if(current.hit(ray)&&!wasHit) {
                    wasHit = true;
//                    Log.debug("GL3DGroup.shapeHit: Ray hit Group "+this);
                }
            }  
            current = current.next;
        } 
        return wasHit;
    }
    
    public void shapeUpdate(GL3DState state) {
        GL3DNode node = this.first;
        while(node!=null) {
            node.update(state);
            node = node.next;
        }
    }
    
    
    
    public void addNode(GL3DNode toAdd) {
        assert toAdd!=null : "Cannot add null node to group!";
        assert toAdd!=this : "Cannot add itself as a child!";
        assert toAdd.previous==null : "The node is already in a group!";
        
        if (this.last == null) {
            this.first = toAdd;
            this.last = toAdd;
            toAdd.previous = this;
        } else {
            toAdd.previous = this.last;
            toAdd.next = null;
            this.last.next = toAdd;
            this.last = toAdd;
        }
        toAdd.parent = this;
        
        this.numberOfNodes++;
        this.markAsChanged();
    }
    
    public void insertFirst(GL3DNode toInsert) {
        assert toInsert!=null : "Cannot insert null node";
        assert toInsert!=this : "Cannot insert itself as a child";
        assert toInsert.previous==null : "Node is already in a group";
        
        toInsert.next = this.first;
        this.first = toInsert;
        if(toInsert.next!=null) {
            toInsert.next.previous = toInsert;
        } else {
            this.last = toInsert;
        }
        toInsert.parent = this;
        toInsert.previous = null;
        
        this.numberOfNodes ++;
        this.markAsChanged();
    }

    public void insertNode(GL3DNode toInsert, GL3DNode afterNode) {
        assert afterNode!=null : "After Node is null";
        assert toInsert!=null : "Cannot insert null node";
        assert toInsert!=this : "Cannot insert itself as a child";
        assert toInsert.previous==null : "Node is already in a group";
        
        if(afterNode==this.last) {
            this.last = toInsert;
        } else {
            afterNode.next.previous = toInsert;
        }
        toInsert.next = afterNode.next;
        toInsert.previous = afterNode;
        toInsert.parent = this;
        afterNode.next = toInsert;
        
        this.numberOfNodes ++;
        this.markAsChanged();
    }
    
    public void moveNode(GL3DNode toInsert, int index) {
        assert index < this.numChildNodes();
        assert index >= 0;
        assert toInsert!=null;
        assert toInsert.parent == this;
        
        //find afterNode
        if(index==0) {
            this.removeNode(toInsert);
            this.insertFirst(toInsert);
        } else {
            this.removeNode(toInsert);
            int i=1;
            GL3DNode node = this.first;
            while(i<(index)&&node.next!=null) {
                node = node.next;
                i++;
            }

            if(node==toInsert) {
                throw new RuntimeException("Cannot move node before itself!");
            }
            this.insertNode(toInsert, node);
        }
    }
    
    @Override
    public void shapeDelete(GL3DState state) {
        deleteAll(state);
    }

    public void deleteNode(GL3DState state, GL3DNode toDelete) {
        removeNode(toDelete);
        toDelete.delete(state);
    }
    
    public void removeNode(GL3DNode toDelete) {
        
        assert toDelete!=null : "Cannot delete null node";
        assert toDelete.previous != null : "Node to delete is not in a group";
        assert this.first!=null : "No nodes present to delete";
        
        if(toDelete == this.first) {
            if(this.first.next!=null) {
                this.first = this.first.next;
                this.first.previous = this;
            } else {
                this.first = null;
                this.last = null;
            }
        } else {
            if(toDelete==this.last) {
                this.last = toDelete.previous;
                this.last.next = null;
            } else {
                toDelete.previous.next = toDelete.next;
                toDelete.next.previous = toDelete.previous;
            }
        }
        toDelete.parent = null;
        toDelete = null;
        this.numberOfNodes--;
        this.markAsChanged();
    }

    public void deleteAll(GL3DState state) {
        while(this.first!=null) {
            this.deleteNode(state, this.first);
        }
        this.markAsChanged();
    }
    
    public int numChildNodes() {
        return this.numberOfNodes;
    }

    public GL3DNode getChild(int index) {
        if(this.numberOfNodes < index) {
            return null;
        }
        
        GL3DNode node = this.first;
        for(int i = 0; i<index; i++) {
            node = node.next;
        }

        return node;
    }
    
    public GL3DAABBox buildAABB() {
        GL3DNode current = this.first;
        this.aabb = new GL3DAABBox();
        aabb.minWS = new Vector3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
        aabb.maxWS = new Vector3d(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);
        while(current!=null) {
            this.aabb.merge(current.buildAABB());
            current = current.next;
        }
        this.aabb.fromWStoOS(aabb.minWS, aabb.maxWS, wmI);
        return aabb;
    }

    @Override
    public void clearDrawBit(Bit bit) {
        super.clearDrawBit(bit);
        GL3DNode node = this.first;
        while(node!=null) {
            node.clearDrawBit(bit);
            node = node.next;
        }
    }
    
    public GL3DNode getFirst() {
        return first;
    }
}
