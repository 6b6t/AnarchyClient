package net.lenni0451.rivet.text;

import lombok.experimental.UtilityClass;
import net.lenni0451.rivet.backend.Backend;
import net.lenni0451.rivet.backend.text.ShapedText;
import net.lenni0451.rivet.backend.text.ShapedTextBlock;
import net.lenni0451.rivet.text.model.TextBlock;
import net.lenni0451.rivet.text.model.TextLine;
import net.lenni0451.rivet.text.model.TextSection;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class TextWrapper {

    public static ShapedTextBlock wrapLine(final Backend backend, final TextLine line, final float maxWidth) {
        List<TextLine> lines = new ArrayList<>();
        for (TextLine explicitLine : splitExplicitLines(line)) {
            lines.addAll(wrapSingleLine(backend, explicitLine, maxWidth));
        }
        return backend.shapeText(new TextBlock(lines));
    }

    private static List<TextLine> splitExplicitLines(final TextLine line) {
        List<TextLine> lines = new ArrayList<>();
        List<TextSection> currentLine = new ArrayList<>();
        for (TextSection section : line.sections()) {
            String text = section.text();
            if (text.contains("\n")) {
                String[] parts = text.split("\n", -1);
                for (int i = 0; i < parts.length; i++) {
                    String part = parts[i];
                    if (!part.isEmpty()) {
                        currentLine.add(new TextSection(part, section.format()));
                    }
                    if (i < parts.length - 1) {
                        lines.add(new TextLine(currentLine));
                        currentLine = new ArrayList<>();
                    }
                }
            } else {
                currentLine.add(section);
            }
        }
        lines.add(new TextLine(currentLine));
        return lines;
    }

    private static List<TextLine> wrapSingleLine(final Backend backend, final TextLine line, final float maxWidth) {
        List<Word> words = splitWords(line.sections());
        List<TextLine> lines = new ArrayList<>();
        List<TextSection> currentLine = new ArrayList<>();
        for (int i = 0; i < words.size(); i++) {
            Word word = words.get(i);
            currentLine.addAll(word.sections);
            ShapedText shapedLine = backend.shapeText(new TextLine(currentLine));
            if (shapedLine.visualBounds().width() > maxWidth) {
                for (TextSection ignored : word.sections) {
                    currentLine.remove(currentLine.size() - 1);
                }
                if (currentLine.isEmpty()) {
                    boolean isSpace = word.sections().size() == 1 && word.sections().get(0).text().equals(" ");
                    if (!isSpace) {
                        List<TextLine> splitWord = wrapWord(backend, word, maxWidth);
                        if (splitWord.size() < 2) {
                            throw new IllegalStateException("Word is too long but also isn't at the same time. Please report immediately and include information on how to reproduce this!");
                        }
                        for (int j = 0; j < splitWord.size(); j++) {
                            if (j == splitWord.size() - 1) {
                                currentLine.addAll(splitWord.get(j).sections());
                            } else {
                                lines.add(splitWord.get(j));
                            }
                        }
                    }
                } else {
                    while (!currentLine.isEmpty() && currentLine.get(currentLine.size() - 1).text().equals(" ")) {
                        currentLine.remove(currentLine.size() - 1);
                    }
                    if (!currentLine.isEmpty()) {
                        lines.add(new TextLine(currentLine));
                    }
                    currentLine = new ArrayList<>();
                    i--;
                }
            }
        }
        lines.add(new TextLine(currentLine));
        return lines;
    }

    private static List<Word> splitWords(final List<TextSection> sections) {
        List<Word> words = new ArrayList<>();
        Word currentWord = new Word(new ArrayList<>());
        for (TextSection section : sections) {
            if (section.text().contains(" ")) {
                String[] parts = section.text().split(" ", -1);
                for (int i = 0; i < parts.length; i++) {
                    String part = parts[i];
                    if (!part.isEmpty()) {
                        currentWord.sections.add(new TextSection(part, section.format()));
                    }
                    if (i < parts.length - 1) {
                        if (!currentWord.sections().isEmpty()) {
                            words.add(currentWord);
                            currentWord = new Word(new ArrayList<>());
                        }
                        words.add(new Word(List.of(new TextSection(" ", section.format()))));
                    }
                }
            } else {
                currentWord.sections.add(section);
            }
        }
        if (!currentWord.sections.isEmpty()) {
            words.add(currentWord);
        }
        return words;
    }

    private static List<TextLine> wrapWord(final Backend backend, final Word word, final float maxWidth) {
        List<TextSection> chars = splitWord(word);
        List<TextLine> lines = new ArrayList<>();
        List<TextSection> currentLine = new ArrayList<>();
        for (int i = 0; i < chars.size(); i++) {
            TextSection section = chars.get(i);
            currentLine.add(section);
            ShapedText shapedLine = backend.shapeText(new TextLine(currentLine));
            if (shapedLine.visualBounds().width() > maxWidth) {
                currentLine.remove(currentLine.size() - 1);
                if (currentLine.isEmpty()) {
                    // If not even a single character fits, we just return all characters
                    return chars.stream().map(TextLine::new).toList();
                } else {
                    lines.add(new TextLine(currentLine));
                    currentLine = new ArrayList<>();
                    i--;
                }
            }
        }
        if (!currentLine.isEmpty()) {
            lines.add(new TextLine(currentLine));
        }
        return lines;
    }

    private static List<TextSection> splitWord(final Word word) {
        List<TextSection> chars = new ArrayList<>();
        for (TextSection section : word.sections) {
            for (char c : section.text().toCharArray()) {
                chars.add(new TextSection(String.valueOf(c), section.format()));
            }
        }
        return chars;
    }

    private record Word(List<TextSection> sections) {
    }

}
