package net.lenni0451.rivet.input.keyboard;

public enum Key {

    SPACE,
    APOSTROPHE,
    COMMA,
    MINUS,
    PERIOD,
    SLASH,
    KB_0,
    KB_1,
    KB_2,
    KB_3,
    KB_4,
    KB_5,
    KB_6,
    KB_7,
    KB_8,
    KB_9,
    SEMICOLON,
    EQUAL,
    A,
    B,
    C,
    D,
    E,
    F,
    G,
    H,
    I,
    J,
    K,
    L,
    M,
    N,
    O,
    P,
    Q,
    R,
    S,
    T,
    U,
    V,
    W,
    X,
    Y,
    Z,
    LEFT_BRACKET,
    BACKSLASH,
    RIGHT_BRACKET,
    GRAVE_ACCENT,
    ESCAPE,
    ENTER,
    TAB,
    BACKSPACE,
    INSERT,
    DELETE,
    RIGHT,
    LEFT,
    DOWN,
    UP,
    PAGE_UP,
    PAGE_DOWN,
    HOME,
    END,
    CAPS_LOCK,
    SCROLL_LOCK,
    NUM_LOCK,
    PRINT_SCREEN,
    PAUSE,
    F1,
    F2,
    F3,
    F4,
    F5,
    F6,
    F7,
    F8,
    F9,
    F10,
    F11,
    F12,
    F13,
    F14,
    F15,
    F16,
    F17,
    F18,
    F19,
    F20,
    F21,
    F22,
    F23,
    F24,
    KP_0,
    KP_1,
    KP_2,
    KP_3,
    KP_4,
    KP_5,
    KP_6,
    KP_7,
    KP_8,
    KP_9,
    KP_DECIMAL,
    KP_DIVIDE,
    KP_MULTIPLY,
    KP_SUBTRACT,
    KP_ADD,
    KP_ENTER,
    KP_EQUAL,
    LEFT_SHIFT,
    LEFT_CONTROL,
    LEFT_ALT,
    LEFT_SUPER,
    RIGHT_SHIFT,
    RIGHT_CONTROL,
    RIGHT_ALT,
    RIGHT_SUPER,
    MENU;

    public boolean isEquivalent(final Key key) {
        return switch (key) {
            case KB_0, KP_0 -> this == KB_0 || this == KP_0;
            case KB_1, KP_1 -> this == KB_1 || this == KP_1;
            case KB_2, KP_2 -> this == KB_2 || this == KP_2;
            case KB_3, KP_3 -> this == KB_3 || this == KP_3;
            case KB_4, KP_4 -> this == KB_4 || this == KP_4;
            case KB_5, KP_5 -> this == KB_5 || this == KP_5;
            case KB_6, KP_6 -> this == KB_6 || this == KP_6;
            case KB_7, KP_7 -> this == KB_7 || this == KP_7;
            case KB_8, KP_8 -> this == KB_8 || this == KP_8;
            case KB_9, KP_9 -> this == KB_9 || this == KP_9;
            case SLASH, KP_DIVIDE -> this == SLASH || this == KP_DIVIDE;
            case COMMA, KP_DECIMAL -> this == COMMA || this == KP_DECIMAL;
            case MINUS, KP_SUBTRACT -> this == MINUS || this == KP_SUBTRACT;
            case EQUAL, KP_EQUAL -> this == EQUAL || this == KP_EQUAL;
            case ENTER, KP_ENTER -> this == ENTER || this == KP_ENTER;
            case LEFT_SHIFT, RIGHT_SHIFT -> this == LEFT_SHIFT || this == RIGHT_SHIFT;
            case LEFT_CONTROL, RIGHT_CONTROL -> this == LEFT_CONTROL || this == RIGHT_CONTROL;
            case LEFT_ALT, RIGHT_ALT -> this == LEFT_ALT || this == RIGHT_ALT;
            case LEFT_SUPER, RIGHT_SUPER -> this == LEFT_SUPER || this == RIGHT_SUPER;
            default -> this == key;
        };
    }

}
