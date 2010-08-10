package toxi.geom.mesh;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import toxi.geom.Vec3D;

public class MidpointSubdivision implements SubdivisionStrategy {

    public List<Vec3D> computeSplitPoint(WingedEdge edge) {
        List<Vec3D> mid = new ArrayList<Vec3D>(1);
        mid.add(edge.getMidPoint());
        return mid;
    }

    public Comparator<? super WingedEdge> getEdgeOrdering() {
        return new EdgeLengthComparator();
    }
}
