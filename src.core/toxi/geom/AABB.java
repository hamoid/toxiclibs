/*
 * Copyright (c) 2007 Karsten Schmidt
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * http://creativecommons.org/licenses/LGPL/2.1/
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package toxi.geom;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import toxi.geom.mesh.TriangleMesh;
import toxi.math.MathUtils;

/**
 * Axis-aligned bounding box with basic intersection features for Ray, AABB and
 * Sphere classes.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class AABB extends Vec3D implements Shape3D {

    /**
     * Creates a new instance from two vectors specifying opposite corners of
     * the box
     * 
     * @param min
     *            first corner point
     * @param max
     *            second corner point
     * @return new AABB with centre at the half point between the 2 input
     *         vectors
     */
    public static final AABB fromMinMax(Vec3D min, Vec3D max) {
        Vec3D a = Vec3D.min(min, max);
        Vec3D b = Vec3D.max(min, max);
        return new AABB(a.interpolateTo(b, 0.5f), b.sub(a).scaleSelf(0.5f));
    }

    @XmlElement(required = true)
    protected Vec3D extent;

    @XmlTransient
    protected Vec3D min, max;

    public AABB() {
        super();
        setExtent(new Vec3D());
    }

    /**
     * Creates an independent copy of the passed in box
     * 
     * @param box
     */
    public AABB(AABB box) {
        this(box, box.getExtent());
    }

    /**
     * Creates a new instance from centre point and uniform extent in all
     * directions.
     * 
     * @param pos
     * @param extent
     */
    public AABB(ReadonlyVec3D pos, float extent) {
        super(pos);
        setExtent(new Vec3D(extent, extent, extent));
    }

    /**
     * Creates a new instance from centre point and extent
     * 
     * @param pos
     * @param extent
     *            box dimensions (the box will be double the size in each
     *            direction)
     */
    public AABB(ReadonlyVec3D pos, ReadonlyVec3D extent) {
        super(pos);
        setExtent(extent);
    }

    public boolean containsPoint(ReadonlyVec3D p) {
        return p.isInAABB(this);
    }

    public AABB copy() {
        return new AABB(this);
    }

    /**
     * Returns the current box size as new Vec3D instance (updating this vector
     * will NOT update the box size! Use {@link #setExtent(ReadonlyVec3D)} for
     * those purposes)
     * 
     * @return box size
     */
    public final Vec3D getExtent() {
        return extent.copy();
    }

    public final Vec3D getMax() {
        // return this.add(extent);
        return max.copy();
    }

    public final Vec3D getMin() {
        return min.copy();
    }

    public Vec3D getNormalForPoint(ReadonlyVec3D p) {
        p = p.sub(this);
        Vec3D pabs = extent.sub(p.getAbs());
        Vec3D psign = p.getSignum();
        Vec3D normal = Vec3D.X_AXIS.scale(psign.x);
        float minDist = pabs.x;
        if (pabs.y < minDist) {
            minDist = pabs.y;
            normal = Vec3D.Y_AXIS.scale(psign.y);
        }
        if (pabs.z < minDist) {
            normal = Vec3D.Z_AXIS.scale(psign.z);
        }
        return normal;
    }

    /**
     * Adjusts the box size and position such that it includes the given point.
     * 
     * @param p
     *            point to include
     * @return itself
     */
    public AABB includePoint(ReadonlyVec3D p) {
        min.minSelf(p);
        max.maxSelf(p);
        set(min.interpolateTo(max, 0.5f));
        extent.set(max.sub(min).scaleSelf(0.5f));
        return this;
    }

    /**
     * Checks if the box intersects the passed in one.
     * 
     * @param box
     *            box to check
     * @return true, if boxes overlap
     */
    public boolean intersectsBox(AABB box) {
        Vec3D t = box.sub(this);
        return MathUtils.abs(t.x) <= (extent.x + box.extent.x)
                && MathUtils.abs(t.y) <= (extent.y + box.extent.y)
                && MathUtils.abs(t.z) <= (extent.z + box.extent.z);
    }

    /**
     * Calculates intersection with the given ray between a certain distance
     * interval.
     * 
     * Ray-box intersection is using IEEE numerical properties to ensure the
     * test is both robust and efficient, as described in:
     * 
     * Amy Williams, Steve Barrus, R. Keith Morley, and Peter Shirley: "An
     * Efficient and Robust Ray-Box Intersection Algorithm" Journal of graphics
     * tools, 10(1):49-54, 2005
     * 
     * @param ray
     *            incident ray
     * @param minDist
     * @param maxDist
     * @return intersection point on the bounding box (only the first is
     *         returned) or null if no intersection
     */
    public Vec3D intersectsRay(Ray3D ray, float minDist, float maxDist) {
        Vec3D invDir = ray.getDirection().reciprocal();
        boolean signDirX = invDir.x < 0;
        boolean signDirY = invDir.y < 0;
        boolean signDirZ = invDir.z < 0;
        Vec3D bbox = signDirX ? max : min;
        float tmin = (bbox.x - ray.x) * invDir.x;
        bbox = signDirX ? min : max;
        float tmax = (bbox.x - ray.x) * invDir.x;
        bbox = signDirY ? max : min;
        float tymin = (bbox.y - ray.y) * invDir.y;
        bbox = signDirY ? min : max;
        float tymax = (bbox.y - ray.y) * invDir.y;
        if ((tmin > tymax) || (tymin > tmax)) {
            return null;
        }
        if (tymin > tmin) {
            tmin = tymin;
        }
        if (tymax < tmax) {
            tmax = tymax;
        }
        bbox = signDirZ ? max : min;
        float tzmin = (bbox.z - ray.z) * invDir.z;
        bbox = signDirZ ? min : max;
        float tzmax = (bbox.z - ray.z) * invDir.z;
        if ((tmin > tzmax) || (tzmin > tmax)) {
            return null;
        }
        if (tzmin > tmin) {
            tmin = tzmin;
        }
        if (tzmax < tmax) {
            tmax = tzmax;
        }
        if ((tmin < maxDist) && (tmax > minDist)) {
            return ray.getPointAtDistance(tmin);
        }
        return null;
    }

    public boolean intersectsSphere(Sphere s) {
        return intersectsSphere(s, s.radius);
    }

    /**
     * @param c
     *            sphere centre
     * @param r
     *            sphere radius
     * @return true, if AABB intersects with sphere
     */
    public boolean intersectsSphere(Vec3D c, float r) {
        float s, d = 0;
        // find the square of the distance
        // from the sphere to the box
        if (c.x < min.x) {
            s = c.x - min.x;
            d = s * s;
        } else if (c.x > max.x) {
            s = c.x - max.x;
            d += s * s;
        }

        if (c.y < min.y) {
            s = c.y - min.y;
            d += s * s;
        } else if (c.y > max.y) {
            s = c.y - max.y;
            d += s * s;
        }

        if (c.z < min.z) {
            s = c.z - min.z;
            d += s * s;
        } else if (c.z > max.z) {
            s = c.z - max.z;
            d += s * s;
        }

        return d <= r * r;
    }

    public AABB set(AABB box) {
        extent.set(box.extent);
        return set((ReadonlyVec3D) box);
    }

    /**
     * Updates the position of the box in space and calls
     * {@link #updateBounds()} immediately
     * 
     * @see toxi.geom.Vec3D#set(float, float, float)
     */
    public Vec3D set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        updateBounds();
        return this;
    }

    /**
     * Updates the position of the box in space and calls
     * {@link #updateBounds()} immediately
     * 
     * @see toxi.geom.Vec3D#set(toxi.geom.Vec3D)
     */
    public AABB set(ReadonlyVec3D v) {
        x = v.x();
        y = v.y();
        z = v.z();
        updateBounds();
        return this;
    }

    /**
     * Updates the size of the box and calls {@link #updateBounds()} immediately
     * 
     * @param extent
     *            new box size
     * @return itself, for method chaining
     */
    public AABB setExtent(ReadonlyVec3D extent) {
        this.extent = extent.copy();
        return updateBounds();
    }

    public TriangleMesh toMesh() {
        return toMesh("box");
    }

    public TriangleMesh toMesh(String name) {
        TriangleMesh mesh = new TriangleMesh(name, 8, 12);
        // front
        Vec3D a = new Vec3D(min.x, max.y, max.z);
        Vec3D b = new Vec3D(max.x, max.y, max.z);
        Vec3D c = new Vec3D(max.x, min.y, max.z);
        Vec3D d = new Vec3D(min.x, min.y, max.z);
        mesh.addFace(a, b, d, null);
        mesh.addFace(b, c, d, null);
        // back
        Vec3D e = new Vec3D(min.x, max.y, min.z);
        Vec3D f = new Vec3D(max.x, max.y, min.z);
        Vec3D g = new Vec3D(max.x, min.y, min.z);
        Vec3D h = new Vec3D(min.x, min.y, min.z);
        mesh.addFace(f, e, g, null);
        mesh.addFace(e, h, g, null);
        // top
        mesh.addFace(e, f, a, null);
        mesh.addFace(f, b, a, null);
        // bottom
        mesh.addFace(g, h, d, null);
        mesh.addFace(g, d, c, null);
        // left
        mesh.addFace(e, a, h, null);
        mesh.addFace(a, d, h, null);
        // right
        mesh.addFace(b, f, g, null);
        mesh.addFace(b, g, c, null);
        return mesh;
    }

    /*
     * (non-Javadoc)
     * 
     * @see toxi.geom.Vec3D#toString()
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("<aabb> pos: ").append(super.toString()).append(" ext: ")
                .append(extent);
        return sb.toString();
    }

    /**
     * Updates the min/max corner points of the box. MUST be called after moving
     * the box in space by manipulating the public x,y,z coordinates directly.
     * 
     * @return itself
     */
    public final AABB updateBounds() {
        // this is check is necessary for the constructor
        if (extent != null) {
            this.min = this.sub(extent);
            this.max = this.add(extent);
        }
        return this;
    }
}