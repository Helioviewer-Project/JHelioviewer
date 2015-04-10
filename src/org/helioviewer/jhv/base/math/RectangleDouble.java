package org.helioviewer.jhv.base.math;

public final class RectangleDouble {

    private final Vector2d corner;
    private final Vector2d size;

    public RectangleDouble(final double newX, final double newY, final double newWidth, final double newHeight) {
        corner = new Vector2d(newX, newY);
        size = new Vector2d(newWidth, newHeight);
    }

    public RectangleDouble(final double newX, final double newY, final Vector2d newSize) {
        corner = new Vector2d(newX, newY);
        size = newSize;
    }

    public RectangleDouble(final Vector2d newCorner, final double newWidth, final double newHeight) {
        corner = newCorner;
        size = new Vector2d(newHeight, newWidth);
    }

    public RectangleDouble(final Vector2d newCorner, final Vector2d newSize) {
        corner = newCorner;
        size = newSize;
    }

    public double getX() {
        return corner.x;
    }

    public double getY() {
        return corner.y;
    }

    public double getWidth() {
        return size.x;
    }

    public double getHeight() {
        return size.y;
    }

    public Vector2d getLowerLeftCorner() {
        return corner;
    }

    public Vector2d getLowerRightCorner() {
        return corner.add(size.getXVector());
    }

    public Vector2d getUpperLeftCorner() {
        return corner.add(size.getYVector());
    }

    public Vector2d getUpperRightCorner() {
        return corner.add(size);
    }

    public Vector2d getSize() {
        return size;
    }

    public double area() {
        return size.x * size.y;
    }

    public double aspectRatio() {
        return size.x / size.y;
    }

    public boolean isInsideOuterRectangle(final RectangleDouble outer) {
        return getX() >= outer.getX() && getY() >= outer.getY() && getX() + getWidth() <= outer.getX() + outer.getWidth() && getY() + getHeight() <= outer.getY() + outer.getHeight();
    }

    public static boolean isInsideOuterRectangle(final RectangleDouble inner, final RectangleDouble outer) {
        return inner.isInsideOuterRectangle(outer);
    }

    public RectangleDouble cropToOuterRectangle(final RectangleDouble outer) {
        Vector2d newCorner = corner.crop(outer.getLowerLeftCorner(), outer.getUpperRightCorner());
        Vector2d newUpperRight = getUpperRightCorner().crop(outer.getLowerLeftCorner(), outer.getUpperRightCorner());
        return new RectangleDouble(newCorner, newUpperRight.subtract(newCorner));
    }

    public static RectangleDouble cropToOuterRectangle(final RectangleDouble inner, final RectangleDouble outer) {
        return inner.cropToOuterRectangle(outer);
    }

    public RectangleDouble moveAndCropToOuterRectangle(final RectangleDouble outer) {
        Vector2d newSize = size.crop(Vector2d.NULL_VECTOR, outer.size);
        Vector2d croppedCorner = corner.crop(outer.getLowerLeftCorner(), outer.getUpperRightCorner());
        Vector2d newCorner = croppedCorner.subtract(croppedCorner.add(newSize).subtract(outer.getUpperRightCorner()).crop(Vector2d.NULL_VECTOR, Vector2d.POSITIVE_INFINITY_VECTOR));
        return new RectangleDouble(newCorner, newSize);
    }

    public static RectangleDouble moveAndCropToOuterRectangle(final RectangleDouble inner, final RectangleDouble outer) {
        return inner.moveAndCropToOuterRectangle(outer);
    }

    public RectangleDouble expandToAspectRatioKeepingCenter(final double newAspectRatio) {
        Vector2d newSize;
        if (size.x / size.y < newAspectRatio) {
            newSize = new Vector2d(newAspectRatio * size.y, size.y);
        } else {
            newSize = new Vector2d(size.x, size.x / newAspectRatio);
        }
        Vector2d newCorner = corner.add((size.subtract(newSize).scale(0.5)));
        return new RectangleDouble(newCorner, newSize);
    }

    public static RectangleDouble expandToAspectRatioKeepingCenter(final RectangleDouble rectangle, final double newAspectRatio) {
        return rectangle.expandToAspectRatioKeepingCenter(newAspectRatio);
    }

    public RectangleDouble contractToAspectRatioKeepingCenter(final double newAspectRatio) {
        Vector2d newSize;
        if (size.x / size.y < newAspectRatio) {
            newSize = new Vector2d(size.x, size.x / newAspectRatio);
        } else {
            newSize = new Vector2d(newAspectRatio * size.y, size.y);
        }
        Vector2d newCorner = corner.add((size.subtract(newSize).scale(0.5)));
        return new RectangleDouble(newCorner, newSize);
    }

    public static RectangleDouble contractToAspectRatioKeepingCenter(final RectangleDouble rectangle, final double newAspectRatio) {
        return rectangle.contractToAspectRatioKeepingCenter(newAspectRatio);
    }

    public RectangleDouble getBoundingRectangle(final RectangleDouble r) {
        Vector2d newLowerLeftCorner = corner.componentMin(r.getLowerLeftCorner());
        Vector2d newUpperRightCorner = getUpperRightCorner().componentMax(r.getUpperRightCorner());
        return new RectangleDouble(newLowerLeftCorner, newUpperRightCorner.subtract(newLowerLeftCorner));
    }

    public static RectangleDouble getBoundingRectangle(final RectangleDouble r1, final RectangleDouble r2) {
        return r1.getBoundingRectangle(r2);
    }

    public boolean equals(final Object o) {
        if (!(o instanceof RectangleDouble)) {
            return false;
        }
        RectangleDouble r = (RectangleDouble) o;

        return corner.equals(r.corner) && size.equals(r.size);
    }

    /**
     * {@inheritDoc}
     */

    public String toString() {
        return "[Rectangle: Corner: " + corner + ", Size: " + size + "]";
    }

}
