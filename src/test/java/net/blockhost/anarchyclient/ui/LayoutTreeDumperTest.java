package net.blockhost.anarchyclient.ui;

import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.Rivet;
import net.lenni0451.rivet.backend.Backend;
import net.lenni0451.rivet.backend.text.Font;
import net.lenni0451.rivet.backend.text.ShapedText;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.component.container.ScrollContainer;
import net.lenni0451.rivet.input.keyboard.Key;
import net.lenni0451.rivet.layout.absolute.AbsoluteLayout;
import net.lenni0451.rivet.layout.absolute.AbsoluteOptions;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.text.model.TextLine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutTreeDumperTest {

    @Test
    void snapshotIncludesLabelsBoundsAndLayoutIssues() {
        Rivet rivet = new Rivet(new TestBackend(), AbsoluteLayout.INSTANCE, new Size(200, 100));
        DebugContainer panel = new DebugContainer("panel");
        panel.layoutOptions(new AbsoluteOptions(10, 5, 100F, 50F));
        panel.addChild(new DebugComponent("bad").layoutOptions(new AbsoluteOptions(95, 45, 20F, 20F)));
        panel.addChild(new DebugComponent("hidden").layoutOptions(new AbsoluteOptions(0, 0, 0F, 0F)));
        rivet.root().addChild(panel);

        String snapshot = LayoutTreeDumper.snapshot(rivet);

        assertTrue(snapshot.contains("DebugContainer[panel]"));
        assertTrue(snapshot.contains("DebugComponent[bad]"));
        assertTrue(snapshot.contains("DebugComponent[hidden]"));
        assertTrue(snapshot.contains("flags=HIDDEN"));
        assertTrue(snapshot.contains("issues=1"));
        assertTrue(snapshot.contains("outside parent"));
    }

    @Test
    void snapshotSuppressesIssuesInsideHiddenSubtrees() {
        Rivet rivet = new Rivet(new TestBackend(), AbsoluteLayout.INSTANCE, new Size(200, 100));
        DebugContainer panel = new DebugContainer("panel");
        panel.layoutOptions(new AbsoluteOptions(0, 0, 0F, 0F));
        panel.addChild(new DebugComponent("child").layoutOptions(new AbsoluteOptions(0, 28, 62F, 28F)));
        rivet.root().addChild(panel);

        String snapshot = LayoutTreeDumper.snapshot(rivet);

        assertTrue(snapshot.contains("DebugContainer[panel]"));
        assertTrue(snapshot.contains("DebugComponent[child]"));
        assertTrue(snapshot.contains("flags=HIDDEN"));
        assertTrue(snapshot.contains("flags=ANCESTOR_HIDDEN"));
        assertTrue(snapshot.contains("issues=0"));
        assertFalse(snapshot.contains("outside parent"));
    }

    @Test
    void snapshotIncludesScrollContentChildren() {
        Rivet rivet = new Rivet(new TestBackend(), AbsoluteLayout.INSTANCE, new Size(200, 100));
        DebugContainer rows = new DebugContainer("rows");
        rows.addChild(new DebugComponent("visible").layoutOptions(new AbsoluteOptions(0, 0, 100F, 20F)));
        rows.addChild(new DebugComponent("below-fold").layoutOptions(new AbsoluteOptions(0, 60, 100F, 20F)));
        LayoutDebugScrollContainer scroll = new LayoutDebugScrollContainer(rows);
        scroll.layoutOptions(new AbsoluteOptions(0, 0, 100F, 50F));
        scroll.barType().set(ScrollContainer.ScrollBarType.FLOATING);
        rivet.root().addChild(scroll);

        String snapshot = LayoutTreeDumper.snapshot(rivet);

        assertTrue(snapshot.contains("LayoutDebugScrollContainer"));
        assertTrue(snapshot.contains("content:DebugContainer[rows] local=0.0,0.0 100.0x80.0"));
        assertTrue(snapshot.contains("DebugComponent[below-fold]"));
        assertTrue(snapshot.contains("issues=0"));
    }

    private static final class DebugContainer extends Container implements LayoutDebugLabel {

        private final String label;

        private DebugContainer(final String label) {
            super(AbsoluteLayout.INSTANCE);
            this.label = label;
        }

        @Override
        public String layoutDebugLabel() {
            return this.label;
        }
    }

    private static final class DebugComponent extends Component implements LayoutDebugLabel {

        private final String label;

        private DebugComponent(final String label) {
            this.label = label;
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(10, 10);
        }

        @Override
        public String layoutDebugLabel() {
            return this.label;
        }
    }

    private static final class TestBackend implements Backend {

        private final Font font = new TestFont();

        @Override
        public Font font() {
            return this.font;
        }

        @Override
        public String getClipboard() {
            return null;
        }

        @Override
        public void setClipboard(final String clipboard) {
        }

        @Override
        public boolean isKeyDown(final Key key) {
            return false;
        }
    }

    private static final class TestFont implements Font {

        @Override
        public int size() {
            return 9;
        }

        @Override
        public float height() {
            return 9F;
        }

        @Override
        public Font derive(final int size) {
            return this;
        }

        @Override
        public ShapedText shapeText(final String text, final Color color) {
            return new TestShapedText();
        }

        @Override
        public ShapedText shapeText(final TextLine line) {
            return new TestShapedText();
        }
    }

    private static final class TestShapedText implements ShapedText {

        @Override
        public Rectangle visualBounds() {
            return new Rectangle(0, -9, 0, 9);
        }

        @Override
        public Rectangle logicalBounds() {
            return this.visualBounds();
        }

        @Override
        public net.lenni0451.rivet.math.Point cursorPosition(final int index) {
            return new net.lenni0451.rivet.math.Point(0, 0);
        }

        @Override
        public int index(final float x, final float y) {
            return 0;
        }
    }
}
