package net.lenni0451.rivet.layer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.lenni0451.rivet.component.Parent;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.math.Size;

@Getter
@Setter
@RequiredArgsConstructor
@Accessors(fluent = true, chain = true, makeFinal = true)
public final class Layer implements Parent {

    private final Container container;
    private final LayerBucket bucket;
    private boolean recalculateNextFrame;

    @Override
    public void requestLayoutRecalculation() {
        this.recalculateNextFrame = true;
    }

    @Override
    public Size contentSize() {
        return this.container.contentSize();
    }

}
