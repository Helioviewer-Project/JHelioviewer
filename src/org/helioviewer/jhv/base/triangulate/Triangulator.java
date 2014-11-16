package org.helioviewer.jhv.base.triangulate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.helioviewer.jhv.base.math.Matrix4d;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector3d;

class Triangulator
{
    private class HeapNode
    {
        int index,prev,next;
        double ratio;

        void set(HeapNode _n)
        {
            index=_n.index;
            prev=_n.prev;
            next=_n.next;
            ratio=_n.ratio;
        }
    }

    private class PntNode
    {
        int pnt;
        int next;

        PntNode(int _pnt,int _next)
        {
            pnt=_pnt;
            next=_next;
        }
    }

    private class ListNode
    {
        int index;
        int prev;
        int next;
        int convex;
        int vcntIndex;

        ListNode(int ind)
        {
            index=ind;
            prev=-1;
            next=-1;
            convex=0;
            vcntIndex=-1;
        }
    }

    private class Distance
    {
        int ind;
        double dist;
    }

    private class Left
    {
        int ind;
        int index;
    }

    private class Triangle
    {
        int a;
        int b;
        int c;

        Triangle(int _a,int _b,int _c)
        {
            a=_a;
            b=_b;
            c=_c;
        }
    }

    private GeometryInfo gInfo=null;
    private int faces[]=null;
    private int loops[]=null;
    private int chains[]=null;
    Vector2d points[]=null;
    private ArrayList<Triangle> triangles=new ArrayList<Triangle>();
    private ListNode list[]=null;

    private int numPoints=0;
    private int maxNumPoints=0;
    private int numList=0;
    private int maxNumList=0;
    private int numLoops=0;
    private int maxNumLoops=0;
    private int numFaces=0;
    private int firstNode=0;
    private int numChains=0;
    private int maxNumChains=0;

    private ArrayList<Vector2d> pUnsorted=new ArrayList<Vector2d>();

    private int loopMin,loopMax;
    private PntNode vtxList[]=null;
    private int numVtxList=0;
    private int numReflex=0;
    private int reflexVertices;

    private Distance distances[]=null;
    private int maxNumDist=0;
    private Left leftMost[]=null;
    private int maxNumLeftMost=0;

    private HeapNode heap[]=null;
    private int numHeap=0;
    private int maxNumHeap=0;
    private int numZero=0;

    private int maxNumPolyArea=0;
    private double polyArea[]=null;

    private int stripCounts[]=null;
    private int vertexIndices[]=null;
    private Vector3d vertices[]=null;

    private boolean ccwLoop=true;
    private boolean earsRandom=true;
    private boolean earsSorted=true;

    private int identCntr;
    private double epsilon=1.0e-12;

    private final static double ZERO=1.0e-8;
    private final static int INC_LIST_BK=100;
    private final static int INC_LOOP_BK=20;
    private final static int INC_POINT_BK=100;
    private final static int INC_DIST_BK=50;

    public Triangulator()
    {
        earsRandom=false;
        earsSorted=false;
    }

    public void triangulate(GeometryInfo gi)
    {
        int i,j,k;
        int sIndex=0,index,currLoop,lastInd,ind;
        boolean proceed;
        boolean reset=false;

        boolean done[]=new boolean[1];
        boolean gotIt[]=new boolean[1];

        gi.indexify();

        vertices=gi.getCoordinates();
        if(vertices!=null)
            vertexIndices=gi.getCoordinateIndices();
        else
            vertexIndices=null;

        gInfo=gi;
        stripCounts=gi.getStripCounts();

        faces=gi.getContourCounts();
        if(faces==null)
        {
            faces=new int[stripCounts.length];
            for(i=0;i<stripCounts.length;i++)
                faces[i]=1;
        }

        numFaces=faces.length;

        maxNumLoops=0;
        maxNumList=0;
        maxNumPoints=0;
        maxNumDist=0;
        maxNumLeftMost=0;

        for(i=0;i<faces.length;i++)
        {
            maxNumLoops+=faces[i];
            for(j=0;j<faces[i];j++,sIndex++)
                maxNumList+=(stripCounts[sIndex]+1);
        }

        maxNumList+=20;

        loops=new int[maxNumLoops];
        list=new ListNode[maxNumList];

        numVtxList=0;
        numReflex=0;

        triangles.clear();

        numChains=0;
        numPoints=0;
        numLoops=0;
        numList=0;
        sIndex=0;
        index=0;

        for(int f:faces)
            for(j=0;j<f;j++,sIndex++)
            {
                currLoop=makeLoopHeader();
                lastInd=loops[currLoop];

                for(k=0;k<stripCounts[sIndex];k++)
                {
                    list[numList]=new ListNode(vertexIndices[index]);
                    ind=numList++;

                    insertAfter(lastInd,ind);
                    list[ind].vcntIndex=index;

                    lastInd=ind;
                    index++;
                }

                deleteHook(currLoop);
            }

        epsilon=ZERO;

        int i1=0;
        int i2=0;
        for(j=0;j<numFaces;++j)
        {
            ccwLoop=true;
            done[0]=false;
            i2=i1+faces[j];

            if(faces[j]>1)
                proceed=true;
            else if(simpleFace(loops[i1]))
                proceed=false;
            else
                proceed=true;

            if(proceed)
            {
                for(int lpIndex=0;lpIndex<faces[j];lpIndex++)
                    preProcessList(i1+lpIndex);

                projectFace(i1,i2);
                cleanPolyhedralFace(i1,i2);
                if(faces[j]==1)
                    determineOrientation(loops[i1]);
                else
                    adjustOrientation(i1,i2);

                if(faces[j]>1)
                    prepareNoHashEdges(i1,i2);

                for(i=i1;i<i2;++i)
                    classifyAngles(loops[i]);

                if(faces[j]>1)
                    constructBridges(i1,i2);

                firstNode=loops[i1];
                prepareNoHashPnts(i1);
                classifyEars(loops[i1]);
                done[0]=false;

                while(!done[0])
                {
                    if(!clipEar(done))
                    {
                        if(reset)
                        {
                            ind=firstNode;

                            loops[i1]=ind;
                            if(desperate(ind,i1,done))
                            {
                                if(!letsHope(ind))
                                    return;
                            }
                            else
                                reset=false;
                        }
                        else
                        {
                            ind=firstNode;

                            classifyEars(ind);
                            reset=true;
                        }
                    }
                    else
                        reset=false;

                    if(done[0])
                    {
                        ind=getNextChain(gotIt);
                        if(gotIt[0])
                        {
                            firstNode=ind;
                            loops[i1]=ind;
                            prepareNoHashPnts(i1);
                            classifyEars(ind);
                            reset=false;
                            done[0]=false;
                        }
                    }
                }
            }

            i1=i2;
        }

        gInfo.setContourCounts(null);
        gInfo.setStripCounts(null);

        int currIndex=0;
        int newVertexIndices[]=new int[triangles.size()*3];
        for(Triangle t:triangles)
        {
            newVertexIndices[currIndex++]=vertexIndices[list[t.a].vcntIndex];
            newVertexIndices[currIndex++]=vertexIndices[list[t.b].vcntIndex];
            newVertexIndices[currIndex++]=vertexIndices[list[t.c].vcntIndex];
        }
        gInfo.setCoordinateIndices(newVertexIndices);
    }

    private void constructBridges(int loopMin,int loopMax)
    {
        int i,numDist,numLeftMost;

        int[] i0=new int[1];
        int[] ind0=new int[1];
        int[] i1=new int[1];
        int[] ind1=new int[1];

        int[] iTmp=new int[1];
        int[] indTmp=new int[1];

        numLeftMost=loopMax-loopMin-1;

        if(numLeftMost>maxNumLeftMost)
        {
            maxNumLeftMost=numLeftMost;
            leftMost=new Left[numLeftMost];
        }

        findLeftMostVertex(loops[loopMin],ind0,i0);
        int j=0;
        for(i=loopMin+1;i<loopMax;++i)
        {
            findLeftMostVertex(loops[i],indTmp,iTmp);
            leftMost[j]=new Left();
            leftMost[j].ind=indTmp[0];
            leftMost[j].index=iTmp[0];

            j++;
        }

        Arrays.sort(leftMost,0,numLeftMost,new Comparator<Left>()
        {
            public int compare(Left _a,Left _b)
            {
                return l_comp(_a,_b);
            }
        });

        numDist=numPoints+2*numLoops;
        maxNumDist=numDist;
        distances=new Distance[numDist];
        for(int k=0;k<maxNumDist;k++)
            distances[k]=new Distance();

        for(j=0;j<numLeftMost;++j)
        {
            findBridge(ind0[0],i0[0],leftMost[j].index,ind1,i1);
            if(i1[0]==leftMost[j].index)
                simpleBridge(ind1[0],leftMost[j].ind);
            else
                insertBridge(ind1[0],i1[0],leftMost[j].ind,leftMost[j].index);
        }
    }

    private boolean findBridge(int ind,int i,int start,int[] ind1,int[] i1)
    {
        int i0,i2,j,numDist=0;
        int ind0,ind2;
        BBox bb;
        Distance old[]=null;
        boolean convex,coneOk;

        ind1[0]=ind;
        i1[0]=i;
        if(i1[0]==start)
            return true;
        if(numDist>=maxNumDist)
        {
            maxNumDist+=Triangulator.INC_DIST_BK;
            old=distances;
            distances=new Distance[maxNumDist];
            System.arraycopy(old,0,distances,0,old.length);
            for(int k=old.length;k<maxNumDist;k++)
                distances[k]=new Distance();
        }

        distances[numDist].dist=baseLength(points[start],points[i1[0]]);
        distances[numDist].ind=ind1[0];
        ++numDist;

        ind1[0]=list[ind1[0]].next;
        i1[0]=list[ind1[0]].index;
        while(ind1[0]!=ind)
        {
            if(i1[0]==start)
                return true;
            if(numDist>=maxNumDist)
            {
                maxNumDist+=Triangulator.INC_DIST_BK;
                old=distances;
                distances=new Distance[maxNumDist];
                System.arraycopy(old,0,distances,0,old.length);
                for(int k=old.length;k<maxNumDist;k++)
                    distances[k]=new Distance();
            }

            distances[numDist].dist=baseLength(points[start],points[i1[0]]);
            distances[numDist].ind=ind1[0];
            ++numDist;
            ind1[0]=list[ind1[0]].next;
            i1[0]=list[ind1[0]].index;
        }

        Arrays.sort(distances,0,numDist,new Comparator<Distance>()
        {
            public int compare(Distance _a,Distance _b)
            {
                return d_comp(_a,_b);
            }
        });

        for(j=0;j<numDist;++j)
        {
            ind1[0]=distances[j].ind;
            i1[0]=list[ind1[0]].index;
            if(i1[0]<=start)
            {
                ind0=list[ind1[0]].prev;
                i0=list[ind0].index;
                ind2=list[ind1[0]].next;
                i2=list[ind2].index;
                convex=list[ind1[0]].convex>0;

                coneOk=isInCone(i0,i1[0],i2,start,convex);
                if(coneOk)
                {
                    bb=new BBox(this,i1[0],start);
                    if(!noHashEdgeIntersectionExists(bb,-1,-1,ind1[0],-1))
                        return true;
                }
            }
        }

        for(j=0;j<numDist;++j)
        {
            ind1[0]=distances[j].ind;
            i1[0]=list[ind1[0]].index;
            ind0=list[ind1[0]].prev;
            i0=list[ind0].index;
            ind2=list[ind1[0]].next;
            i2=list[ind2].index;
            bb=new BBox(this,i1[0],start);
            if(!noHashEdgeIntersectionExists(bb,-1,-1,ind1[0],-1))
                return true;
        }

        ind1[0]=ind;
        i1[0]=i;

        return false;
    }

    private void prepareNoHashEdges(int currLoopMin,int currLoopMax)
    {
        loopMin=currLoopMin;
        loopMax=currLoopMax;
    }

    private boolean checkArea(int ind4,int ind5)
    {
        int ind1,ind2;
        int i0,i1,i2;
        double area=0.0,area1=0,area2=0.0;

        i0=list[ind4].index;
        ind1=list[ind4].next;
        i1=list[ind1].index;

        while(ind1!=ind5)
        {
            ind2=list[ind1].next;
            i2=list[ind2].index;
            area=stableDet2D(i0,i1,i2);
            area1+=area;
            ind1=ind2;
            i1=i2;
        }

        if((area1<=Triangulator.ZERO))
            return false;

        ind1=list[ind5].next;
        i1=list[ind1].index;
        while(ind1!=ind4)
        {
            ind2=list[ind1].next;
            i2=list[ind2].index;
            area=stableDet2D(i0,i1,i2);
            area2+=area;
            ind1=ind2;
            i1=i2;
        }

        return !(area2<=Triangulator.ZERO);
    }

    private boolean checkBottleNeck(int i1,int i2,int i3,int ind4)
    {
        int ind5;
        int i4,i5;
        boolean flag;

        i4=i1;

        ind5=list[ind4].prev;
        i5=list[ind5].index;
        if((i5!=i2)&&(i5!=i3))
        {
            flag=pntInTriangle(i1,i2,i3,i5);
            if(flag)
                return true;
        }

        if(i2<=i3)
        {
            if(i4<=i5)
                flag=segIntersect(i2,i3,i4,i5,-1);
            else
                flag=segIntersect(i2,i3,i5,i4,-1);
        }
        else
        {
            if(i4<=i5)
                flag=segIntersect(i3,i2,i4,i5,-1);
            else
                flag=segIntersect(i3,i2,i5,i4,-1);
        }
        if(flag)
            return true;

        ind5=list[ind4].next;
        i5=list[ind5].index;

        if((i5!=i2)&&(i5!=i3))
        {
            flag=pntInTriangle(i1,i2,i3,i5);
            if(flag)
                return true;
        }

        if(i2<=i3)
        {
            if(i4<=i5)
                flag=segIntersect(i2,i3,i4,i5,-1);
            else
                flag=segIntersect(i2,i3,i5,i4,-1);
        }
        else
        {
            if(i4<=i5)
                flag=segIntersect(i3,i2,i4,i5,-1);
            else
                flag=segIntersect(i3,i2,i5,i4,-1);
        }

        if(flag)
            return true;

        ind5=list[ind4].next;
        i5=list[ind5].index;
        while(ind5!=ind4)
        {
            if(i4==i5)
                if(checkArea(ind4,ind5))
                    return true;

            ind5=list[ind5].next;
            i5=list[ind5].index;
        }

        return false;
    }

    private boolean noHashEdgeIntersectionExists(BBox bb,int i1,int i2,int ind5,int i5)
    {
        int ind,ind2;
        int i,i3,i4;
        BBox bb1;

        identCntr=0;

        for(i=loopMin;i<loopMax;++i)
        {
            ind=loops[i];
            ind2=ind;
            i3=list[ind2].index;

            do
            {
                ind2=list[ind2].next;
                i4=list[ind2].index;
                bb1=new BBox(this,i3,i4);
                if(bb.BBoxOverlap(bb1))
                    if(segIntersect(bb.imin,bb.imax,bb1.imin,bb1.imax,i5))
                        return true;

                i3=i4;
            } while(ind2!=ind);
        }

        if(identCntr>=4)
            return checkBottleNeck(i5,i1,i2,ind5);

        return false;
    }

    private void storeHeapData(int index,double ratio,int ind,int prev,int next)
    {
        heap[index]=new HeapNode();
        heap[index].ratio=ratio;
        heap[index].index=ind;
        heap[index].prev=prev;
        heap[index].next=next;
    }

    private void dumpOnHeap(double ratio,int ind,int prev,int next)
    {
        int index;
        if(numHeap>=maxNumHeap)
        {
            HeapNode old[]=heap;
            maxNumHeap=maxNumHeap+numPoints;
            heap=new HeapNode[maxNumHeap];
            System.arraycopy(old,0,heap,0,old.length);
        }

        if(ratio==0.0)
        {
            if(numZero<numHeap)
                if(heap[numHeap]==null)
                    storeHeapData(numHeap,heap[numZero].ratio,heap[numZero].index,heap[numZero].prev,heap[numZero].next);
                else
                    heap[numHeap].set(heap[numZero]);

            index=numZero;
            numZero++;
        }
        else
            index=numHeap;

        storeHeapData(index,ratio,ind,prev,next);
        numHeap++;
    }

    private void findLeftMostVertex(int ind,int[] leftInd,int[] leftI)
    {
        int ind1,i1;

        ind1=ind;
        i1=list[ind1].index;
        leftInd[0]=ind1;
        leftI[0]=i1;
        ind1=list[ind1].next;
        i1=list[ind1].index;
        while(ind1!=ind)
        {
            if(i1<leftI[0])
            {
                leftInd[0]=ind1;
                leftI[0]=i1;
            }
            else if(i1==leftI[0])
            {
                if(list[ind1].convex<0)
                {
                    leftInd[0]=ind1;
                    leftI[0]=i1;
                }
            }
            ind1=list[ind1].next;
            i1=list[ind1].index;
        }

    }

    private void simpleBridge(int ind1,int ind2)
    {
        int prev,next;
        int i1,i2,prv,nxt;
        int angle;

        rotateLinks(ind1,ind2);

        i1=list[ind1].index;
        next=list[ind1].next;
        nxt=list[next].index;
        prev=list[ind1].prev;
        prv=list[prev].index;
        angle=isConvexAngle(prv,i1,nxt,ind1);
        list[ind1].convex=angle;

        i2=list[ind2].index;
        next=list[ind2].next;
        nxt=list[next].index;
        prev=list[ind2].prev;
        prv=list[prev].index;
        angle=isConvexAngle(prv,i2,nxt,ind2);
        list[ind2].convex=angle;
    }

    private void insertBridge(int ind1,int i1,int ind3,int i3)
    {
        int ind2,ind4,prev,next;
        int prv,nxt,angle;

        ind2=makeNode(i1);
        insertAfter(ind1,ind2);

        list[ind2].vcntIndex=list[ind1].vcntIndex;

        ind4=makeNode(i3);
        insertAfter(ind3,ind4);

        list[ind4].vcntIndex=list[ind3].vcntIndex;

        splitSplice(ind1,ind2,ind3,ind4);

        next=list[ind1].next;
        nxt=list[next].index;
        prev=list[ind1].prev;
        prv=list[prev].index;
        angle=isConvexAngle(prv,i1,nxt,ind1);
        list[ind1].convex=angle;

        next=list[ind2].next;
        nxt=list[next].index;
        prev=list[ind2].prev;
        prv=list[prev].index;
        angle=isConvexAngle(prv,i1,nxt,ind2);
        list[ind2].convex=angle;

        next=list[ind3].next;
        nxt=list[next].index;
        prev=list[ind3].prev;
        prv=list[prev].index;
        angle=isConvexAngle(prv,i3,nxt,ind3);
        list[ind3].convex=angle;

        next=list[ind4].next;
        nxt=list[next].index;
        prev=list[ind4].prev;
        prv=list[prev].index;
        angle=isConvexAngle(prv,i3,nxt,ind4);
        list[ind4].convex=angle;
    }

    private int l_comp(Left a,Left b)
    {
        if(a.index<b.index)
            return -1;
        else if(a.index>b.index)
            return 1;
        else
            return 0;
    }

    private int d_comp(Distance a,Distance b)
    {
        if(a.dist<b.dist)
            return -1;
        else if(a.dist>b.dist)
            return 1;
        else
            return 0;
    }

    private boolean handleDegeneracies(int i1,int ind1,int i2,int i3,int i4,int ind4)
    {
        int i0,i5;
        int type[]=new int[1];
        int ind0,ind2,ind5;
        double area=0.0,area1=0,area2=0.0;

        ind5=list[ind4].prev;
        i5=list[ind5].index;

        if((i5!=i2)&&(i5!=i3))
        {
            if(vtxInTriangle(i1,i2,i3,i5,type)&&(type[0]==0))
                return true;

            if(i2<=i3)
            {
                if(i4<=i5)
                {
                    if(segIntersect(i2,i3,i4,i5,-1))
                        return true;
                }
                else
                {
                    if(segIntersect(i2,i3,i5,i4,-1))
                        return true;
                }
            }
            else
            {
                if(i4<=i5)
                {
                    if(segIntersect(i3,i2,i4,i5,-1))
                        return true;
                }
                else
                {
                    if(segIntersect(i3,i2,i5,i4,-1))
                        return true;
                }
            }
        }

        ind5=list[ind4].next;
        i5=list[ind5].index;
        if((i5!=i2)&&(i5!=i3))
        {
            if(vtxInTriangle(i1,i2,i3,i5,type)&&(type[0]==0))
                return true;
            if(i2<=i3)
            {
                if(i4<=i5)
                {
                    if(segIntersect(i2,i3,i4,i5,-1))
                        return true;
                }
                else
                {
                    if(segIntersect(i2,i3,i5,i4,-1))
                        return true;
                }
            }
            else
            {
                if(i4<=i5)
                {
                    if(segIntersect(i3,i2,i4,i5,-1))
                        return true;
                }
                else
                {
                    if(segIntersect(i3,i2,i5,i4,-1))
                        return true;
                }
            }
        }

        i0=i1;
        ind0=ind1;
        ind1=list[ind1].next;
        i1=list[ind1].index;
        while(ind1!=ind4)
        {
            ind2=list[ind1].next;
            i2=list[ind2].index;
            area=stableDet2D(i0,i1,i2);
            area1+=area;
            ind1=ind2;
            i1=i2;
        }

        ind1=list[ind0].prev;
        i1=list[ind1].index;
        while(ind1!=ind4)
        {
            ind2=list[ind1].prev;
            i2=list[ind2].index;
            area=stableDet2D(i0,i1,i2);
            area2+=area;
            ind1=ind2;
            i1=i2;
        }

        if((area1<=Triangulator.ZERO)&&(area2<=Triangulator.ZERO))
            return false;
        else if((!((area1)<=-Triangulator.ZERO))&&(!((area2)<=-Triangulator.ZERO)))
            return false;

        return true;
    }

    private boolean desperate(int ind,int i,boolean[] splitted)
    {
        int[] i1=new int[1];
        int[] i2=new int[1];
        int[] i3=new int[1];
        int[] i4=new int[1];
        int[] ind1=new int[1];
        int[] ind2=new int[1];
        int[] ind3=new int[1];
        int[] ind4=new int[1];

        splitted[0]=false;

        if(existsCrossOver(ind,ind1,i1,ind2,i2,ind3,i3,ind4,i4))
        {
            handleCrossOver(ind1[0],i1[0],ind2[0],i2[0],ind3[0],i3[0],ind4[0],i4[0]);
            return false;
        }

        prepareNoHashEdges(i,i+1);

        if(existsSplit(ind,ind1,i1,ind2,i2))
        {
            handleSplit(ind1[0],i1[0],ind2[0],i2[0]);
            splitted[0]=true;
            return false;
        }

        return true;
    }

    private int cleanPolyhedralFace(int i1,int i2)
    {
        int removed;
        int i,j,numSorted,index;
        int ind1,ind2;

        pUnsorted.clear();
        for(i=0;i<numPoints;++i)
            pUnsorted.add(points[i]);

        Arrays.sort(points,0,numPoints,new Comparator<Vector2d>()
        {
            public int compare(Vector2d _a,Vector2d _b)
            {
                return pComp(_a,_b);
            }
        });

        i=0;
        for(j=1;j<numPoints;++j)
            if(pComp(points[i],points[j])!=0)
            {
                i++;
                points[i]=points[j];
            }

        numSorted=i+1;
        removed=numPoints-numSorted;

        for(i=i1;i<i2;++i)
        {
            ind1=loops[i];
            ind2=list[ind1].next;
            index=list[ind2].index;
            while(ind2!=ind1)
            {
                j=findPInd(points,numSorted,pUnsorted.get(index));
                list[ind2].index=j;
                ind2=list[ind2].next;
                index=list[ind2].index;
            }
            j=findPInd(points,numSorted,pUnsorted.get(index));
            list[ind2].index=j;
        }

        numPoints=numSorted;

        return removed;
    }

    private int findPInd(Vector2d sorted[],int numPts,Vector2d pnt)
    {
        for(int i=0;i<numPts;i++)
            if((pnt.x==sorted[i].x)&&(pnt.y==sorted[i].y))
                return i;
        return -1;
    }

    private int pComp(Vector2d a,Vector2d b)
    {
        if(a.x<b.x)
            return -1;
        else if(a.x>b.x)
            return 1;
        else
        {
            if(a.y<b.y)
                return -1;
            else if(a.y>b.y)
                return 1;
            else
                return 0;
        }
    }

    private double angle(Vector2d p,Vector2d p1,Vector2d p2)
    {
        int sign=signEps(det2D(p2,p,p1),epsilon);
        if(sign==0)
            return 0.0;

        Vector2d v1=p1.subtract(p);
        Vector2d v2=p2.subtract(p);

        double angle1=Math.atan2(v1.y,v1.x);
        double angle2=Math.atan2(v2.y,v2.x);

        if(angle1<0.0)
            angle1+=2.0*Math.PI;
        if(angle2<0.0)
            angle2+=2.0*Math.PI;

        double angle=angle1-angle2;
        if(angle>Math.PI)
            angle=2.0*Math.PI-angle;
        else if(angle<-Math.PI)
            angle=2.0*Math.PI+angle;

        if(sign==1)
            return Math.abs(angle);

        return -Math.abs(angle);
    }

    private boolean existsCrossOver(int ind,int[] ind1,int[] i1,int[] ind2,int[] i2,int[] ind3,int[] i3,int[] ind4,int[] i4)
    {
        BBox bb1,bb2;

        ind1[0]=ind;
        i1[0]=list[ind1[0]].index;
        ind2[0]=list[ind1[0]].next;
        i2[0]=list[ind2[0]].index;
        ind3[0]=list[ind2[0]].next;
        i3[0]=list[ind3[0]].index;
        ind4[0]=list[ind3[0]].next;
        i4[0]=list[ind4[0]].index;

        do
        {
            bb1=new BBox(this,i1[0],i2[0]);
            bb2=new BBox(this,i3[0],i4[0]);
            if(bb1.BBoxOverlap(bb2))
            {
                if(segIntersect(bb1.imin,bb1.imax,bb2.imin,bb2.imax,-1))
                    return true;
            }
            ind1[0]=ind2[0];
            i1[0]=i2[0];
            ind2[0]=ind3[0];
            i2[0]=i3[0];
            ind3[0]=ind4[0];
            i3[0]=i4[0];
            ind4[0]=list[ind3[0]].next;
            i4[0]=list[ind4[0]].index;

        } while(ind1[0]!=ind);

        return false;
    }

    private void handleCrossOver(int ind1,int i1,int ind2,int i2,int ind3,int i3,int ind4,int i4)
    {
        double ratio1,ratio4;
        boolean first;
        int angle1,angle4;

        angle1=list[ind1].convex;
        angle4=list[ind4].convex;
        if(angle1<angle4)
            first=true;
        else if(angle1>angle4)
            first=false;
        else if(earsSorted)
        {
            ratio1=getRatio(i3,i4,i1);
            ratio4=getRatio(i1,i2,i4);
            if(ratio4<ratio1)
                first=false;
            else
                first=true;
        }
        else
            first=true;

        if(first)
        {
            deleteLinks(ind2);
            storeTriangle(ind1,ind2,ind3);
            list[ind3].convex=1;
            dumpOnHeap(0.0,ind3,ind1,ind4);
        }
        else
        {
            deleteLinks(ind3);
            storeTriangle(ind2,ind3,ind4);
            list[ind2].convex=1;
            dumpOnHeap(0.0,ind2,ind1,ind4);
        }
    }

    private boolean letsHope(int ind)
    {
        int ind0,ind1,ind2;
        ind1=ind;

        do
        {
            if(list[ind1].convex>0)
            {
                ind0=list[ind1].prev;
                ind2=list[ind1].next;
                dumpOnHeap(0.0,ind1,ind0,ind2);
                return true;
            }
            ind1=list[ind1].next;
        } while(ind1!=ind);

        list[ind].convex=1;
        ind0=list[ind].prev;
        ind2=list[ind].next;
        dumpOnHeap(0.0,ind,ind0,ind2);
        return true;
    }

    private boolean existsSplit(int ind,int[] ind1,int[] i1,int[] ind2,int[] i2)
    {
        int ind3,ind4,ind5;
        int i3,i4,i5;

        if(numPoints>maxNumDist)
        {
            maxNumDist=numPoints;
            distances=new Distance[maxNumDist];
            for(int k=0;k<maxNumDist;k++)
                distances[k]=new Distance();
        }
        ind1[0]=ind;
        i1[0]=list[ind1[0]].index;
        ind4=list[ind1[0]].next;
        i4=list[ind4].index;
        ind5=list[ind4].next;
        i5=list[ind5].index;
        ind3=list[ind1[0]].prev;
        i3=list[ind3].index;
        if(foundSplit(ind5,i5,ind3,ind1[0],i1[0],i3,i4,ind2,i2))
            return true;
        i3=i1[0];
        ind1[0]=ind4;
        i1[0]=i4;
        ind4=ind5;
        i4=i5;
        ind5=list[ind4].next;
        i5=list[ind5].index;

        while(ind5!=ind)
        {
            if(foundSplit(ind5,i5,ind,ind1[0],i1[0],i3,i4,ind2,i2))
                return true;
            i3=i1[0];
            ind1[0]=ind4;
            i1[0]=i4;
            ind4=ind5;
            i4=i5;
            ind5=list[ind4].next;
            i5=list[ind5].index;
        }

        return false;
    }

    private int windingNumber(int ind,Vector2d p)
    {
        double angle;
        int ind2;
        int i1,i2,number;

        i1=list[ind].index;
        ind2=list[ind].next;
        i2=list[ind2].index;
        angle=angle(p,points[i1],points[i2]);
        while(ind2!=ind)
        {
            i1=i2;
            ind2=list[ind2].next;
            i2=list[ind2].index;
            angle+=angle(p,points[i1],points[i2]);
        }

        angle+=Math.PI;
        number=(int)(angle/(Math.PI*2.0));

        return number;
    }

    private boolean foundSplit(int ind5,int i5,int ind,int ind1,int i1,int i3,int i4,int[] ind2,int[] i2)
    {
        Vector2d center;
        int numDist=0;
        int j,i6,i7;
        int ind6,ind7;
        BBox bb;
        boolean convex,coneOk;

        do
        {
            distances[numDist].dist=baseLength(points[i1],points[i5]);
            distances[numDist].ind=ind5;
            ++numDist;
            ind5=list[ind5].next;
            i5=list[ind5].index;
        } while(ind5!=ind);

        Arrays.sort(distances,0,numDist,new Comparator<Distance>()
        {
            public int compare(Distance _a,Distance _b)
            {
                return d_comp(_a,_b);
            }
        });

        for(j=0;j<numDist;++j)
        {
            ind2[0]=distances[j].ind;
            i2[0]=list[ind2[0]].index;
            if(i1!=i2[0])
            {
                ind6=list[ind2[0]].prev;
                i6=list[ind6].index;
                ind7=list[ind2[0]].next;
                i7=list[ind7].index;

                convex=list[ind2[0]].convex>0;
                coneOk=isInCone(i6,i2[0],i7,i1,convex);
                if(coneOk)
                {
                    convex=list[ind1].convex>0;
                    coneOk=isInCone(i3,i1,i4,i2[0],convex);
                    if(coneOk)
                    {
                        bb=new BBox(this,i1,i2[0]);
                        if(!noHashEdgeIntersectionExists(bb,-1,-1,ind1,-1))
                        {
                            center=points[i1].add(points[i2[0]]).scale(0.5);
                            if(windingNumber(ind,center)==1)
                                return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private double baseLength(Vector2d u,Vector2d v)
    {
        double x,y;
        x=v.x-u.x;
        y=v.y-u.y;
        return Math.abs(x)+Math.abs(y);
    }

    private double det2D(Vector2d _u,Vector2d _v,Vector2d _w)
    {
        return((_u.x-_v.x)*(_v.y-_w.y)+(_v.y-_u.y)*(_v.x-_w.x));
    }

    private boolean inBetween(int i1,int i2,int i3)
    {
        return((i1<=i3)&&(i3<=i2));
    }

    private boolean strictlyInBetween(int i1,int i2,int i3)
    {
        return((i1<i3)&&(i3<i2));
    }

    private double stableDet2D(int i,int j,int k)
    {
        if((i==j)||(i==k)||(j==k))
            return 0.0;
        else
        {
            Vector2d numericsHP=points[i];
            Vector2d numericsHQ=points[j];
            Vector2d numericsHR=points[k];

            if(i<j)
            {
                if(j<k)
                    return det2D(numericsHP,numericsHQ,numericsHR);
                else if(i<k)
                    return -det2D(numericsHP,numericsHR,numericsHQ);
                else
                    return det2D(numericsHR,numericsHP,numericsHQ);
            }
            else
            {
                if(i<k)
                    return -det2D(numericsHQ,numericsHP,numericsHR);
                else if(j<k)
                    return det2D(numericsHQ,numericsHR,numericsHP);
                else
                    return -det2D(numericsHR,numericsHQ,numericsHP);
            }
        }
    }

    private int orientation(int i,int j,int k)
    {
        double numericsHDet=stableDet2D(i,j,k);
        if((numericsHDet<-epsilon))
            return -1;
        else if(!((numericsHDet)<=epsilon))
            return 1;
        else
            return 0;
    }

    private boolean isInCone(int i,int j,int k,int l,boolean convex)
    {
        if(convex)
        {
            if(i!=j)
            {
                int numericsHOri=orientation(i,j,l);
                if(numericsHOri<0)
                    return false;
                else if(numericsHOri==0)
                {
                    if(i<j)
                    {
                        if(!inBetween(i,j,l))
                            return false;
                    }
                    else
                    {
                        if(!inBetween(j,i,l))
                            return false;
                    }
                }
            }
            if(j!=k)
            {
                int numericsHOri=orientation(j,k,l);
                if(numericsHOri<0)
                    return false;
                else if(numericsHOri==0)
                {
                    if(j<k)
                    {
                        if(!inBetween(j,k,l))
                            return false;
                    }
                    else
                    {
                        if(!inBetween(k,j,l))
                            return false;
                    }
                }
            }
        }
        else
        {
            if(orientation(i,j,l)<=0)
                if(orientation(j,k,l)<0)
                    return false;
        }
        return true;
    }

    private int isConvexAngle(int i,int j,int k,int ind)
    {
        double numericsHDot;
        int numericsHOri1;
        Vector2d numericsHP,numericsHQ;

        if(i==j)
            return 1;
        else if(j==k)
            return -1;

        numericsHOri1=orientation(i,j,k);
        if(numericsHOri1>0)
            return 1;
        else if(numericsHOri1<0)
            return -1;

        numericsHP=points[i].subtract(points[j]);
        numericsHQ=points[k].subtract(points[j]);
        numericsHDot=numericsHP.dot(numericsHQ);
        if(numericsHDot<0.0)
            return 0;
        else
            return recSpikeAngle(i,j,k,list[ind].prev,list[ind].next);
    }

    private boolean pntInTriangle(int i1,int i2,int i3,int i4)
    {
        if(orientation(i2,i3,i4)>=0)
            if(orientation(i1,i2,i4)>=0)
                if(orientation(i3,i1,i4)>=0)
                    return true;

        return false;
    }

    private boolean vtxInTriangle(int i1,int i2,int i3,int i4,int[] type)
    {
        if(orientation(i2,i3,i4)>=0)
        {
            int numericsHOri1=orientation(i1,i2,i4);
            if(numericsHOri1>0)
            {
                numericsHOri1=orientation(i3,i1,i4);
                if(numericsHOri1>0)
                {
                    type[0]=0;
                    return true;
                }
                else if(numericsHOri1==0)
                {
                    type[0]=1;
                    return true;
                }
            }
            else if(numericsHOri1==0)
            {
                numericsHOri1=orientation(i3,i1,i4);
                if(numericsHOri1>0)
                {
                    type[0]=2;
                    return true;
                }
                else if(numericsHOri1==0)
                {
                    type[0]=3;
                    return true;
                }
            }
        }
        return false;
    }

    private boolean segIntersect(int i1,int i2,int i3,int i4,int i5)
    {
        int ori1,ori2,ori3,ori4;

        if((i1==i2)||(i3==i4))
            return false;
        if((i1==i3)&&(i2==i4))
            return true;

        if((i3==i5)||(i4==i5))
            identCntr++;

        ori3=orientation(i1,i2,i3);
        ori4=orientation(i1,i2,i4);
        if(((ori3==1)&&(ori4==1))||((ori3==-1)&&(ori4==-1)))
            return false;

        if(ori3==0)
        {
            if(strictlyInBetween(i1,i2,i3))
                return true;
            if(ori4==0)
            {
                if(strictlyInBetween(i1,i2,i4))
                    return true;
            }
            else
                return false;
        }
        else if(ori4==0)
            return strictlyInBetween(i1,i2,i4);

        ori1=orientation(i3,i4,i1);
        ori2=orientation(i3,i4,i2);
        return !(((ori1<=0)&&(ori2<=0))||((ori1>=0)&&(ori2>=0)));
    }

    private double getRatio(int i,int j,int k)
    {
        double area,a,b,c,base,ratio;
        Vector2d p,q,r;

        p=points[i];
        q=points[j];
        r=points[k];

        a=baseLength(p,q);
        b=baseLength(p,r);
        c=baseLength(r,q);

        base=a;
        if(b>base)
            base=b;
        if(c>base)
            base=c;

        if((10.0*a)<Math.min(b,c))
            return 0.1;

        area=stableDet2D(i,j,k);
        if((area<-epsilon))
            area=-area;
        else if(!(!((area)<=epsilon)))
        {
            if(base>a)
                return 0.1;
            else
                return Double.MAX_VALUE;
        }

        ratio=base*base/area;

        if(ratio<10.0)
            return ratio;

        if(a<base)
            return 0.1;

        return ratio;
    }

    private int recSpikeAngle(int i1,int i2,int i3,int ind1,int ind3)
    {
        int ori,ori1,ori2,i0,ii1,ii2;
        Vector2d pq,pr;
        double dot;

        if(ind1==ind3)
            return -2;

        if(i1!=i3)
        {
            if(i1<i2)
            {
                ii1=i1;
                ii2=i2;
            }
            else
            {
                ii1=i2;
                ii2=i1;
            }
            if(inBetween(ii1,ii2,i3))
            {
                i2=i3;
                ind3=list[ind3].next;
                i3=list[ind3].index;

                if(ind1==ind3)
                    return 2;
                ori=orientation(i1,i2,i3);
                if(ori>0)
                    return 2;
                else if(ori<0)
                    return -2;
                else
                    return recSpikeAngle(i1,i2,i3,ind1,ind3);
            }
            else
            {
                i2=i1;
                ind1=list[ind1].prev;
                i1=list[ind1].index;
                if(ind1==ind3)
                    return 2;
                ori=orientation(i1,i2,i3);
                if(ori>0)
                    return 2;
                else if(ori<0)
                    return -2;
                else
                    return recSpikeAngle(i1,i2,i3,ind1,ind3);
            }
        }
        else
        {
            i0=i2;
            i2=i1;
            ind1=list[ind1].prev;
            i1=list[ind1].index;

            if(ind1==ind3)
                return 2;
            ind3=list[ind3].next;
            i3=list[ind3].index;
            if(ind1==ind3)
                return 2;
            ori=orientation(i1,i2,i3);
            if(ori>0)
            {
                ori1=orientation(i1,i2,i0);
                if(ori1>0)
                {
                    ori2=orientation(i2,i3,i0);
                    if(ori2>0)
                        return -2;
                }
                return 2;
            }
            else if(ori<0)
            {
                ori1=orientation(i2,i1,i0);
                if(ori1>0)
                {
                    ori2=orientation(i3,i2,i0);
                    if(ori2>0)
                        return 2;
                }
                return -2;
            }
            else
            {
                pq=points[i1].subtract(points[i2]);
                pr=points[i3].subtract(points[i2]);
                dot=pq.dot(pr);
                if(dot<0.0)
                {
                    ori=orientation(i2,i1,i0);
                    if(ori>0)
                        return 2;
                    else
                        return -2;
                }
                else
                    return recSpikeAngle(i1,i2,i3,ind1,ind3);
            }
        }
    }

    private void handleSplit(int ind1,int i1,int ind3,int i3)
    {
        int ind2=makeNode(i1);
        insertAfter(ind1,ind2);

        list[ind2].vcntIndex=list[ind1].vcntIndex;

        int ind4=makeNode(i3);
        insertAfter(ind3,ind4);

        list[ind4].vcntIndex=list[ind3].vcntIndex;

        splitSplice(ind1,ind2,ind3,ind4);

        storeChain(ind1);
        storeChain(ind3);

        int next=list[ind1].next;
        int nxt=list[next].index;
        int prev=list[ind1].prev;
        int prv=list[prev].index;
        int angle=isConvexAngle(prv,i1,nxt,ind1);
        list[ind1].convex=angle;

        next=list[ind2].next;
        nxt=list[next].index;
        prev=list[ind2].prev;
        prv=list[prev].index;
        angle=isConvexAngle(prv,i1,nxt,ind2);
        list[ind2].convex=angle;

        next=list[ind3].next;
        nxt=list[next].index;
        prev=list[ind3].prev;
        prv=list[prev].index;
        angle=isConvexAngle(prv,i3,nxt,ind3);
        list[ind3].convex=angle;

        next=list[ind4].next;
        nxt=list[next].index;
        prev=list[ind4].prev;
        prv=list[prev].index;
        angle=isConvexAngle(prv,i3,nxt,ind4);
        list[ind4].convex=angle;
    }

    private void classifyAngles(int ind)
    {
        int ind0,ind1,ind2;
        int i0,i1,i2;
        int angle;

        ind1=ind;
        i1=list[ind1].index;
        ind0=list[ind1].prev;
        i0=list[ind0].index;

        do
        {
            ind2=list[ind1].next;
            i2=list[ind2].index;
            angle=isConvexAngle(i0,i1,i2,ind1);
            list[ind1].convex=angle;
            i0=i1;
            i1=i2;
            ind1=ind2;
        } while(ind1!=ind);
    }

    private boolean isEar(int ind2,int[] ind1,int[] ind3,double[] ratio)
    {
        BBox bb;
        boolean convex,coneOk;

        int i2=list[ind2].index;
        ind3[0]=list[ind2].next;
        int i3=list[ind3[0]].index;
        int ind4=list[ind3[0]].next;
        int i4=list[ind4].index;
        ind1[0]=list[ind2].prev;
        int i1=list[ind1[0]].index;
        int ind0=list[ind1[0]].prev;
        int i0=list[ind0].index;

        if((i1==i3)||(i1==i2)||(i2==i3)||(list[ind2].convex==2))
        {
            ratio[0]=0.0;
            return true;
        }

        if(i0==i3)
        {
            if((list[ind0].convex<0)||(list[ind3[0]].convex<0))
            {
                ratio[0]=0.0;
                return true;
            }
            else
                return false;
        }

        if(i1==i4)
        {
            if((list[ind1[0]].convex<0)||(list[ind4].convex<0))
            {
                ratio[0]=0.0;
                return true;
            }
            else
                return false;
        }

        convex=list[ind1[0]].convex>0;
        coneOk=isInCone(i0,i1,i2,i3,convex);

        if(!coneOk)
            return false;
        convex=list[ind3[0]].convex>0;
        coneOk=isInCone(i2,i3,i4,i1,convex);

        if(coneOk)
        {
            bb=new BBox(this,i1,i3);
            if(!noHashIntersectionExists(i2,ind2,i3,i1,bb))
            {
                if(earsSorted)
                    ratio[0]=getRatio(i1,i3,i2);
                else
                    ratio[0]=1.0;
                return true;
            }
        }

        return false;
    }

    private boolean noHashIntersectionExists(int i1,int ind1,int i2,int i3,BBox bb)
    {
        int indVtx,ind5;
        int indPnt;
        int i4;
        int type[]=new int[1];
        double y;

        if(numReflex<=0)
            return false;

        if(i1<bb.imin)
            bb.imin=i1;
        else if(i1>bb.imax)
            bb.imax=i1;
        y=points[i1].y;
        if(y<bb.ymin)
            bb.ymin=y;
        else if(y>bb.ymax)
            bb.ymax=y;

        indPnt=reflexVertices;
        do
        {
            indVtx=vtxList[indPnt].pnt;
            i4=list[indVtx].index;

            if(bb.pntInBBox(this,i4))
            {
                ind5=list[indVtx].next;
                if((indVtx!=ind1)&&(indVtx!=ind5))
                {
                    if(i4==i1)
                    {
                        if(handleDegeneracies(i1,ind1,i2,i3,i4,indVtx))
                            return true;
                    }
                    else if((i4!=i2)&&(i4!=i3))
                    {
                        if(vtxInTriangle(i1,i2,i3,i4,type))
                            return true;
                    }
                }
            }
            indPnt=vtxList[indPnt].next;

        } while(indPnt!=-1);

        return false;
    }

    private void deleteFromList(int i)
    {
        if(numReflex==0)
            return;

        int indPnt=reflexVertices;
        int indVtx=vtxList[indPnt].pnt;

        if(indVtx==i)
        {
            reflexVertices=vtxList[indPnt].next;
            numReflex--;
        }
        else
        {
            int indPnt1=vtxList[indPnt].next;
            while(indPnt1!=-1)
            {
                indVtx=vtxList[indPnt1].pnt;
                if(indVtx==i)
                {
                    vtxList[indPnt].next=vtxList[indPnt1].next;
                    indPnt1=-1;
                    numReflex--;
                }
                else
                {
                    indPnt=indPnt1;
                    indPnt1=vtxList[indPnt].next;
                }
            }
        }
    }

    private boolean clipEar(boolean[] done)
    {
        int ind0,ind1,ind3,ind4;
        int i0,i1,i3,i4;
        int angle1,angle3;

        double ratio[]=new double[1];
        int index0[]=new int[1];
        int index1[]=new int[1];
        int index2[]=new int[1];
        int index3[]=new int[1];
        int index4[]=new int[1];
        int ind2[]=new int[1];

        do
        {
            if(!deleteFromHeap(ind2,index1,index3))
                return false;

            ind1=list[ind2[0]].prev;
            i1=list[ind1].index;
            ind3=list[ind2[0]].next;
            i3=list[ind3].index;
        } while((index1[0]!=ind1)||(index3[0]!=ind3));

        deleteLinks(ind2[0]);
        storeTriangle(ind1,ind2[0],ind3);

        ind0=list[ind1].prev;
        i0=list[ind0].index;
        if(ind0==ind3)
        {
            done[0]=true;
            return true;
        }
        angle1=isConvexAngle(i0,i1,i3,ind1);

        ind4=list[ind3].next;
        i4=list[ind4].index;

        angle3=isConvexAngle(i1,i3,i4,ind3);

        if(i1!=i3)
        {
            if((angle1>=0)&&(list[ind1].convex<0))
                deleteFromList(ind1);
            if((angle3>=0)&&(list[ind3].convex<0))
                deleteFromList(ind3);
        }
        else
        {
            if((angle1>=0)&&(list[ind1].convex<0))
                deleteFromList(ind1);
            else if((angle3>=0)&&(list[ind3].convex<0))
                deleteFromList(ind3);
        }

        list[ind1].convex=angle1;
        list[ind3].convex=angle3;

        if(angle1>0)
            if(isEar(ind1,index0,index2,ratio))
                dumpOnHeap(ratio[0],ind1,index0[0],index2[0]);

        if(angle3>0)
            if(isEar(ind3,index2,index4,ratio))
                dumpOnHeap(ratio[0],ind3,index2[0],index4[0]);

        ind0=list[ind1].prev;
        ind4=list[ind3].next;
        if(ind0==ind4)
        {
            storeTriangle(ind1,ind3,ind4);
            done[0]=true;
        }
        else
            done[0]=false;

        return true;
    }

    private boolean deleteFromHeap(int[] ind,int[] prev,int[] next)
    {
        double rnd;
        int rndInd;

        if(numZero>0)
        {
            numZero--;
            numHeap--;

            ind[0]=heap[numZero].index;
            prev[0]=heap[numZero].prev;
            next[0]=heap[numZero].next;
            if(numZero<numHeap)
                heap[numZero].set(heap[numHeap]);

            return true;
        }
        else if(earsRandom)
        {
            if(numHeap<=0)
            {
                numHeap=0;
                return false;
            }
            rnd=Math.random();
            rndInd=(int)(rnd*numHeap);
            numHeap--;
            if(rndInd>numHeap)
                rndInd=numHeap;

            ind[0]=heap[rndInd].index;
            prev[0]=heap[rndInd].prev;
            next[0]=heap[rndInd].next;
            if(rndInd<numHeap)
                heap[rndInd].set(heap[numHeap]);

            return true;
        }
        else
        {
            if(numHeap<=0)
            {
                numHeap=0;
                return false;
            }

            numHeap--;
            ind[0]=heap[numHeap].index;
            prev[0]=heap[numHeap].prev;
            next[0]=heap[numHeap].next;

            return true;
        }
    }

    private void classifyEars(int ind)
    {
        int ind1;
        int[] ind0,ind2;
        double[] ratio;

        ind0=new int[1];
        ind2=new int[1];
        ratio=new double[1];

        maxNumHeap=numPoints;
        heap=new HeapNode[maxNumHeap];
        numHeap=0;
        numZero=0;

        ind1=ind;
        do
        {
            if((list[ind1].convex>0)&&isEar(ind1,ind0,ind2,ratio))
                dumpOnHeap(ratio[0],ind1,ind0[0],ind2[0]);

            ind1=list[ind1].next;
        } while(ind1!=ind);
    }

    private boolean simpleFace(int ind1)
    {
        int ind0,ind2,ind3,ind4;
        int i1,i2,i3,i4;

        Vector3d pq,pr,nr;

        double x,y,z;
        int ori2,ori4;

        ind0=list[ind1].prev;
        if(ind0==ind1)
            return true;

        ind2=list[ind1].next;
        i2=list[ind2].index;
        if(ind0==ind2)
            return true;

        ind3=list[ind2].next;
        i3=list[ind3].index;
        if(ind0==ind3)
        {
            storeTriangle(ind1,ind2,ind3);
            return true;
        }

        ind4=list[ind3].next;
        i4=list[ind4].index;
        if(ind0==ind4)
        {
            initPnts(5);
            i1=list[ind1].index;

            pq=vertices[i1].subtract(vertices[i2]);
            pr=vertices[i3].subtract(vertices[i2]);
            nr=pq.cross(pr);

            x=Math.abs(nr.x);
            y=Math.abs(nr.y);
            z=Math.abs(nr.z);
            if((z>=x)&&(z>=y))
            {
                points[1]=new Vector2d(vertices[i1].x,vertices[i1].y);
                points[2]=new Vector2d(vertices[i2].x,vertices[i2].y);
                points[3]=new Vector2d(vertices[i3].x,vertices[i3].y);
                points[4]=new Vector2d(vertices[i4].x,vertices[i4].y);
            }
            else if((x>=y)&&(x>=z))
            {
                points[1]=new Vector2d(vertices[i1].z,vertices[i1].y);
                points[2]=new Vector2d(vertices[i2].z,vertices[i2].y);
                points[3]=new Vector2d(vertices[i3].z,vertices[i3].y);
                points[4]=new Vector2d(vertices[i4].z,vertices[i4].y);
            }
            else
            {
                points[1]=new Vector2d(vertices[i1].x,vertices[i1].z);
                points[2]=new Vector2d(vertices[i2].x,vertices[i2].z);
                points[3]=new Vector2d(vertices[i3].x,vertices[i3].z);
                points[4]=new Vector2d(vertices[i4].x,vertices[i4].z);
            }
            numPoints=5;

            ori2=orientation(1,2,3);
            ori4=orientation(1,3,4);

            if(((ori2>0)&&(ori4>0))||((ori2<0)&&(ori4<0)))
            {
                storeTriangle(ind1,ind2,ind3);
                storeTriangle(ind1,ind3,ind4);
            }
            else
            {
                storeTriangle(ind2,ind3,ind4);
                storeTriangle(ind2,ind4,ind1);
            }
            return true;
        }

        return false;
    }

    private void preProcessList(int i1)
    {
        int tInd,tInd1,tInd2;

        firstNode=loops[i1];
        tInd=loops[i1];
        tInd1=tInd;
        tInd2=list[tInd1].next;
        while(tInd2!=tInd)
        {
            if(list[tInd1].index==list[tInd2].index)
            {
                if(tInd2==loops[i1])
                    loops[i1]=list[tInd2].next;
                deleteLinks(tInd2);
            }
            tInd1=list[tInd1].next;
            tInd2=list[tInd1].next;
        }
    }

    private boolean inPolyList(int ind)
    {
        return((ind>=0)&&(ind<numList)&&(numList<=maxNumList));
    }

    private void deleteHook(int currLoop)
    {
        int ind1,ind2;

        ind1=loops[currLoop];
        ind2=list[ind1].next;
        if((inPolyList(ind1))&&(inPolyList(ind2)))
        {
            deleteLinks(ind1);
            loops[currLoop]=ind2;
        }
    }

    private void deleteLinks(int ind)
    {
        if(inPolyList(ind)&&inPolyList(list[ind].prev)&&inPolyList(list[ind].next))
        {
            if(firstNode==ind)
                firstNode=list[ind].next;

            list[list[ind].next].prev=list[ind].prev;
            list[list[ind].prev].next=list[ind].next;
            list[ind].prev=list[ind].next=ind;
        }
    }

    private void prepareNoHashPnts(int currLoopMin)
    {
        numVtxList=0;
        reflexVertices=-1;

        int ind=loops[currLoopMin];
        int ind1=ind;
        numReflex=0;
        do
        {
            if(list[ind1].convex<0)
                insertAfterVtx(ind1);

            ind1=list[ind1].next;
        } while(ind1!=ind);
    }

    private void rotateLinks(int ind1,int ind2)
    {
        int ind;
        int ind0,ind3;

        ind0=list[ind1].next;
        ind3=list[ind2].next;

        ind=list[ind1].next;
        list[ind1].next=list[ind2].next;
        list[ind2].next=ind;
        list[ind0].prev=ind2;
        list[ind3].prev=ind1;
    }

    private void storeChain(int ind)
    {
        if(numChains>=maxNumChains)
        {
            maxNumChains+=20;
            int old[]=chains;
            chains=new int[maxNumChains];
            if(old!=null)
                System.arraycopy(old,0,chains,0,old.length);
        }
        chains[numChains]=ind;
        numChains++;
    }

    private int getNextChain(boolean[] done)
    {
        if(numChains>0)
        {
            done[0]=true;
            numChains--;
            return chains[numChains];
        }

        done[0]=false;
        numChains=0;
        return 0;
    }

    private void splitSplice(int ind1,int ind2,int ind3,int ind4)
    {
        list[ind1].next=ind4;
        list[ind4].prev=ind1;
        list[ind2].prev=ind3;
        list[ind3].next=ind2;
    }

    private int makeHook()
    {
        int ind=numList;
        if(numList>=maxNumList)
        {
            maxNumList+=INC_LIST_BK;
            ListNode old[]=list;
            list=new ListNode[maxNumList];
            System.arraycopy(old,0,list,0,old.length);
        }

        list[numList]=new ListNode(-1);
        list[numList].prev=ind;
        list[numList].next=ind;
        list[numList].index=-1;
        numList++;

        return ind;
    }

    private void insertAfterVtx(int iVtx)
    {
        int size;

        if(vtxList==null)
        {
            size=Math.max(numVtxList+1,100);
            vtxList=new PntNode[size];
        }
        else if(numVtxList>=vtxList.length)
        {
            size=Math.max(numVtxList+1,vtxList.length+100);
            PntNode old[]=vtxList;
            vtxList=new PntNode[size];
            System.arraycopy(old,0,vtxList,0,old.length);
        }

        vtxList[numVtxList]=new PntNode(iVtx,reflexVertices);
        reflexVertices=numVtxList;
        numVtxList++;
        numReflex++;
    }

    private int makeLoopHeader()
    {
        int ind;

        ind=makeHook();
        if(numLoops>=maxNumLoops)
        {
            maxNumLoops+=INC_LOOP_BK;
            int old[]=loops;
            loops=new int[maxNumLoops];
            System.arraycopy(old,0,loops,0,old.length);
        }

        loops[numLoops]=ind;
        numLoops++;

        return numLoops-1;
    }

    private int makeNode(int index)
    {
        if(numList>=maxNumList)
        {
            maxNumList+=INC_LIST_BK;
            ListNode old[]=list;
            list=new ListNode[maxNumList];
            System.arraycopy(old,0,list,0,old.length);
        }

        list[numList]=new ListNode(index);
        list[numList].index=index;
        list[numList].prev=-1;
        list[numList].next=-1;
        numList++;

        return numList-1;
    }

    private void insertAfter(int ind1,int ind2)
    {
        if((inPolyList(ind1))&&(inPolyList(ind2)))
        {

            list[ind2].next=list[ind1].next;
            list[ind2].prev=ind1;
            list[ind1].next=ind2;
            int ind3=list[ind2].next;

            if(inPolyList(ind3))
                list[ind3].prev=ind2;

            return;
        }
    }

    private void swapLinks(int ind1)
    {
        int ind2,ind3;

        ind2=list[ind1].next;
        list[ind1].next=list[ind1].prev;
        list[ind1].prev=ind2;
        ind3=ind2;
        while(ind2!=ind1)
        {
            ind3=list[ind2].next;
            list[ind2].next=list[ind2].prev;
            list[ind2].prev=ind3;
            ind2=ind3;
        }
    }

    private void storeTriangle(int i,int j,int k)
    {
        if(ccwLoop)
            triangles.add(new Triangle(i,j,k));
        else
            triangles.add(new Triangle(j,i,k));
    }

    private void initPnts(int number)
    {
        if(maxNumPoints<number)
        {
            maxNumPoints=number;
            points=new Vector2d[maxNumPoints];
        }

        for(int i=0;i<number;i++)
            points[i]=new Vector2d(0.0f,0.0f);

        numPoints=0;
    }

    int storePoint(double x,double y)
    {
        if(numPoints>=maxNumPoints)
        {
            maxNumPoints+=INC_POINT_BK;
            Vector2d old[]=points;
            points=new Vector2d[maxNumPoints];
            if(old!=null)
                System.arraycopy(old,0,points,0,old.length);
        }

        points[numPoints]=new Vector2d(x,y);
        numPoints++;

        return numPoints-1;
    }

    private void projectFace(int loopMin,int loopMax)
    {
        Vector3d normal,nr;
        int i,j;
        double d;

        normal=new Vector3d();
        nr=new Vector3d();

        normal=determineNormal(loops[loopMin]);
        j=loopMin+1;
        if(j<loopMax)
        {
            for(i=j;i<loopMax;++i)
            {
                nr=determineNormal(loops[i]);
                if(normal.dot(nr)<0.0)
                    nr.negate();

                normal=normal.add(nr);
            }
            d=normal.length();
            if(!((d)<=Triangulator.ZERO))
                normal=normal.scale(1/d);
            else
                normal=new Vector3d(0,0,1);
        }

        projectPoints(loopMin,loopMax,normal);
    }

    private Vector3d determineNormal(int ind)
    {
        Vector3d normal;
        
        int ind1=ind;
        int i1=list[ind1].index;
        int ind0=list[ind1].prev;
        int i0=list[ind0].index;
        int ind2=list[ind1].next;
        int i2=list[ind2].index;
        Vector3d pq=vertices[i0].subtract(vertices[i1]);
        Vector3d pr=vertices[i2].subtract(vertices[i1]);
        Vector3d nr=pq.cross(pr);
        double d=nr.length();
        if(!((d)<=Triangulator.ZERO))
            normal=nr.scale(1/d);
        else
            normal=new Vector3d(0,0,0);

        pq=pr;
        ind1=ind2;
        ind2=list[ind1].next;
        i2=list[ind2].index;
        while(ind1!=ind)
        {
            pr=vertices[i2].subtract(vertices[i1]);
            nr=pq.cross(pr);
            d=nr.length();
            if(!((d)<=Triangulator.ZERO))
            {
                nr=nr.scale(1/d);
                if(normal.dot(nr)<0.0)
                    nr=nr.negate();
                normal=normal.add(nr);
            }
            pq=pr;
            ind1=ind2;
            ind2=list[ind1].next;
            i2=list[ind2].index;
        }

        d=normal.length();
        if(!((d)<=Triangulator.ZERO))
            normal=normal.scale(1/d);
        else
            normal=new Vector3d(0,0,1);
        
        return normal;
    }

    private int signEps(double _x,double _eps)
    {
        if(_x>_eps)
            return 1;

        if(_x<-_eps)
            return -1;

        return 0;
    }

    private void projectPoints(int i1,int i2,Vector3d n3)
    {
        Vector3d vtx=new Vector3d();
        Vector3d n1=new Vector3d();
        Vector3d n2=new Vector3d();

        if((Math.abs(n3.x)>0.1)||(Math.abs(n3.y)>0.1))
            n1=new Vector3d(-n3.y,n3.x,0);
        else
            n1=new Vector3d(n3.z,-n3.x,0);

        n1=n1.normalize();
        n2=n1.cross(n3).normalize();

        Matrix4d matrix=new Matrix4d();
        matrix.m[0]=n1.x;
        matrix.m[4]=n1.y;
        matrix.m[8]=n1.z;
        matrix.m[12]=0;
        matrix.m[1]=n2.x;
        matrix.m[5]=n2.y;
        matrix.m[9]=n2.z;
        matrix.m[13]=0;
        matrix.m[2]=n3.x;
        matrix.m[6]=n3.y;
        matrix.m[10]=n3.z;
        matrix.m[14]=0;
        matrix.m[3]=0;
        matrix.m[7]=0;
        matrix.m[11]=0;
        matrix.m[15]=1;
        
        initPnts(20);
        for(int i=i1;i<i2;++i)
        {
            int ind=loops[i];
            int ind1=ind;
            int j1=list[ind1].index;
            vtx=matrix.multiply(vertices[j1]);
            j1=storePoint(vtx.x,vtx.y);
            list[ind1].index=j1;
            ind1=list[ind1].next;
            j1=list[ind1].index;
            while(ind1!=ind)
            {
                vtx=matrix.multiply(vertices[j1]);
                j1=storePoint(vtx.x,vtx.y);
                list[ind1].index=j1;
                ind1=list[ind1].next;
                j1=list[ind1].index;
            }
        }
    }

    private void adjustOrientation(int i1,int i2)
    {
        if(numLoops>=maxNumPolyArea)
        {
            maxNumPolyArea=numLoops;
            double old[]=polyArea;
            polyArea=new double[maxNumPolyArea];
            if(old!=null)
                System.arraycopy(old,0,polyArea,0,old.length);
        }

        for(int i=i1;i<i2;++i)
        {
            int ind=loops[i];
            polyArea[i]=polygonArea(ind);
        }

        double area=Math.abs(polyArea[i1]);
        int outer=i1;
        for(int i=i1+1;i<i2;++i)
            if(area<Math.abs(polyArea[i]))
            {
                area=Math.abs(polyArea[i]);
                outer=i;
            }

        if(outer!=i1)
        {
            int ind=loops[i1];
            loops[i1]=loops[outer];
            loops[outer]=ind;

            area=polyArea[i1];
            polyArea[i1]=polyArea[outer];
            polyArea[outer]=area;
        }

        if(polyArea[i1]<0.0)
            swapLinks(loops[i1]);

        for(int i=i1+1;i<i2;++i)
            if(polyArea[i]>0.0)
                swapLinks(loops[i]);
    }

    private double polygonArea(int ind)
    {
        int hook=0;
        double area1=0;

        int ind1=ind;
        int i1=list[ind1].index;
        int ind2=list[ind1].next;
        int i2=list[ind2].index;
        double area=stableDet2D(hook,i1,i2);

        ind1=ind2;
        i1=i2;
        while(ind1!=ind)
        {
            ind2=list[ind1].next;
            i2=list[ind2].index;
            area1=stableDet2D(hook,i1,i2);
            area+=area1;
            ind1=ind2;
            i1=i2;
        }

        return area;
    }

    private void determineOrientation(int ind)
    {
        if(polygonArea(ind)<0.0)
        {
            swapLinks(ind);
            ccwLoop=false;
        }
    }
}
