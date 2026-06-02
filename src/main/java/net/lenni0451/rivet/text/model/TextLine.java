package net.lenni0451.rivet.text.model;

import lombok.With;
import lombok.experimental.WithBy;

import java.util.List;

@With
@WithBy
public record TextLine(List<TextSection> sections) {

    public TextLine(final TextSection... sections) {
        this(List.of(sections));
    }

    public TextLine(final List<TextSection> sections) {
        this.sections = List.copyOf(sections);
    }

}
