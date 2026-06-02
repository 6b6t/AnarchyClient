package net.lenni0451.rivet.math;

import lombok.With;
import lombok.experimental.WithBy;

@With
@WithBy
public record Rectangle(float x, float y, float width, float height) {

    public static final Rectangle EMPTY = new Rectangle(0, 0, 0, 0);

    public Rectangle(final Size size) {
        this(0, 0, size.width(), size.height());
    }

    public Rectangle(final float x, final float y, final Size size) {
        this(x, y, size.width(), size.height());
    }

    public float maxX() {
        return this.x + this.width;
    }

    public float maxY() {
        return this.y + this.height;
    }

    public Size size() {
        return new Size(this.width, this.height);
    }

    public boolean contains(final float x, final float y) {
        return x >= this.x && x < this.x + this.width && y >= this.y && y < this.y + this.height;
    }

    public Rectangle add(final float x, final float y) {
        if (x == 0 && y == 0) return this;
        return new Rectangle(this.x + x, this.y + y, this.width, this.height);
    }

    public Rectangle offset(final Padding padding) {
        return new Rectangle(this.x + padding.left(), this.y + padding.top(), this.width - padding.horizontal(), this.height - padding.vertical());
    }

}
