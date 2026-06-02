package net.lenni0451.rivet.component;

import net.lenni0451.rivet.math.Size;

public interface Parent {

    void requestLayoutRecalculation();

    Size contentSize();

}
