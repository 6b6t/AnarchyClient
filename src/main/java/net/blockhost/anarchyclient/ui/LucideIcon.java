package net.blockhost.anarchyclient.ui;

enum LucideIcon {
    CHEVRON_DOWN("\uf16f"),
    CHEVRON_RIGHT("\uf171"),
    CIRCLE("\uf17a"),
    CROSSHAIR("\uf1b6"),
    EYE("\uf1d6"),
    GLOBE("\uf211"),
    LAYOUT("\uf23d"),
    MORE_VERTICAL("\uf26e"),
    MOVE("\uf275"),
    PACKAGE("\uf281"),
    REFRESH_CW("\uf2b0"),
    SEARCH("\uf2c5"),
    SETTINGS("\uf2cc"),
    STAR("\uf2f4"),
    USER("\uf32a"),
    X("\uf344"),
    ZAP("\uf349");

    private final String glyph;

    LucideIcon(final String glyph) {
        this.glyph = glyph;
    }

    String glyph() {
        return this.glyph;
    }
}
