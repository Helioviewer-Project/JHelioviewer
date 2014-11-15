package org.helioviewer.jhv.base.triangulate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector3d;

class Triangulator
{
    static private class HeapNode
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

    static private class PntNode
    {
        int pnt;
        int next;

        PntNode(int _pnt,int _next)
        {
            pnt=_pnt;
            next=_next;
        }
    }

    static private class ListNode
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

    static private class Distance
    {
        int ind;
        double dist;
    }

    static private class Left
    {
        int ind;
        int index;
    }

    static private class Triangle
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

    private static final double ZERO=1.0e-8;
    private static final int INC_LIST_BK=100;
    private static final int INC_LOOP_BK=20;
    private static final int INC_POINT_BK=100;
    private static final int INC_DIST_BK=50;

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
            else if(simpleFace(this,loops[i1]))
                proceed=false;
            else
                proceed=true;

            if(proceed)
            {
                for(int lpIndex=0;lpIndex<faces[j];lpIndex++)
                    preProcessList(i1+lpIndex);

                projectFace(this,i1,i2);
                cleanPolyhedralFace(this,i1,i2);
                if(faces[j]==1)
                    determineOrientation(this,loops[i1]);
                else
                    adjustOrientation(this,i1,i2);

                if(faces[j]>1)
                    prepareNoHashEdges(this,i1,i2);

                for(i=i1;i<i2;++i)
                    classifyAngles(this,loops[i]);

                if(faces[j]>1)
                    constructBridges(this,i1,i2);

                firstNode=loops[i1];
                prepareNoHashPnts(this,i1);
                classifyEars(this,loops[i1]);
                done[0]=false;

                while(!done[0])
                {
                    if(!clipEar(this,done))
                    {
                        if(reset)
                        {
                            ind=firstNode;

                            loops[i1]=ind;
                            if(desperate(this,ind,i1,done))
                            {
                                if(!letsHope(this,ind))
                                    return;
                            }
                            else
                                reset=false;
                        }
                        else
                        {
                            ind=firstNode;

                            classifyEars(this,ind);
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
                            prepareNoHashPnts(this,i1);
                            classifyEars(this,ind);
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

    private static void constructBridges(Triangulator triRef,int loopMin,int loopMax)
    {
        int i,numDist,numLeftMost;

        int[] i0=new int[1];
        int[] ind0=new int[1];
        int[] i1=new int[1];
        int[] ind1=new int[1];

        int[] iTmp=new int[1];
        int[] indTmp=new int[1];

        numLeftMost=loopMax-loopMin-1;

        if(numLeftMost>triRef.maxNumLeftMost)
        {
            triRef.maxNumLeftMost=numLeftMost;
            triRef.leftMost=new Left[numLeftMost];
        }

        findLeftMostVertex(triRef,triRef.loops[loopMin],ind0,i0);
        int j=0;
        for(i=loopMin+1;i<loopMax;++i)
        {
            findLeftMostVertex(triRef,triRef.loops[i],indTmp,iTmp);
            triRef.leftMost[j]=new Left();
            triRef.leftMost[j].ind=indTmp[0];
            triRef.leftMost[j].index=iTmp[0];

            j++;
        }

        Arrays.sort(triRef.leftMost,0,numLeftMost,new Comparator<Left>()
        {
            public int compare(Left _a,Left _b)
            {
                return l_comp(_a,_b);
            }
        });

        numDist=triRef.numPoints+2*triRef.numLoops;
        triRef.maxNumDist=numDist;
        triRef.distances=new Distance[numDist];
        for(int k=0;k<triRef.maxNumDist;k++)
            triRef.distances[k]=new Distance();

        for(j=0;j<numLeftMost;++j)
        {
            findBridge(triRef,ind0[0],i0[0],triRef.leftMost[j].index,ind1,i1);
            if(i1[0]==triRef.leftMost[j].index)
                simpleBridge(triRef,ind1[0],triRef.leftMost[j].ind);
            else
                insertBridge(triRef,ind1[0],i1[0],triRef.leftMost[j].ind,triRef.leftMost[j].index);
        }
    }

    private static boolean findBridge(Triangulator triRef,int ind,int i,int start,int[] ind1,int[] i1)
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
        if(numDist>=triRef.maxNumDist)
        {
            triRef.maxNumDist+=Triangulator.INC_DIST_BK;
            old=triRef.distances;
            triRef.distances=new Distance[triRef.maxNumDist];
            System.arraycopy(old,0,triRef.distances,0,old.length);
            for(int k=old.length;k<triRef.maxNumDist;k++)
                triRef.distances[k]=new Distance();
        }

        triRef.distances[numDist].dist=baseLength(triRef.points[start],triRef.points[i1[0]]);
        triRef.distances[numDist].ind=ind1[0];
        ++numDist;

        ind1[0]=triRef.list[ind1[0]].next;
        i1[0]=triRef.list[ind1[0]].index;
        while(ind1[0]!=ind)
        {
            if(i1[0]==start)
                return true;
            if(numDist>=triRef.maxNumDist)
            {
                triRef.maxNumDist+=Triangulator.INC_DIST_BK;
                old=triRef.distances;
                triRef.distances=new Distance[triRef.maxNumDist];
                System.arraycopy(old,0,triRef.distances,0,old.length);
                for(int k=old.length;k<triRef.maxNumDist;k++)
                    triRef.distances[k]=new Distance();
            }

            triRef.distances[numDist].dist=baseLength(triRef.points[start],triRef.points[i1[0]]);
            triRef.distances[numDist].ind=ind1[0];
            ++numDist;
            ind1[0]=triRef.list[ind1[0]].next;
            i1[0]=triRef.list[ind1[0]].index;
        }

        Arrays.sort(triRef.distances,0,numDist,new Comparator<Distance>()
        {
            public int compare(Distance _a,Distance _b)
            {
                return d_comp(_a,_b);
            }
        });

        for(j=0;j<numDist;++j)
        {
            ind1[0]=triRef.distances[j].ind;
            i1[0]=triRef.list[ind1[0]].index;
            if(i1[0]<=start)
            {
                ind0=triRef.list[ind1[0]].prev;
                i0=triRef.list[ind0].index;
                ind2=triRef.list[ind1[0]].next;
                i2=triRef.list[ind2].index;
                convex=triRef.list[ind1[0]].convex>0;

                coneOk=isInCone(triRef,i0,i1[0],i2,start,convex);
                if(coneOk)
                {
                    bb=new BBox(triRef,i1[0],start);
                    if(!noHashEdgeIntersectionExists(triRef,bb,-1,-1,ind1[0],-1))
                        return true;
                }
            }
        }

        for(j=0;j<numDist;++j)
        {
            ind1[0]=triRef.distances[j].ind;
            i1[0]=triRef.list[ind1[0]].index;
            ind0=triRef.list[ind1[0]].prev;
            i0=triRef.list[ind0].index;
            ind2=triRef.list[ind1[0]].next;
            i2=triRef.list[ind2].index;
            bb=new BBox(triRef,i1[0],start);
            if(!noHashEdgeIntersectionExists(triRef,bb,-1,-1,ind1[0],-1))
                return true;
        }

        ind1[0]=ind;
        i1[0]=i;

        return false;
    }

    private static void prepareNoHashEdges(Triangulator triRef,int currLoopMin,int currLoopMax)
    {
        triRef.loopMin=currLoopMin;
        triRef.loopMax=currLoopMax;
        return;
    }

    private static boolean checkArea(Triangulator triRef,int ind4,int ind5)
    {
        int ind1,ind2;
        int i0,i1,i2;
        double area=0.0,area1=0,area2=0.0;

        i0=triRef.list[ind4].index;
        ind1=triRef.list[ind4].next;
        i1=triRef.list[ind1].index;

        while(ind1!=ind5)
        {
            ind2=triRef.list[ind1].next;
            i2=triRef.list[ind2].index;
            area=stableDet2D(triRef,i0,i1,i2);
            area1+=area;
            ind1=ind2;
            i1=i2;
        }

        if((area1<=Triangulator.ZERO))
            return false;

        ind1=triRef.list[ind5].next;
        i1=triRef.list[ind1].index;
        while(ind1!=ind4)
        {
            ind2=triRef.list[ind1].next;
            i2=triRef.list[ind2].index;
            area=stableDet2D(triRef,i0,i1,i2);
            area2+=area;
            ind1=ind2;
            i1=i2;
        }

        return !(area2<=Triangulator.ZERO);
    }

    private static boolean checkBottleNeck(Triangulator triRef,int i1,int i2,int i3,int ind4)
    {
        int ind5;
        int i4,i5;
        boolean flag;

        i4=i1;

        ind5=triRef.list[ind4].prev;
        i5=triRef.list[ind5].index;
        if((i5!=i2)&&(i5!=i3))
        {
            flag=pntInTriangle(triRef,i1,i2,i3,i5);
            if(flag)
                return true;
        }

        if(i2<=i3)
        {
            if(i4<=i5)
                flag=segIntersect(triRef,i2,i3,i4,i5,-1);
            else
                flag=segIntersect(triRef,i2,i3,i5,i4,-1);
        }
        else
        {
            if(i4<=i5)
                flag=segIntersect(triRef,i3,i2,i4,i5,-1);
            else
                flag=segIntersect(triRef,i3,i2,i5,i4,-1);
        }
        if(flag)
            return true;

        ind5=triRef.list[ind4].next;
        i5=triRef.list[ind5].index;

        if((i5!=i2)&&(i5!=i3))
        {
            flag=pntInTriangle(triRef,i1,i2,i3,i5);
            if(flag)
                return true;
        }

        if(i2<=i3)
        {
            if(i4<=i5)
                flag=segIntersect(triRef,i2,i3,i4,i5,-1);
            else
                flag=segIntersect(triRef,i2,i3,i5,i4,-1);
        }
        else
        {
            if(i4<=i5)
                flag=segIntersect(triRef,i3,i2,i4,i5,-1);
            else
                flag=segIntersect(triRef,i3,i2,i5,i4,-1);
        }

        if(flag)
            return true;

        ind5=triRef.list[ind4].next;
        i5=triRef.list[ind5].index;
        while(ind5!=ind4)
        {
            if(i4==i5)
                if(checkArea(triRef,ind4,ind5))
                    return true;

            ind5=triRef.list[ind5].next;
            i5=triRef.list[ind5].index;
        }

        return false;
    }

    private static boolean noHashEdgeIntersectionExists(Triangulator triRef,BBox bb,int i1,int i2,int ind5,int i5)
    {
        int ind,ind2;
        int i,i3,i4;
        BBox bb1;

        triRef.identCntr=0;

        for(i=triRef.loopMin;i<triRef.loopMax;++i)
        {
            ind=triRef.loops[i];
            ind2=ind;
            i3=triRef.list[ind2].index;

            do
            {
                ind2=triRef.list[ind2].next;
                i4=triRef.list[ind2].index;
                bb1=new BBox(triRef,i3,i4);
                if(bb.BBoxOverlap(bb1))
                    if(segIntersect(triRef,bb.imin,bb.imax,bb1.imin,bb1.imax,i5))
                        return true;

                i3=i4;
            } while(ind2!=ind);
        }

        if(triRef.identCntr>=4)
            return checkBottleNeck(triRef,i5,i1,i2,ind5);

        return false;
    }

    private static void storeHeapData(Triangulator triRef,int index,double ratio,int ind,int prev,int next)
    {
        triRef.heap[index]=new HeapNode();
        triRef.heap[index].ratio=ratio;
        triRef.heap[index].index=ind;
        triRef.heap[index].prev=prev;
        triRef.heap[index].next=next;
    }

    private static void dumpOnHeap(Triangulator triRef,double ratio,int ind,int prev,int next)
    {
        int index;
        if(triRef.numHeap>=triRef.maxNumHeap)
        {
            HeapNode old[]=triRef.heap;
            triRef.maxNumHeap=triRef.maxNumHeap+triRef.numPoints;
            triRef.heap=new HeapNode[triRef.maxNumHeap];
            System.arraycopy(old,0,triRef.heap,0,old.length);
        }

        if(ratio==0.0)
        {
            if(triRef.numZero<triRef.numHeap)
                if(triRef.heap[triRef.numHeap]==null)
                    storeHeapData(triRef,triRef.numHeap,triRef.heap[triRef.numZero].ratio,triRef.heap[triRef.numZero].index,triRef.heap[triRef.numZero].prev,triRef.heap[triRef.numZero].next);
                else
                    triRef.heap[triRef.numHeap].set(triRef.heap[triRef.numZero]);

            index=triRef.numZero;
            triRef.numZero++;
        }
        else
            index=triRef.numHeap;

        storeHeapData(triRef,index,ratio,ind,prev,next);
        triRef.numHeap++;
    }

    private static void findLeftMostVertex(Triangulator triRef,int ind,int[] leftInd,int[] leftI)
    {
        int ind1,i1;

        ind1=ind;
        i1=triRef.list[ind1].index;
        leftInd[0]=ind1;
        leftI[0]=i1;
        ind1=triRef.list[ind1].next;
        i1=triRef.list[ind1].index;
        while(ind1!=ind)
        {
            if(i1<leftI[0])
            {
                leftInd[0]=ind1;
                leftI[0]=i1;
            }
            else if(i1==leftI[0])
            {
                if(triRef.list[ind1].convex<0)
                {
                    leftInd[0]=ind1;
                    leftI[0]=i1;
                }
            }
            ind1=triRef.list[ind1].next;
            i1=triRef.list[ind1].index;
        }

    }

    private static void simpleBridge(Triangulator triRef,int ind1,int ind2)
    {
        int prev,next;
        int i1,i2,prv,nxt;
        int angle;

        triRef.rotateLinks(ind1,ind2);

        i1=triRef.list[ind1].index;
        next=triRef.list[ind1].next;
        nxt=triRef.list[next].index;
        prev=triRef.list[ind1].prev;
        prv=triRef.list[prev].index;
        angle=isConvexAngle(triRef,prv,i1,nxt,ind1);
        triRef.list[ind1].convex=angle;

        i2=triRef.list[ind2].index;
        next=triRef.list[ind2].next;
        nxt=triRef.list[next].index;
        prev=triRef.list[ind2].prev;
        prv=triRef.list[prev].index;
        angle=isConvexAngle(triRef,prv,i2,nxt,ind2);
        triRef.list[ind2].convex=angle;
    }

    private static void insertBridge(Triangulator triRef,int ind1,int i1,int ind3,int i3)
    {
        int ind2,ind4,prev,next;
        int prv,nxt,angle;

        ind2=triRef.makeNode(i1);
        triRef.insertAfter(ind1,ind2);

        triRef.list[ind2].vcntIndex=triRef.list[ind1].vcntIndex;

        ind4=triRef.makeNode(i3);
        triRef.insertAfter(ind3,ind4);

        triRef.list[ind4].vcntIndex=triRef.list[ind3].vcntIndex;

        triRef.splitSplice(ind1,ind2,ind3,ind4);

        next=triRef.list[ind1].next;
        nxt=triRef.list[next].index;
        prev=triRef.list[ind1].prev;
        prv=triRef.list[prev].index;
        angle=isConvexAngle(triRef,prv,i1,nxt,ind1);
        triRef.list[ind1].convex=angle;

        next=triRef.list[ind2].next;
        nxt=triRef.list[next].index;
        prev=triRef.list[ind2].prev;
        prv=triRef.list[prev].index;
        angle=isConvexAngle(triRef,prv,i1,nxt,ind2);
        triRef.list[ind2].convex=angle;

        next=triRef.list[ind3].next;
        nxt=triRef.list[next].index;
        prev=triRef.list[ind3].prev;
        prv=triRef.list[prev].index;
        angle=isConvexAngle(triRef,prv,i3,nxt,ind3);
        triRef.list[ind3].convex=angle;

        next=triRef.list[ind4].next;
        nxt=triRef.list[next].index;
        prev=triRef.list[ind4].prev;
        prv=triRef.list[prev].index;
        angle=isConvexAngle(triRef,prv,i3,nxt,ind4);
        triRef.list[ind4].convex=angle;
    }

    private static int l_comp(Left a,Left b)
    {
        if(a.index<b.index)
            return -1;
        else if(a.index>b.index)
            return 1;
        else
            return 0;
    }

    private static int d_comp(Distance a,Distance b)
    {
        if(a.dist<b.dist)
            return -1;
        else if(a.dist>b.dist)
            return 1;
        else
            return 0;
    }

    private static boolean handleDegeneracies(Triangulator triRef,int i1,int ind1,int i2,int i3,int i4,int ind4)
    {
        int i0,i5;
        int type[]=new int[1];
        int ind0,ind2,ind5;
        double area=0.0,area1=0,area2=0.0;

        ind5=triRef.list[ind4].prev;
        i5=triRef.list[ind5].index;

        if((i5!=i2)&&(i5!=i3))
        {
            if(vtxInTriangle(triRef,i1,i2,i3,i5,type)&&(type[0]==0))
                return true;

            if(i2<=i3)
            {
                if(i4<=i5)
                {
                    if(segIntersect(triRef,i2,i3,i4,i5,-1))
                        return true;
                }
                else
                {
                    if(segIntersect(triRef,i2,i3,i5,i4,-1))
                        return true;
                }
            }
            else
            {
                if(i4<=i5)
                {
                    if(segIntersect(triRef,i3,i2,i4,i5,-1))
                        return true;
                }
                else
                {
                    if(segIntersect(triRef,i3,i2,i5,i4,-1))
                        return true;
                }
            }
        }

        ind5=triRef.list[ind4].next;
        i5=triRef.list[ind5].index;
        if((i5!=i2)&&(i5!=i3))
        {
            if(vtxInTriangle(triRef,i1,i2,i3,i5,type)&&(type[0]==0))
                return true;
            if(i2<=i3)
            {
                if(i4<=i5)
                {
                    if(segIntersect(triRef,i2,i3,i4,i5,-1))
                        return true;
                }
                else
                {
                    if(segIntersect(triRef,i2,i3,i5,i4,-1))
                        return true;
                }
            }
            else
            {
                if(i4<=i5)
                {
                    if(segIntersect(triRef,i3,i2,i4,i5,-1))
                        return true;
                }
                else
                {
                    if(segIntersect(triRef,i3,i2,i5,i4,-1))
                        return true;
                }
            }
        }

        i0=i1;
        ind0=ind1;
        ind1=triRef.list[ind1].next;
        i1=triRef.list[ind1].index;
        while(ind1!=ind4)
        {
            ind2=triRef.list[ind1].next;
            i2=triRef.list[ind2].index;
            area=stableDet2D(triRef,i0,i1,i2);
            area1+=area;
            ind1=ind2;
            i1=i2;
        }

        ind1=triRef.list[ind0].prev;
        i1=triRef.list[ind1].index;
        while(ind1!=ind4)
        {
            ind2=triRef.list[ind1].prev;
            i2=triRef.list[ind2].index;
            area=stableDet2D(triRef,i0,i1,i2);
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

    private static boolean desperate(Triangulator triRef,int ind,int i,boolean[] splitted)
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

        if(existsCrossOver(triRef,ind,ind1,i1,ind2,i2,ind3,i3,ind4,i4))
        {
            handleCrossOver(triRef,ind1[0],i1[0],ind2[0],i2[0],ind3[0],i3[0],ind4[0],i4[0]);
            return false;
        }

        prepareNoHashEdges(triRef,i,i+1);

        if(existsSplit(triRef,ind,ind1,i1,ind2,i2))
        {
            handleSplit(triRef,ind1[0],i1[0],ind2[0],i2[0]);
            splitted[0]=true;
            return false;
        }

        return true;
    }

    private static int cleanPolyhedralFace(Triangulator triRef,int i1,int i2)
    {
        int removed;
        int i,j,numSorted,index;
        int ind1,ind2;

        triRef.pUnsorted.clear();
        for(i=0;i<triRef.numPoints;++i)
            triRef.pUnsorted.add(triRef.points[i]);

        Arrays.sort(triRef.points,0,triRef.numPoints,new Comparator<Vector2d>()
        {
            public int compare(Vector2d _a,Vector2d _b)
            {
                return pComp(_a,_b);
            }
        });

        i=0;
        for(j=1;j<triRef.numPoints;++j)
            if(pComp(triRef.points[i],triRef.points[j])!=0)
            {
                i++;
                triRef.points[i]=triRef.points[j];
            }

        numSorted=i+1;
        removed=triRef.numPoints-numSorted;

        for(i=i1;i<i2;++i)
        {
            ind1=triRef.loops[i];
            ind2=triRef.list[ind1].next;
            index=triRef.list[ind2].index;
            while(ind2!=ind1)
            {
                j=findPInd(triRef.points,numSorted,triRef.pUnsorted.get(index));
                triRef.list[ind2].index=j;
                ind2=triRef.list[ind2].next;
                index=triRef.list[ind2].index;
            }
            j=findPInd(triRef.points,numSorted,triRef.pUnsorted.get(index));
            triRef.list[ind2].index=j;
        }

        triRef.numPoints=numSorted;

        return removed;
    }

    private static int findPInd(Vector2d sorted[],int numPts,Vector2d pnt)
    {
        for(int i=0;i<numPts;i++)
            if((pnt.x==sorted[i].x)&&(pnt.y==sorted[i].y))
                return i;
        return -1;
    }

    private static int pComp(Vector2d a,Vector2d b)
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

    private static double angle(Triangulator triRef,Vector2d p,Vector2d p1,Vector2d p2)
    {
        int sign=signEps(det2D(p2,p,p1),triRef.epsilon);
        if(sign==0)
            return 0.0;

        Vector2d v1=new Vector2d();
        Vector2d v2=new Vector2d();
        vectorSub2D(p1,p,v1);
        vectorSub2D(p2,p,v2);

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

    private static boolean existsCrossOver(Triangulator triRef,int ind,int[] ind1,int[] i1,int[] ind2,int[] i2,int[] ind3,int[] i3,int[] ind4,int[] i4)
    {
        BBox bb1,bb2;

        ind1[0]=ind;
        i1[0]=triRef.list[ind1[0]].index;
        ind2[0]=triRef.list[ind1[0]].next;
        i2[0]=triRef.list[ind2[0]].index;
        ind3[0]=triRef.list[ind2[0]].next;
        i3[0]=triRef.list[ind3[0]].index;
        ind4[0]=triRef.list[ind3[0]].next;
        i4[0]=triRef.list[ind4[0]].index;

        do
        {
            bb1=new BBox(triRef,i1[0],i2[0]);
            bb2=new BBox(triRef,i3[0],i4[0]);
            if(bb1.BBoxOverlap(bb2))
            {
                if(segIntersect(triRef,bb1.imin,bb1.imax,bb2.imin,bb2.imax,-1))
                    return true;
            }
            ind1[0]=ind2[0];
            i1[0]=i2[0];
            ind2[0]=ind3[0];
            i2[0]=i3[0];
            ind3[0]=ind4[0];
            i3[0]=i4[0];
            ind4[0]=triRef.list[ind3[0]].next;
            i4[0]=triRef.list[ind4[0]].index;

        } while(ind1[0]!=ind);

        return false;
    }

    private static void handleCrossOver(Triangulator triRef,int ind1,int i1,int ind2,int i2,int ind3,int i3,int ind4,int i4)
    {
        double ratio1,ratio4;
        boolean first;
        int angle1,angle4;

        angle1=triRef.list[ind1].convex;
        angle4=triRef.list[ind4].convex;
        if(angle1<angle4)
            first=true;
        else if(angle1>angle4)
            first=false;
        else if(triRef.earsSorted)
        {
            ratio1=getRatio(triRef,i3,i4,i1);
            ratio4=getRatio(triRef,i1,i2,i4);
            if(ratio4<ratio1)
                first=false;
            else
                first=true;
        }
        else
            first=true;

        if(first)
        {
            triRef.deleteLinks(ind2);
            triRef.storeTriangle(ind1,ind2,ind3);
            triRef.list[ind3].convex=1;
            dumpOnHeap(triRef,0.0,ind3,ind1,ind4);
        }
        else
        {
            triRef.deleteLinks(ind3);
            triRef.storeTriangle(ind2,ind3,ind4);
            triRef.list[ind2].convex=1;
            dumpOnHeap(triRef,0.0,ind2,ind1,ind4);
        }
    }

    private static boolean letsHope(Triangulator triRef,int ind)
    {
        int ind0,ind1,ind2;
        ind1=ind;

        do
        {
            if(triRef.list[ind1].convex>0)
            {
                ind0=triRef.list[ind1].prev;
                ind2=triRef.list[ind1].next;
                dumpOnHeap(triRef,0.0,ind1,ind0,ind2);
                return true;
            }
            ind1=triRef.list[ind1].next;
        } while(ind1!=ind);

        triRef.list[ind].convex=1;
        ind0=triRef.list[ind].prev;
        ind2=triRef.list[ind].next;
        dumpOnHeap(triRef,0.0,ind,ind0,ind2);
        return true;
    }

    private static boolean existsSplit(Triangulator triRef,int ind,int[] ind1,int[] i1,int[] ind2,int[] i2)
    {
        int ind3,ind4,ind5;
        int i3,i4,i5;

        if(triRef.numPoints>triRef.maxNumDist)
        {
            triRef.maxNumDist=triRef.numPoints;
            triRef.distances=new Distance[triRef.maxNumDist];
            for(int k=0;k<triRef.maxNumDist;k++)
                triRef.distances[k]=new Distance();
        }
        ind1[0]=ind;
        i1[0]=triRef.list[ind1[0]].index;
        ind4=triRef.list[ind1[0]].next;
        i4=triRef.list[ind4].index;
        ind5=triRef.list[ind4].next;
        i5=triRef.list[ind5].index;
        ind3=triRef.list[ind1[0]].prev;
        i3=triRef.list[ind3].index;
        if(foundSplit(triRef,ind5,i5,ind3,ind1[0],i1[0],i3,i4,ind2,i2))
            return true;
        i3=i1[0];
        ind1[0]=ind4;
        i1[0]=i4;
        ind4=ind5;
        i4=i5;
        ind5=triRef.list[ind4].next;
        i5=triRef.list[ind5].index;

        while(ind5!=ind)
        {
            if(foundSplit(triRef,ind5,i5,ind,ind1[0],i1[0],i3,i4,ind2,i2))
                return true;
            i3=i1[0];
            ind1[0]=ind4;
            i1[0]=i4;
            ind4=ind5;
            i4=i5;
            ind5=triRef.list[ind4].next;
            i5=triRef.list[ind5].index;
        }

        return false;
    }

    private static int windingNumber(Triangulator triRef,int ind,Vector2d p)
    {
        double angle;
        int ind2;
        int i1,i2,number;

        i1=triRef.list[ind].index;
        ind2=triRef.list[ind].next;
        i2=triRef.list[ind2].index;
        angle=angle(triRef,p,triRef.points[i1],triRef.points[i2]);
        while(ind2!=ind)
        {
            i1=i2;
            ind2=triRef.list[ind2].next;
            i2=triRef.list[ind2].index;
            angle+=angle(triRef,p,triRef.points[i1],triRef.points[i2]);
        }

        angle+=Math.PI;
        number=(int)(angle/(Math.PI*2.0));

        return number;
    }

    private static boolean foundSplit(Triangulator triRef,int ind5,int i5,int ind,int ind1,int i1,int i3,int i4,int[] ind2,int[] i2)
    {
        Vector2d center;
        int numDist=0;
        int j,i6,i7;
        int ind6,ind7;
        BBox bb;
        boolean convex,coneOk;

        do
        {
            triRef.distances[numDist].dist=baseLength(triRef.points[i1],triRef.points[i5]);
            triRef.distances[numDist].ind=ind5;
            ++numDist;
            ind5=triRef.list[ind5].next;
            i5=triRef.list[ind5].index;
        } while(ind5!=ind);

        Arrays.sort(triRef.distances,0,numDist,new Comparator<Distance>()
        {
            public int compare(Distance _a,Distance _b)
            {
                return d_comp(_a,_b);
            }
        });

        for(j=0;j<numDist;++j)
        {
            ind2[0]=triRef.distances[j].ind;
            i2[0]=triRef.list[ind2[0]].index;
            if(i1!=i2[0])
            {
                ind6=triRef.list[ind2[0]].prev;
                i6=triRef.list[ind6].index;
                ind7=triRef.list[ind2[0]].next;
                i7=triRef.list[ind7].index;

                convex=triRef.list[ind2[0]].convex>0;
                coneOk=isInCone(triRef,i6,i2[0],i7,i1,convex);
                if(coneOk)
                {
                    convex=triRef.list[ind1].convex>0;
                    coneOk=isInCone(triRef,i3,i1,i4,i2[0],convex);
                    if(coneOk)
                    {
                        bb=new BBox(triRef,i1,i2[0]);
                        if(!noHashEdgeIntersectionExists(triRef,bb,-1,-1,ind1,-1))
                        {
                            center=vectorAdd2D(triRef.points[i1],triRef.points[i2[0]]).scale(0.5);
                            if(windingNumber(triRef,ind,center)==1)
                                return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private static double baseLength(Vector2d u,Vector2d v)
    {
        double x,y;
        x=v.x-u.x;
        y=v.y-u.y;
        return Math.abs(x)+Math.abs(y);
    }

    private static double det2D(Vector2d _u,Vector2d _v,Vector2d _w)
    {
        return((_u.x-_v.x)*(_v.y-_w.y)+(_v.y-_u.y)*(_v.x-_w.x));
    }

    private static boolean inBetween(int i1,int i2,int i3)
    {
        return((i1<=i3)&&(i3<=i2));
    }

    private static boolean strictlyInBetween(int i1,int i2,int i3)
    {
        return((i1<i3)&&(i3<i2));
    }

    private static double stableDet2D(Triangulator triRef,int i,int j,int k)
    {
        double det;
        Vector2d numericsHP,numericsHQ,numericsHR;

        if((i==j)||(i==k)||(j==k))
            det=0.0;
        else
        {
            numericsHP=triRef.points[i];
            numericsHQ=triRef.points[j];
            numericsHR=triRef.points[k];

            if(i<j)
            {
                if(j<k)
                    det=det2D(numericsHP,numericsHQ,numericsHR);
                else if(i<k)
                    det=-det2D(numericsHP,numericsHR,numericsHQ);
                else
                    det=det2D(numericsHR,numericsHP,numericsHQ);
            }
            else
            {
                if(i<k)
                    det=-det2D(numericsHQ,numericsHP,numericsHR);
                else if(j<k)
                    det=det2D(numericsHQ,numericsHR,numericsHP);
                else
                    det=-det2D(numericsHR,numericsHQ,numericsHP);
            }
        }

        return det;
    }

    private static int orientation(Triangulator triRef,int i,int j,int k)
    {
        double numericsHDet=stableDet2D(triRef,i,j,k);
        if((numericsHDet<-triRef.epsilon))
            return -1;
        else if(!((numericsHDet)<=triRef.epsilon))
            return 1;
        else
            return 0;
    }

    private static boolean isInCone(Triangulator triRef,int i,int j,int k,int l,boolean convex)
    {
        if(convex)
        {
            if(i!=j)
            {
                int numericsHOri=orientation(triRef,i,j,l);
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
                int numericsHOri=orientation(triRef,j,k,l);
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
            if(orientation(triRef,i,j,l)<=0)
                if(orientation(triRef,j,k,l)<0)
                    return false;
        }
        return true;
    }

    private static int isConvexAngle(Triangulator triRef,int i,int j,int k,int ind)
    {
        double numericsHDot;
        int numericsHOri1;
        Vector2d numericsHP,numericsHQ;

        if(i==j)
            return 1;
        else if(j==k)
            return -1;

        numericsHOri1=orientation(triRef,i,j,k);
        if(numericsHOri1>0)
            return 1;
        else if(numericsHOri1<0)
            return -1;

        numericsHP=new Vector2d();
        numericsHQ=new Vector2d();
        vectorSub2D(triRef.points[i],triRef.points[j],numericsHP);
        vectorSub2D(triRef.points[k],triRef.points[j],numericsHQ);
        numericsHDot=dotProduct2D(numericsHP,numericsHQ);
        if(numericsHDot<0.0)
            return 0;
        else
            return spikeAngle(triRef,i,j,k,ind);
    }

    private static boolean pntInTriangle(Triangulator triRef,int i1,int i2,int i3,int i4)
    {
        if(orientation(triRef,i2,i3,i4)>=0)
            if(orientation(triRef,i1,i2,i4)>=0)
                if(orientation(triRef,i3,i1,i4)>=0)
                    return true;

        return false;
    }

    private static Vector2d vectorSub2D(Vector2d p,Vector2d q,Vector2d r)
    {
        return p.subtract(q);
    }

    private static boolean vtxInTriangle(Triangulator triRef,int i1,int i2,int i3,int i4,int[] type)
    {
        if(orientation(triRef,i2,i3,i4)>=0)
        {
            int numericsHOri1=orientation(triRef,i1,i2,i4);
            if(numericsHOri1>0)
            {
                numericsHOri1=orientation(triRef,i3,i1,i4);
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
                numericsHOri1=orientation(triRef,i3,i1,i4);
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

    private static boolean segIntersect(Triangulator triRef,int i1,int i2,int i3,int i4,int i5)
    {
        int ori1,ori2,ori3,ori4;

        if((i1==i2)||(i3==i4))
            return false;
        if((i1==i3)&&(i2==i4))
            return true;

        if((i3==i5)||(i4==i5))
            triRef.identCntr++;

        ori3=orientation(triRef,i1,i2,i3);
        ori4=orientation(triRef,i1,i2,i4);
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

        ori1=orientation(triRef,i3,i4,i1);
        ori2=orientation(triRef,i3,i4,i2);
        return !(((ori1<=0)&&(ori2<=0))||((ori1>=0)&&(ori2>=0)));
    }

    private static double getRatio(Triangulator triRef,int i,int j,int k)
    {
        double area,a,b,c,base,ratio;
        Vector2d p,q,r;

        p=triRef.points[i];
        q=triRef.points[j];
        r=triRef.points[k];

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

        area=stableDet2D(triRef,i,j,k);
        if((area<-triRef.epsilon))
            area=-area;
        else if(!(!((area)<=triRef.epsilon)))
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

    private static int spikeAngle(Triangulator triRef,int i,int j,int k,int ind)
    {
        int ind2=ind;
        int ind1=triRef.list[ind2].prev;
        int ind3=triRef.list[ind2].next;

        return recSpikeAngle(triRef,i,j,k,ind1,ind3);
    }

    private static int recSpikeAngle(Triangulator triRef,int i1,int i2,int i3,int ind1,int ind3)
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
                ind3=triRef.list[ind3].next;
                i3=triRef.list[ind3].index;

                if(ind1==ind3)
                    return 2;
                ori=orientation(triRef,i1,i2,i3);
                if(ori>0)
                    return 2;
                else if(ori<0)
                    return -2;
                else
                    return recSpikeAngle(triRef,i1,i2,i3,ind1,ind3);
            }
            else
            {
                i2=i1;
                ind1=triRef.list[ind1].prev;
                i1=triRef.list[ind1].index;
                if(ind1==ind3)
                    return 2;
                ori=orientation(triRef,i1,i2,i3);
                if(ori>0)
                    return 2;
                else if(ori<0)
                    return -2;
                else
                    return recSpikeAngle(triRef,i1,i2,i3,ind1,ind3);
            }
        }
        else
        {
            i0=i2;
            i2=i1;
            ind1=triRef.list[ind1].prev;
            i1=triRef.list[ind1].index;

            if(ind1==ind3)
                return 2;
            ind3=triRef.list[ind3].next;
            i3=triRef.list[ind3].index;
            if(ind1==ind3)
                return 2;
            ori=orientation(triRef,i1,i2,i3);
            if(ori>0)
            {
                ori1=orientation(triRef,i1,i2,i0);
                if(ori1>0)
                {
                    ori2=orientation(triRef,i2,i3,i0);
                    if(ori2>0)
                        return -2;
                }
                return 2;
            }
            else if(ori<0)
            {
                ori1=orientation(triRef,i2,i1,i0);
                if(ori1>0)
                {
                    ori2=orientation(triRef,i3,i2,i0);
                    if(ori2>0)
                        return 2;
                }
                return -2;
            }
            else
            {
                pq=new Vector2d();
                vectorSub2D(triRef.points[i1],triRef.points[i2],pq);
                pr=new Vector2d();
                vectorSub2D(triRef.points[i3],triRef.points[i2],pr);
                dot=dotProduct2D(pq,pr);
                if(dot<0.0)
                {
                    ori=orientation(triRef,i2,i1,i0);
                    if(ori>0)
                        return 2;
                    else
                        return -2;
                }
                else
                    return recSpikeAngle(triRef,i1,i2,i3,ind1,ind3);
            }
        }
    }

    private static double dotProduct2D(Vector2d u,Vector2d v)
    {
        return((u.x*v.x)+(u.y*v.y));
    }

    private static Vector2d vectorAdd2D(Vector2d p,Vector2d q)
    {
        return p.add(q);
    }

    private static void handleSplit(Triangulator triRef,int ind1,int i1,int ind3,int i3)
    {
        int ind2=triRef.makeNode(i1);
        triRef.insertAfter(ind1,ind2);

        triRef.list[ind2].vcntIndex=triRef.list[ind1].vcntIndex;

        int ind4=triRef.makeNode(i3);
        triRef.insertAfter(ind3,ind4);

        triRef.list[ind4].vcntIndex=triRef.list[ind3].vcntIndex;

        triRef.splitSplice(ind1,ind2,ind3,ind4);

        triRef.storeChain(ind1);
        triRef.storeChain(ind3);

        int next=triRef.list[ind1].next;
        int nxt=triRef.list[next].index;
        int prev=triRef.list[ind1].prev;
        int prv=triRef.list[prev].index;
        int angle=isConvexAngle(triRef,prv,i1,nxt,ind1);
        triRef.list[ind1].convex=angle;

        next=triRef.list[ind2].next;
        nxt=triRef.list[next].index;
        prev=triRef.list[ind2].prev;
        prv=triRef.list[prev].index;
        angle=isConvexAngle(triRef,prv,i1,nxt,ind2);
        triRef.list[ind2].convex=angle;

        next=triRef.list[ind3].next;
        nxt=triRef.list[next].index;
        prev=triRef.list[ind3].prev;
        prv=triRef.list[prev].index;
        angle=isConvexAngle(triRef,prv,i3,nxt,ind3);
        triRef.list[ind3].convex=angle;

        next=triRef.list[ind4].next;
        nxt=triRef.list[next].index;
        prev=triRef.list[ind4].prev;
        prv=triRef.list[prev].index;
        angle=isConvexAngle(triRef,prv,i3,nxt,ind4);
        triRef.list[ind4].convex=angle;
    }

    private static void classifyAngles(Triangulator triRef,int ind)
    {
        int ind0,ind1,ind2;
        int i0,i1,i2;
        int angle;

        ind1=ind;
        i1=triRef.list[ind1].index;
        ind0=triRef.list[ind1].prev;
        i0=triRef.list[ind0].index;

        do
        {
            ind2=triRef.list[ind1].next;
            i2=triRef.list[ind2].index;
            angle=isConvexAngle(triRef,i0,i1,i2,ind1);
            triRef.list[ind1].convex=angle;
            i0=i1;
            i1=i2;
            ind1=ind2;
        } while(ind1!=ind);
    }

    private static boolean isEar(Triangulator triRef,int ind2,int[] ind1,int[] ind3,double[] ratio)
    {
        BBox bb;
        boolean convex,coneOk;

        int i2=triRef.list[ind2].index;
        ind3[0]=triRef.list[ind2].next;
        int i3=triRef.list[ind3[0]].index;
        int ind4=triRef.list[ind3[0]].next;
        int i4=triRef.list[ind4].index;
        ind1[0]=triRef.list[ind2].prev;
        int i1=triRef.list[ind1[0]].index;
        int ind0=triRef.list[ind1[0]].prev;
        int i0=triRef.list[ind0].index;

        if((i1==i3)||(i1==i2)||(i2==i3)||(triRef.list[ind2].convex==2))
        {
            ratio[0]=0.0;
            return true;
        }

        if(i0==i3)
        {
            if((triRef.list[ind0].convex<0)||(triRef.list[ind3[0]].convex<0))
            {
                ratio[0]=0.0;
                return true;
            }
            else
                return false;
        }

        if(i1==i4)
        {
            if((triRef.list[ind1[0]].convex<0)||(triRef.list[ind4].convex<0))
            {
                ratio[0]=0.0;
                return true;
            }
            else
                return false;
        }

        convex=triRef.list[ind1[0]].convex>0;
        coneOk=isInCone(triRef,i0,i1,i2,i3,convex);

        if(!coneOk)
            return false;
        convex=triRef.list[ind3[0]].convex>0;
        coneOk=isInCone(triRef,i2,i3,i4,i1,convex);

        if(coneOk)
        {
            bb=new BBox(triRef,i1,i3);
            if(!noHashIntersectionExists(triRef,i2,ind2,i3,i1,bb))
            {
                if(triRef.earsSorted)
                    ratio[0]=getRatio(triRef,i1,i3,i2);
                else
                    ratio[0]=1.0;
                return true;
            }
        }

        return false;
    }

    private static boolean noHashIntersectionExists(Triangulator triRef,int i1,int ind1,int i2,int i3,BBox bb)
    {
        int indVtx,ind5;
        int indPnt;
        int i4;
        int type[]=new int[1];
        double y;

        if(triRef.numReflex<=0)
            return false;

        if(i1<bb.imin)
            bb.imin=i1;
        else if(i1>bb.imax)
            bb.imax=i1;
        y=triRef.points[i1].y;
        if(y<bb.ymin)
            bb.ymin=y;
        else if(y>bb.ymax)
            bb.ymax=y;

        indPnt=triRef.reflexVertices;
        do
        {
            indVtx=triRef.vtxList[indPnt].pnt;
            i4=triRef.list[indVtx].index;

            if(bb.pntInBBox(triRef,i4))
            {
                ind5=triRef.list[indVtx].next;
                if((indVtx!=ind1)&&(indVtx!=ind5))
                {
                    if(i4==i1)
                    {
                        if(handleDegeneracies(triRef,i1,ind1,i2,i3,i4,indVtx))
                            return true;
                    }
                    else if((i4!=i2)&&(i4!=i3))
                    {
                        if(vtxInTriangle(triRef,i1,i2,i3,i4,type))
                            return true;
                    }
                }
            }
            indPnt=triRef.vtxList[indPnt].next;

        } while(indPnt!=-1);

        return false;
    }

    private static void deleteFromList(Triangulator triRef,int i)
    {
        if(triRef.numReflex==0)
            return;

        int indPnt=triRef.reflexVertices;
        int indVtx=triRef.vtxList[indPnt].pnt;

        if(indVtx==i)
        {
            triRef.reflexVertices=triRef.vtxList[indPnt].next;
            triRef.numReflex--;
        }
        else
        {
            int indPnt1=triRef.vtxList[indPnt].next;
            while(indPnt1!=-1)
            {
                indVtx=triRef.vtxList[indPnt1].pnt;
                if(indVtx==i)
                {
                    triRef.vtxList[indPnt].next=triRef.vtxList[indPnt1].next;
                    indPnt1=-1;
                    triRef.numReflex--;
                }
                else
                {
                    indPnt=indPnt1;
                    indPnt1=triRef.vtxList[indPnt].next;
                }
            }
        }
    }

    private static boolean clipEar(Triangulator triRef,boolean[] done)
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
            if(!deleteFromHeap(triRef,ind2,index1,index3))
                return false;

            ind1=triRef.list[ind2[0]].prev;
            i1=triRef.list[ind1].index;
            ind3=triRef.list[ind2[0]].next;
            i3=triRef.list[ind3].index;
        } while((index1[0]!=ind1)||(index3[0]!=ind3));

        triRef.deleteLinks(ind2[0]);
        triRef.storeTriangle(ind1,ind2[0],ind3);

        ind0=triRef.list[ind1].prev;
        i0=triRef.list[ind0].index;
        if(ind0==ind3)
        {
            done[0]=true;
            return true;
        }
        angle1=isConvexAngle(triRef,i0,i1,i3,ind1);

        ind4=triRef.list[ind3].next;
        i4=triRef.list[ind4].index;

        angle3=isConvexAngle(triRef,i1,i3,i4,ind3);

        if(i1!=i3)
        {
            if((angle1>=0)&&(triRef.list[ind1].convex<0))
                deleteFromList(triRef,ind1);
            if((angle3>=0)&&(triRef.list[ind3].convex<0))
                deleteFromList(triRef,ind3);
        }
        else
        {
            if((angle1>=0)&&(triRef.list[ind1].convex<0))
                deleteFromList(triRef,ind1);
            else if((angle3>=0)&&(triRef.list[ind3].convex<0))
                deleteFromList(triRef,ind3);
        }

        triRef.list[ind1].convex=angle1;
        triRef.list[ind3].convex=angle3;

        if(angle1>0)
            if(isEar(triRef,ind1,index0,index2,ratio))
                dumpOnHeap(triRef,ratio[0],ind1,index0[0],index2[0]);

        if(angle3>0)
            if(isEar(triRef,ind3,index2,index4,ratio))
                dumpOnHeap(triRef,ratio[0],ind3,index2[0],index4[0]);

        ind0=triRef.list[ind1].prev;
        ind4=triRef.list[ind3].next;
        if(ind0==ind4)
        {
            triRef.storeTriangle(ind1,ind3,ind4);
            done[0]=true;
        }
        else
            done[0]=false;

        return true;
    }

    private static boolean deleteFromHeap(Triangulator triRef,int[] ind,int[] prev,int[] next)
    {
        double rnd;
        int rndInd;

        if(triRef.numZero>0)
        {
            triRef.numZero--;
            triRef.numHeap--;

            ind[0]=triRef.heap[triRef.numZero].index;
            prev[0]=triRef.heap[triRef.numZero].prev;
            next[0]=triRef.heap[triRef.numZero].next;
            if(triRef.numZero<triRef.numHeap)
                triRef.heap[triRef.numZero].set(triRef.heap[triRef.numHeap]);

            return true;
        }
        else if(triRef.earsRandom)
        {
            if(triRef.numHeap<=0)
            {
                triRef.numHeap=0;
                return false;
            }
            rnd=Math.random();
            rndInd=(int)(rnd*triRef.numHeap);
            triRef.numHeap--;
            if(rndInd>triRef.numHeap)
                rndInd=triRef.numHeap;

            ind[0]=triRef.heap[rndInd].index;
            prev[0]=triRef.heap[rndInd].prev;
            next[0]=triRef.heap[rndInd].next;
            if(rndInd<triRef.numHeap)
                triRef.heap[rndInd].set(triRef.heap[triRef.numHeap]);

            return true;
        }
        else
        {
            if(triRef.numHeap<=0)
            {
                triRef.numHeap=0;
                return false;
            }

            triRef.numHeap--;
            ind[0]=triRef.heap[triRef.numHeap].index;
            prev[0]=triRef.heap[triRef.numHeap].prev;
            next[0]=triRef.heap[triRef.numHeap].next;

            return true;
        }
    }

    private static void classifyEars(Triangulator triRef,int ind)
    {
        int ind1;
        int[] ind0,ind2;
        double[] ratio;

        ind0=new int[1];
        ind2=new int[1];
        ratio=new double[1];

        triRef.maxNumHeap=triRef.numPoints;
        triRef.heap=new HeapNode[triRef.maxNumHeap];
        triRef.numHeap=0;
        triRef.numZero=0;

        ind1=ind;
        do
        {
            if((triRef.list[ind1].convex>0)&&isEar(triRef,ind1,ind0,ind2,ratio))
                dumpOnHeap(triRef,ratio[0],ind1,ind0[0],ind2[0]);

            ind1=triRef.list[ind1].next;
        } while(ind1!=ind);
    }

    private static boolean simpleFace(Triangulator triRef,int ind1)
    {
        int ind0,ind2,ind3,ind4;
        int i1,i2,i3,i4;

        Vector3d pq,pr,nr;

        double x,y,z;
        int ori2,ori4;

        ind0=triRef.list[ind1].prev;
        if(ind0==ind1)
            return true;

        ind2=triRef.list[ind1].next;
        i2=triRef.list[ind2].index;
        if(ind0==ind2)
            return true;

        ind3=triRef.list[ind2].next;
        i3=triRef.list[ind3].index;
        if(ind0==ind3)
        {
            triRef.storeTriangle(ind1,ind2,ind3);
            return true;
        }

        ind4=triRef.list[ind3].next;
        i4=triRef.list[ind4].index;
        if(ind0==ind4)
        {
            triRef.initPnts(5);
            i1=triRef.list[ind1].index;

            pq=vectorSub(triRef.vertices[i1],triRef.vertices[i2]);
            pr=vectorSub(triRef.vertices[i3],triRef.vertices[i2]);
            nr=vectorProduct(pq,pr);

            x=Math.abs(nr.x);
            y=Math.abs(nr.y);
            z=Math.abs(nr.z);
            if((z>=x)&&(z>=y))
            {
                triRef.points[1]=new Vector2d(triRef.vertices[i1].x,triRef.vertices[i1].y);
                triRef.points[2]=new Vector2d(triRef.vertices[i2].x,triRef.vertices[i2].y);
                triRef.points[3]=new Vector2d(triRef.vertices[i3].x,triRef.vertices[i3].y);
                triRef.points[4]=new Vector2d(triRef.vertices[i4].x,triRef.vertices[i4].y);
            }
            else if((x>=y)&&(x>=z))
            {
                triRef.points[1]=new Vector2d(triRef.vertices[i1].z,triRef.vertices[i1].y);
                triRef.points[2]=new Vector2d(triRef.vertices[i2].z,triRef.vertices[i2].y);
                triRef.points[3]=new Vector2d(triRef.vertices[i3].z,triRef.vertices[i3].y);
                triRef.points[4]=new Vector2d(triRef.vertices[i4].z,triRef.vertices[i4].y);
            }
            else
            {
                triRef.points[1]=new Vector2d(triRef.vertices[i1].x,triRef.vertices[i1].z);
                triRef.points[2]=new Vector2d(triRef.vertices[i2].x,triRef.vertices[i2].z);
                triRef.points[3]=new Vector2d(triRef.vertices[i3].x,triRef.vertices[i3].z);
                triRef.points[4]=new Vector2d(triRef.vertices[i4].x,triRef.vertices[i4].z);
            }
            triRef.numPoints=5;

            ori2=orientation(triRef,1,2,3);
            ori4=orientation(triRef,1,3,4);

            if(((ori2>0)&&(ori4>0))||((ori2<0)&&(ori4<0)))
            {
                triRef.storeTriangle(ind1,ind2,ind3);
                triRef.storeTriangle(ind1,ind3,ind4);
            }
            else
            {
                triRef.storeTriangle(ind2,ind3,ind4);
                triRef.storeTriangle(ind2,ind4,ind1);
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

    private static void prepareNoHashPnts(Triangulator triRef,int currLoopMin)
    {
        triRef.numVtxList=0;
        triRef.reflexVertices=-1;

        int ind=triRef.loops[currLoopMin];
        int ind1=ind;
        triRef.numReflex=0;
        do
        {
            if(triRef.list[ind1].convex<0)
                insertAfterVtx(triRef,ind1);

            ind1=triRef.list[ind1].next;
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

    private static void insertAfterVtx(Triangulator triRef,int iVtx)
    {
        int size;

        if(triRef.vtxList==null)
        {
            size=Math.max(triRef.numVtxList+1,100);
            triRef.vtxList=new PntNode[size];
        }
        else if(triRef.numVtxList>=triRef.vtxList.length)
        {
            size=Math.max(triRef.numVtxList+1,triRef.vtxList.length+100);
            PntNode old[]=triRef.vtxList;
            triRef.vtxList=new PntNode[size];
            System.arraycopy(old,0,triRef.vtxList,0,old.length);
        }

        triRef.vtxList[triRef.numVtxList]=new PntNode(iVtx,triRef.reflexVertices);
        triRef.reflexVertices=triRef.numVtxList;
        triRef.numVtxList++;
        triRef.numReflex++;
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

    private static void projectFace(Triangulator triRef,int loopMin,int loopMax)
    {
        Vector3d normal,nr;
        int i,j;
        double d;

        normal=new Vector3d();
        nr=new Vector3d();

        determineNormal(triRef,triRef.loops[loopMin],normal);
        j=loopMin+1;
        if(j<loopMax)
        {
            for(i=j;i<loopMax;++i)
            {
                determineNormal(triRef,triRef.loops[i],nr);
                if(dotProduct(normal,nr)<0.0)
                    nr.negate();

                normal=vectorAdd(normal,nr);
            }
            d=lengthL2(normal);
            if(!((d)<=Triangulator.ZERO))
                normal=divScalar(d,normal);
            else
                normal=new Vector3d(0,0,1);
        }

        projectPoints(triRef,loopMin,loopMax,normal);
    }

    private static double dotProduct(Vector3d _u,Vector3d _v)
    {
        return((_u.x*_v.x)+(_u.y*_v.y)+(_u.z*_v.z));
    }

    private static Vector3d divScalar(double scalar,Vector3d u)
    {
        return u.scale(1/scalar);
    }

    private static void determineNormal(Triangulator triRef,int ind,Vector3d normal)
    {
        int ind1=ind;
        int i1=triRef.list[ind1].index;
        int ind0=triRef.list[ind1].prev;
        int i0=triRef.list[ind0].index;
        int ind2=triRef.list[ind1].next;
        int i2=triRef.list[ind2].index;
        Vector3d pq=vectorSub(triRef.vertices[i0],triRef.vertices[i1]);
        Vector3d pr=vectorSub(triRef.vertices[i2],triRef.vertices[i1]);
        Vector3d nr=vectorProduct(pq,pr);
        double d=lengthL2(nr);
        if(!((d)<=Triangulator.ZERO))
            normal=divScalar(d,nr);
        else
            normal=new Vector3d(0,0,0);

        pq=pr;
        ind1=ind2;
        ind2=triRef.list[ind1].next;
        i2=triRef.list[ind2].index;
        while(ind1!=ind)
        {
            pr=vectorSub(triRef.vertices[i2],triRef.vertices[i1]);
            nr=vectorProduct(pq,pr);
            d=lengthL2(nr);
            if(!((d)<=Triangulator.ZERO))
            {
                nr=divScalar(d,nr);
                if(dotProduct(normal,nr)<0.0)
                    nr=nr.negate();
                normal=vectorAdd(normal,nr);
            }
            pq=pr;
            ind1=ind2;
            ind2=triRef.list[ind1].next;
            i2=triRef.list[ind2].index;
        }

        d=lengthL2(normal);
        if(!((d)<=Triangulator.ZERO))
            normal=divScalar(d,normal);
        else
            normal=new Vector3d(0,0,1);
    }

    private static Vector3d vectorProduct(Vector3d _p,Vector3d _q)
    {
        return new Vector3d(
                _p.y*_q.z-_q.y*_p.z,
                _q.x*_p.z-_p.x*_q.z,
                _p.x*_q.y-_q.x*_p.y);
    }

    private static Vector3d vectorAdd(Vector3d _p,Vector3d _q)
    {
        return _p.add(_q);
    }

    private static Vector3d vectorSub(Vector3d _p,Vector3d _q)
    {
        return _p.subtract(_q);
    }

    private static int signEps(double _x,double _eps)
    {
        if(_x>_eps)
            return 1;

        if(_x<-_eps)
            return -1;

        return 0;
    }

    private static void projectPoints(Triangulator triRef,int i1,int i2,Vector3d n3)
    {
        Vector3d vtx=new Vector3d();
        Vector3d n1=new Vector3d();
        Vector3d n2=new Vector3d();

        if((Math.abs(n3.x)>0.1)||(Math.abs(n3.y)>0.1))
            n1=new Vector3d(-n3.y,n3.x,0);
        else
            n1=new Vector3d(n3.z,-n3.x,0);

        double d=lengthL2(n1);
        n1=divScalar(d,n1);
        n2=vectorProduct(n1,n3);
        d=lengthL2(n2);
        n2=divScalar(d,n2);

        Matrix4f matrix=new Matrix4f();
        matrix.m00=n1.x;
        matrix.m01=n1.y;
        matrix.m02=n1.z;
        matrix.m03=0;
        matrix.m10=n2.x;
        matrix.m11=n2.y;
        matrix.m12=n2.z;
        matrix.m13=0;
        matrix.m20=n3.x;
        matrix.m21=n3.y;
        matrix.m22=n3.z;
        matrix.m23=0;
        matrix.m30=0;
        matrix.m31=0;
        matrix.m32=0;
        matrix.m33=1;

        triRef.initPnts(20);
        for(int i=i1;i<i2;++i)
        {
            int ind=triRef.loops[i];
            int ind1=ind;
            int j1=triRef.list[ind1].index;
            vtx=matrix.transform(triRef.vertices[j1]);
            j1=triRef.storePoint(vtx.x,vtx.y);
            triRef.list[ind1].index=j1;
            ind1=triRef.list[ind1].next;
            j1=triRef.list[ind1].index;
            while(ind1!=ind)
            {
                vtx=matrix.transform(triRef.vertices[j1]);
                j1=triRef.storePoint(vtx.x,vtx.y);
                triRef.list[ind1].index=j1;
                ind1=triRef.list[ind1].next;
                j1=triRef.list[ind1].index;
            }
        }
    }

    private static double lengthL2(Vector3d u)
    {
        return Math.sqrt((u.x*u.x)+(u.y*u.y)+(u.z*u.z));
    }

    private static void adjustOrientation(Triangulator triRef,int i1,int i2)
    {
        if(triRef.numLoops>=triRef.maxNumPolyArea)
        {
            triRef.maxNumPolyArea=triRef.numLoops;
            double old[]=triRef.polyArea;
            triRef.polyArea=new double[triRef.maxNumPolyArea];
            if(old!=null)
                System.arraycopy(old,0,triRef.polyArea,0,old.length);
        }

        for(int i=i1;i<i2;++i)
        {
            int ind=triRef.loops[i];
            triRef.polyArea[i]=polygonArea(triRef,ind);
        }

        double area=Math.abs(triRef.polyArea[i1]);
        int outer=i1;
        for(int i=i1+1;i<i2;++i)
            if(area<Math.abs(triRef.polyArea[i]))
            {
                area=Math.abs(triRef.polyArea[i]);
                outer=i;
            }

        if(outer!=i1)
        {
            int ind=triRef.loops[i1];
            triRef.loops[i1]=triRef.loops[outer];
            triRef.loops[outer]=ind;

            area=triRef.polyArea[i1];
            triRef.polyArea[i1]=triRef.polyArea[outer];
            triRef.polyArea[outer]=area;
        }

        if(triRef.polyArea[i1]<0.0)
            triRef.swapLinks(triRef.loops[i1]);

        for(int i=i1+1;i<i2;++i)
            if(triRef.polyArea[i]>0.0)
                triRef.swapLinks(triRef.loops[i]);
    }

    private static double polygonArea(Triangulator triRef,int ind)
    {
        int hook=0;
        double area1=0;

        int ind1=ind;
        int i1=triRef.list[ind1].index;
        int ind2=triRef.list[ind1].next;
        int i2=triRef.list[ind2].index;
        double area=stableDet2D(triRef,hook,i1,i2);

        ind1=ind2;
        i1=i2;
        while(ind1!=ind)
        {
            ind2=triRef.list[ind1].next;
            i2=triRef.list[ind2].index;
            area1=stableDet2D(triRef,hook,i1,i2);
            area+=area1;
            ind1=ind2;
            i1=i2;
        }

        return area;
    }

    private static void determineOrientation(Triangulator triRef,int ind)
    {
        if(polygonArea(triRef,ind)<0.0)
        {
            triRef.swapLinks(ind);
            triRef.ccwLoop=false;
        }
    }
}
