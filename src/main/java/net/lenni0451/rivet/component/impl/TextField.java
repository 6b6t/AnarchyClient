package net.lenni0451.rivet.component.impl;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.lenni0451.commons.animation.Animation;
import net.lenni0451.commons.animation.AnimationMode;
import net.lenni0451.commons.animation.EasingBehavior;
import net.lenni0451.commons.animation.easing.EasingFunction;
import net.lenni0451.commons.animation.easing.EasingMode;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.backend.text.ShapedText;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.component.ListenerList;
import net.lenni0451.rivet.input.keyboard.CharEvent;
import net.lenni0451.rivet.input.keyboard.Key;
import net.lenni0451.rivet.input.keyboard.KeyEvent;
import net.lenni0451.rivet.input.keyboard.ModifierKey;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import net.lenni0451.rivet.input.mouse.MouseMoveEvent;
import net.lenni0451.rivet.math.Padding;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.text.model.TextOrigin;
import net.lenni0451.rivet.theme.Theme;
import net.lenni0451.rivet.theme.ThemeOption;

import java.util.function.Consumer;

@Accessors(fluent = true, chain = true, makeFinal = true)
public class TextField extends Component {

    private final StringBuffer text = new StringBuffer();
    @Getter
    private final ListenerList<Consumer<String>> valueChangeListener = new ListenerList<>();
    @Getter
    private int cursor = 0;
    @Getter
    private int selection = 0;
    private boolean focused;

    private final Animation cursorAnimation = new Animation(AnimationMode.LOOP)
            .frame(EasingFunction.SINE, EasingMode.EASE_OUT, 1, 1, 250, EasingBehavior.KEEP)
            .frame(EasingFunction.SINE, EasingMode.EASE_OUT, 1, 0, 500, EasingBehavior.KEEP)
            .frame(EasingFunction.SINE, EasingMode.EASE_OUT, 0, 1, 500, EasingBehavior.KEEP)
            .start();
    private ShapedText shapedText;
    private boolean selecting = false;
    private float scrollX = 0;
    private int clickCount;
    private long lastClick;

    @Getter
    private final ThemeOption<Color> backgroundColor;
    @Getter
    private final ThemeOption<Color> outlineColor;
    @Getter
    private final ThemeOption<Color> focusedOutlineColor;
    @Getter
    private final ThemeOption<Color> selectionColor;
    @Getter
    private final ThemeOption<Color> cursorColor;
    @Getter
    private final ThemeOption<Float> cursorWidth;
    @Getter
    private final ThemeOption<Float> outlineWidth;
    @Getter
    private final ThemeOption<Float> cornerRadius;
    @Getter
    private final ThemeOption<Padding> innerPadding;

    public TextField() {
        this("");
    }

    public TextField(final String text) {
        this.text(text);

        this.backgroundColor = new ThemeOption<>(this, Theme.TEXT_FIELD_BACKGROUND_COLOR);
        this.outlineColor = new ThemeOption<>(this, Theme.TEXT_FIELD_OUTLINE_COLOR);
        this.focusedOutlineColor = new ThemeOption<>(this, Theme.TEXT_FIELD_FOCUSED_OUTLINE_COLOR);
        this.selectionColor = new ThemeOption<>(this, Theme.TEXT_FIELD_SELECTION_COLOR);
        this.cursorColor = new ThemeOption<>(this, Theme.TEXT_FIELD_CURSOR_COLOR);
        this.cursorWidth = new ThemeOption<>(this, Theme.TEXT_FIELD_CURSOR_WIDTH);
        this.outlineWidth = new ThemeOption<>(this, Theme.TEXT_FIELD_OUTLINE_WIDTH);
        this.cornerRadius = new ThemeOption<>(this, Theme.TEXT_FIELD_CORNER_RADIUS);
        this.innerPadding = new ThemeOption<>(this, Theme.TEXT_FIELD_INNER_PADDING);
    }

    public String text() {
        return this.text.toString();
    }

    public TextField text(final String text) {
        this.text.setLength(0);
        this.text.append(text);
        this.cursor = Math.min(this.cursor, this.text.length());
        this.selection = Math.min(this.selection, this.text.length());
        if (this.rivet() != null) {
            this.updateShapedText();
        }
        this.onTextChange();
        return this;
    }

    private void updateShapedText() {
        this.shapedText = this.rivet().backend().shapeText(this.text.toString(), this.rivet().theme().get(Theme.TEXT_COLOR));
    }

    @Override
    protected void onComponentAdded() {
        this.updateShapedText();
    }

    @Override
    protected void onComponentRemoved() {
        this.focused = false;
        this.selecting = false;
        this.clickCount = 0;
    }

    @Override
    protected void onComponentFocusGained() {
        this.focused = true;
    }

    @Override
    protected void onComponentFocusLost() {
        this.focused = false;
    }

    @Override
    protected boolean onComponentKeyDown(final KeyEvent event) {
        boolean shift = event.modifiers().contains(ModifierKey.SHIFT);
        boolean ctrl = event.modifiers().contains(ModifierKey.CONTROL);

        if (event.key().isEquivalent(Key.LEFT)) {
            if (ctrl) {
                this.cursor = this.findWordStart(this.cursor);
            } else {
                this.cursor = Math.max(0, this.cursor - 1);
            }
            if (!shift) this.selection = this.cursor;
        } else if (event.key().isEquivalent(Key.RIGHT)) {
            if (ctrl) {
                this.cursor = this.findWordEnd(this.cursor);
            } else {
                this.cursor = Math.min(this.text.length(), this.cursor + 1);
            }
            if (!shift) this.selection = this.cursor;
        } else if (event.key().isEquivalent(Key.HOME)) {
            this.cursor = 0;
            if (!shift) this.selection = this.cursor;
        } else if (event.key().isEquivalent(Key.END)) {
            this.cursor = this.text.length();
            if (!shift) this.selection = this.cursor;
        } else if (event.key().isEquivalent(Key.BACKSPACE)) {
            if (this.cursor != this.selection) {
                this.deleteSelection();
            } else if (ctrl) {
                this.selection = this.findWordStart(this.cursor);
                this.deleteSelection();
            } else if (this.cursor > 0) {
                this.text.deleteCharAt(this.cursor - 1);
                this.cursor--;
                this.selection = this.cursor;
                this.updateShapedText();
                this.onTextChange();
            }
        } else if (event.key().isEquivalent(Key.DELETE)) {
            if (this.cursor != this.selection) {
                this.deleteSelection();
            } else if (ctrl) {
                this.selection = this.findWordEnd(this.cursor);
                this.deleteSelection();
            } else if (this.cursor < this.text.length()) {
                this.text.deleteCharAt(this.cursor);
                this.updateShapedText();
                this.onTextChange();
            }
        } else if (ctrl && event.key().isEquivalent(Key.A)) {
            this.selection = 0;
            this.cursor = this.text.length();
        } else if (ctrl && event.key().isEquivalent(Key.C)) {
            this.copy();
        } else if (ctrl && event.key().isEquivalent(Key.V)) {
            this.paste();
        } else if (ctrl && event.key().isEquivalent(Key.X)) {
            this.copy();
            this.deleteSelection();
        }
        this.cursorAnimation.reset().start();
        return true;
    }

    @Override
    protected boolean onComponentCharTyped(final CharEvent event) {
        if (event.character() < 32 || event.character() == 127) return false;
        this.deleteSelection();
        this.text.insert(this.cursor, event.character());
        this.cursor++;
        this.selection = this.cursor;
        this.cursorAnimation.reset().start();
        this.updateShapedText();
        this.onTextChange();
        return true;
    }

    @Override
    protected boolean onComponentMouseDown(final MouseButtonEvent event, final Rectangle bounds) {
        if (!event.button().equals(MouseButton.LEFT)) return false;

        long now = System.currentTimeMillis();
        if (now - this.lastClick < 250) {
            this.clickCount++;
        } else {
            this.clickCount = 1;
        }
        this.lastClick = now;

        this.cursor = this.shapedText.index(event.x() - this.innerPadding.value().left() + this.scrollX, 0);
        if (!event.modifiers().contains(ModifierKey.SHIFT)) {
            this.selection = this.cursor;
        }
        this.selecting = true;

        if (this.clickCount == 2) {
            this.selection = this.findWordStart(this.cursor);
            this.cursor = this.findWordEnd(this.cursor);
        } else if (this.clickCount == 3) {
            this.selection = 0;
            this.cursor = this.text.length();
        } else {
            this.clickCount = 1;
        }
        return true;
    }

    @Override
    protected boolean onComponentMouseUp(final MouseButtonEvent event, final Rectangle bounds) {
        if (event.button().equals(MouseButton.LEFT)) {
            this.selecting = false;
            return true;
        }
        return false;
    }

    @Override
    protected boolean onComponentMouseMove(final MouseMoveEvent event, final Rectangle bounds) {
        if (this.selecting) {
            this.cursor = this.shapedText.index(event.x() - this.innerPadding.value().left() + this.scrollX, 0);
            return true;
        }
        return false;
    }

    @Override
    public void render(final Renderer renderer, final Rectangle bounds) {
        float visibleWidth = bounds.width() - this.innerPadding.value().horizontal();
        float textHeight = this.shapedText.logicalBounds().height();
        this.ensureCursorVisible(visibleWidth);

        renderer.optimizedFillRoundedRect(0, 0, bounds.width(), bounds.height(), this.cornerRadius.value(), this.backgroundColor.value());
        renderer.optimizedOutlineRoundedRect(0, 0, bounds.width(), bounds.height(), this.cornerRadius.value(), this.outlineWidth.value(), this.focused ? this.focusedOutlineColor.value() : this.outlineColor.value());

        renderer.scissor(this.innerPadding.value().left(), this.innerPadding.value().top(), visibleWidth, bounds.height() - this.innerPadding.value().top() - this.innerPadding.value().bottom(), () -> {
            renderer.translate(this.innerPadding.value().left(), this.innerPadding.value().top() + (bounds.height() - this.innerPadding.value().top() - this.innerPadding.value().bottom()) / 2F, () -> {
                renderer.translate(-this.scrollX, 0, () -> {
                    if (this.cursor != this.selection) {
                        float x1 = this.shapedText.cursorPosition(this.cursor).x();
                        float x2 = this.shapedText.cursorPosition(this.selection).x();
                        renderer.fillRect(Math.min(x1, x2), -textHeight / 2F, Math.abs(x1 - x2), textHeight, this.selectionColor.value());
                    }

                    renderer.text(this.shapedText, 0, 0, TextOrigin.Horizontal.VISUAL_LEFT, TextOrigin.Vertical.LOGICAL_CENTER);

                    if (this.focused) {
                        float cursorWidth = this.cursorWidth.value();
                        float cursorX = this.shapedText.cursorPosition(this.cursor).x();
                        renderer.fillRect(cursorX - cursorWidth / 2F, -textHeight / 2F, cursorWidth, textHeight, this.cursorColor.value().withAlphaF(this.cursorAnimation.getValue()));
                    }
                });
            });
        });
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        float textHeight = this.rivet().backend().getTextHeight();
        return new Size(
                textHeight * 10 + this.innerPadding.value().horizontal(),
                textHeight + this.innerPadding.value().vertical()
        );
    }

    private void ensureCursorVisible(final float visibleWidth) {
        float cursorX = this.shapedText.cursorPosition(this.cursor).x();
        if (cursorX < this.scrollX) {
            this.scrollX = cursorX;
        } else if (cursorX > this.scrollX + visibleWidth) {
            this.scrollX = cursorX - visibleWidth;
        }
        this.scrollX = Math.max(0, Math.min(this.scrollX, Math.max(0, this.shapedText.logicalBounds().width() - visibleWidth)));
    }

    private void deleteSelection() {
        if (this.cursor == this.selection) return;
        int start = Math.min(this.cursor, this.selection);
        int end = Math.max(this.cursor, this.selection);
        this.text.delete(start, end);
        this.cursor = start;
        this.selection = start;
        this.updateShapedText();
        this.onTextChange();
    }

    private void copy() {
        if (this.cursor == this.selection) return;
        int start = Math.min(this.cursor, this.selection);
        int end = Math.max(this.cursor, this.selection);
        this.rivet().backend().setClipboard(this.text.substring(start, end));
    }

    private void paste() {
        String clipboard = this.rivet().backend().getClipboard();
        if (clipboard == null || clipboard.isEmpty()) return;
        this.deleteSelection();
        this.text.insert(this.cursor, clipboard);
        this.cursor += clipboard.length();
        this.selection = this.cursor;
        this.updateShapedText();
        this.onTextChange();
    }

    private int findWordStart(int from) {
        int i = Math.max(0, Math.min(this.text.length(), from));
        while (i > 0 && Character.isWhitespace(this.text.charAt(i - 1))) i--;
        while (i > 0 && !Character.isWhitespace(this.text.charAt(i - 1))) i--;
        return i;
    }

    private int findWordEnd(int from) {
        int i = Math.max(0, Math.min(this.text.length(), from));
        while (i < this.text.length() && Character.isWhitespace(this.text.charAt(i))) i++;
        while (i < this.text.length() && !Character.isWhitespace(this.text.charAt(i))) i++;
        return i;
    }

    private void onTextChange() {
        this.valueChangeListener.callVoid(c -> c.accept(this.text()));
    }

}
