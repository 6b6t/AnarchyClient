package net.lenni0451.rivet.input.keyboard;

import lombok.With;
import lombok.experimental.WithBy;

import java.util.Set;

@With
@WithBy
public record KeyEvent(Key key, Set<ModifierKey> modifiers) {
}
