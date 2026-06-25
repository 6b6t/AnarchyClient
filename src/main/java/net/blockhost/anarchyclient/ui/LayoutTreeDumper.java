package net.blockhost.anarchyclient.ui;

import net.lenni0451.rivet.Rivet;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.layer.Layer;
import net.lenni0451.rivet.layout.LayoutOptions;
import net.lenni0451.rivet.layout.absolute.AbsoluteLayoutOptions;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class LayoutTreeDumper {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT);
    private static final float EPSILON = 0.5F;

    private LayoutTreeDumper() {
    }

    static Path writeSnapshot(final Rivet rivet, final Path directory) throws IOException {
        Files.createDirectories(directory);
        Path file = directory.resolve("layout-" + FILE_TIME.format(LocalDateTime.now()) + ".txt");
        Files.writeString(file, snapshot(rivet), StandardCharsets.UTF_8);
        return file;
    }

    static String snapshot(final Rivet rivet) {
        rivet.recalculateNextFrame();
        rivet.render();

        StringBuilder builder = new StringBuilder(8192);
        Size scaledSize = rivet.scaledSize();
        builder.append("AnarchyClient layout snapshot\n");
        builder.append("window=").append(format(rivet.size())).append('\n');
        builder.append("scaled=").append(format(scaledSize)).append('\n');
        builder.append("scale=").append(format(rivet.scale())).append('\n');
        builder.append("snap=").append(rivet.snapToInteger()).append('\n');
        builder.append("layers=").append(rivet.layers().size()).append("\n\n");

        List<LayoutIssue> issues = new ArrayList<>();
        List<Layer> layers = rivet.layers();
        for (int index = 0; index < layers.size(); index++) {
            Layer layer = layers.get(index);
            Rectangle bounds = new Rectangle(scaledSize);
            builder.append("layer ").append(index)
                    .append(" bucket=").append(layer.bucket())
                    .append(" recalc=").append(layer.recalculateNextFrame())
                    .append('\n');
            appendComponent(builder, issues, layer.container(), bounds, bounds, 1);
            builder.append('\n');
        }

        builder.append("issues=").append(issues.size()).append('\n');
        for (LayoutIssue issue : issues) {
            builder.append("- ").append(issue.path()).append(": ").append(issue.message()).append('\n');
        }
        return builder.toString();
    }

    private static void appendComponent(final StringBuilder builder, final List<LayoutIssue> issues,
                                        final Component component, final Rectangle localBounds,
                                        final Rectangle absoluteBounds, final int depth) {
        indent(builder, depth);
        String path = componentName(component);
        builder.append(path)
                .append(" local=").append(format(localBounds))
                .append(" abs=").append(format(absoluteBounds))
                .append(" min=").append(format(component.minSize()))
                .append(" max=").append(format(component.maxSize()))
                .append(" interactive=").append(component.interactive());

        if (component instanceof Container container) {
            builder.append(" layout=").append(simpleName(container.layout()))
                    .append(" content=").append(format(container.contentSize()))
                    .append(" children=").append(container.children().size());
        }

        List<String> flags = flags(component, localBounds);
        if (!flags.isEmpty()) {
            builder.append(" flags=").append(String.join(",", flags));
        }
        builder.append('\n');

        LayoutOptions options = component.layoutOptions();
        if (options != null) {
            indent(builder, depth + 1);
            builder.append("options ").append(options).append('\n');
        }

        if (!(component instanceof Container container)) {
            return;
        }

        List<Component> children = container.children();
        for (int index = 0; index < children.size(); index++) {
            Component child = children.get(index);
            Rectangle childBounds = container.childBounds(child);
            Rectangle childAbsoluteBounds = childBounds.add(absoluteBounds.x(), absoluteBounds.y());
            collectIssues(issues, path + "/" + index + ":" + componentName(child),
                    child, childBounds, localBounds.size());
            appendComponent(builder, issues, child, childBounds, childAbsoluteBounds, depth + 1);
        }
    }

    private static List<String> flags(final Component component, final Rectangle bounds) {
        List<String> flags = new ArrayList<>();
        if (isHidden(component, bounds)) {
            flags.add("HIDDEN");
        } else if (bounds.width() <= EPSILON || bounds.height() <= EPSILON) {
            flags.add("ZERO_SIZE");
        }
        if (component instanceof Container container
                && (container.contentSize().width() > bounds.width() + EPSILON
                || container.contentSize().height() > bounds.height() + EPSILON)) {
            flags.add("CONTENT_OVERFLOW");
        }
        return flags;
    }

    private static void collectIssues(final List<LayoutIssue> issues, final String path, final Component component,
                                      final Rectangle bounds, final Size parentSize) {
        if (isHidden(component, bounds)) {
            return;
        }
        if (bounds.width() < -EPSILON || bounds.height() < -EPSILON) {
            issues.add(new LayoutIssue(path, "negative size " + format(bounds)));
        }
        if (bounds.width() <= EPSILON || bounds.height() <= EPSILON) {
            issues.add(new LayoutIssue(path, "zero-size visible component " + format(bounds)));
        }
        if (bounds.x() < -EPSILON || bounds.y() < -EPSILON
                || bounds.maxX() > parentSize.width() + EPSILON
                || bounds.maxY() > parentSize.height() + EPSILON) {
            issues.add(new LayoutIssue(path, "outside parent " + format(bounds) + " parent=" + format(parentSize)));
        }
    }

    private static boolean isHidden(final Component component, final Rectangle bounds) {
        if (bounds.width() != 0F || bounds.height() != 0F) {
            return false;
        }
        return component.layoutOptions() instanceof AbsoluteLayoutOptions options
                && options.width() != null
                && options.height() != null
                && options.width() == 0F
                && options.height() == 0F;
    }

    private static String componentName(final Component component) {
        String name = simpleName(component);
        if (component instanceof LayoutDebugLabel label) {
            String debugLabel = sanitize(label.layoutDebugLabel());
            if (!debugLabel.isBlank()) {
                return name + "[" + debugLabel + "]";
            }
        }
        return name;
    }

    private static String simpleName(final Object value) {
        String name = value.getClass().getSimpleName();
        return name.isBlank() ? value.getClass().getName() : name;
    }

    private static String sanitize(final String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static String format(final Rectangle bounds) {
        return format(bounds.x()) + "," + format(bounds.y()) + " " + format(bounds.width()) + "x" + format(bounds.height());
    }

    private static String format(final Size size) {
        return format(size.width()) + "x" + format(size.height());
    }

    private static String format(final float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static void indent(final StringBuilder builder, final int depth) {
        builder.append("  ".repeat(Math.max(0, depth)));
    }

    private record LayoutIssue(String path, String message) {
    }
}
