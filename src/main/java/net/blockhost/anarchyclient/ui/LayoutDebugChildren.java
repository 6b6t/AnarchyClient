package net.blockhost.anarchyclient.ui;

import java.util.List;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.math.Rectangle;

interface LayoutDebugChildren {

    List<LayoutDebugChild> layoutDebugChildren(Rectangle bounds);

    record LayoutDebugChild(String role, Component component, Rectangle bounds, boolean clippedByParent) {}
}
