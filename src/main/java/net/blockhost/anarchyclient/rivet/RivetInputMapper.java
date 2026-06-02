package net.blockhost.anarchyclient.rivet;

import net.lenni0451.rivet.input.keyboard.Key;
import net.lenni0451.rivet.input.keyboard.KeyEvent;
import net.lenni0451.rivet.input.keyboard.ModifierKey;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;

import java.util.EnumSet;
import java.util.Set;

public final class RivetInputMapper {

    private RivetInputMapper() {
    }

    public static KeyEvent key(final net.minecraft.client.input.KeyEvent event) {
        return new KeyEvent(fromGlfw(event.key()), modifiers(event.modifiers()));
    }

    public static MouseButtonEvent mouse(final net.minecraft.client.input.MouseButtonEvent event) {
        MouseButton button = switch (event.button()) {
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> MouseButton.RIGHT;
            case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> MouseButton.MIDDLE;
            default -> MouseButton.LEFT;
        };
        return new MouseButtonEvent((float) event.x(), (float) event.y(), button, modifiers(event.modifiers()));
    }

    public static Set<ModifierKey> modifiers(final int glfwModifiers) {
        Set<ModifierKey> modifiers = EnumSet.noneOf(ModifierKey.class);
        if ((glfwModifiers & GLFW.GLFW_MOD_SHIFT) != 0) modifiers.add(ModifierKey.SHIFT);
        if ((glfwModifiers & GLFW.GLFW_MOD_CONTROL) != 0) modifiers.add(ModifierKey.CONTROL);
        if ((glfwModifiers & GLFW.GLFW_MOD_ALT) != 0) modifiers.add(ModifierKey.ALT);
        if ((glfwModifiers & GLFW.GLFW_MOD_SUPER) != 0) modifiers.add(ModifierKey.SUPER);
        return modifiers;
    }

    public static Key fromGlfw(final int key) {
        return switch (key) {
            case GLFW.GLFW_KEY_A -> Key.A;
            case GLFW.GLFW_KEY_B -> Key.B;
            case GLFW.GLFW_KEY_C -> Key.C;
            case GLFW.GLFW_KEY_D -> Key.D;
            case GLFW.GLFW_KEY_E -> Key.E;
            case GLFW.GLFW_KEY_F -> Key.F;
            case GLFW.GLFW_KEY_G -> Key.G;
            case GLFW.GLFW_KEY_H -> Key.H;
            case GLFW.GLFW_KEY_I -> Key.I;
            case GLFW.GLFW_KEY_J -> Key.J;
            case GLFW.GLFW_KEY_K -> Key.K;
            case GLFW.GLFW_KEY_L -> Key.L;
            case GLFW.GLFW_KEY_M -> Key.M;
            case GLFW.GLFW_KEY_N -> Key.N;
            case GLFW.GLFW_KEY_O -> Key.O;
            case GLFW.GLFW_KEY_P -> Key.P;
            case GLFW.GLFW_KEY_Q -> Key.Q;
            case GLFW.GLFW_KEY_R -> Key.R;
            case GLFW.GLFW_KEY_S -> Key.S;
            case GLFW.GLFW_KEY_T -> Key.T;
            case GLFW.GLFW_KEY_U -> Key.U;
            case GLFW.GLFW_KEY_V -> Key.V;
            case GLFW.GLFW_KEY_W -> Key.W;
            case GLFW.GLFW_KEY_X -> Key.X;
            case GLFW.GLFW_KEY_Y -> Key.Y;
            case GLFW.GLFW_KEY_Z -> Key.Z;
            case GLFW.GLFW_KEY_ESCAPE -> Key.ESCAPE;
            case GLFW.GLFW_KEY_ENTER -> Key.ENTER;
            case GLFW.GLFW_KEY_TAB -> Key.TAB;
            case GLFW.GLFW_KEY_BACKSPACE -> Key.BACKSPACE;
            case GLFW.GLFW_KEY_LEFT_SHIFT -> Key.LEFT_SHIFT;
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> Key.RIGHT_SHIFT;
            case GLFW.GLFW_KEY_LEFT_CONTROL -> Key.LEFT_CONTROL;
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> Key.RIGHT_CONTROL;
            case GLFW.GLFW_KEY_LEFT_ALT -> Key.LEFT_ALT;
            case GLFW.GLFW_KEY_RIGHT_ALT -> Key.RIGHT_ALT;
            default -> Key.SPACE;
        };
    }

    public static int toGlfw(final Key key) {
        return switch (key) {
            case A -> GLFW.GLFW_KEY_A;
            case B -> GLFW.GLFW_KEY_B;
            case C -> GLFW.GLFW_KEY_C;
            case D -> GLFW.GLFW_KEY_D;
            case E -> GLFW.GLFW_KEY_E;
            case F -> GLFW.GLFW_KEY_F;
            case G -> GLFW.GLFW_KEY_G;
            case H -> GLFW.GLFW_KEY_H;
            case I -> GLFW.GLFW_KEY_I;
            case J -> GLFW.GLFW_KEY_J;
            case K -> GLFW.GLFW_KEY_K;
            case L -> GLFW.GLFW_KEY_L;
            case M -> GLFW.GLFW_KEY_M;
            case N -> GLFW.GLFW_KEY_N;
            case O -> GLFW.GLFW_KEY_O;
            case P -> GLFW.GLFW_KEY_P;
            case Q -> GLFW.GLFW_KEY_Q;
            case R -> GLFW.GLFW_KEY_R;
            case S -> GLFW.GLFW_KEY_S;
            case T -> GLFW.GLFW_KEY_T;
            case U -> GLFW.GLFW_KEY_U;
            case V -> GLFW.GLFW_KEY_V;
            case W -> GLFW.GLFW_KEY_W;
            case X -> GLFW.GLFW_KEY_X;
            case Y -> GLFW.GLFW_KEY_Y;
            case Z -> GLFW.GLFW_KEY_Z;
            case LEFT_SHIFT -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case RIGHT_SHIFT -> GLFW.GLFW_KEY_RIGHT_SHIFT;
            case LEFT_CONTROL -> GLFW.GLFW_KEY_LEFT_CONTROL;
            case RIGHT_CONTROL -> GLFW.GLFW_KEY_RIGHT_CONTROL;
            case LEFT_ALT -> GLFW.GLFW_KEY_LEFT_ALT;
            case RIGHT_ALT -> GLFW.GLFW_KEY_RIGHT_ALT;
            case ESCAPE -> GLFW.GLFW_KEY_ESCAPE;
            case ENTER -> GLFW.GLFW_KEY_ENTER;
            case TAB -> GLFW.GLFW_KEY_TAB;
            case BACKSPACE -> GLFW.GLFW_KEY_BACKSPACE;
            default -> GLFW.GLFW_KEY_SPACE;
        };
    }
}
