package dev.khloeleclair.create.additionallogistics.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class RegexTokenizer {

    private final String pattern;
    private int pos = 0;

    @Nullable
    private Node parsed;

    public RegexTokenizer(String pattern) {
        this.pattern = Objects.requireNonNull(pattern);
    }

    @NotNull
    public static Node parse(String pattern) throws PatternSyntaxException {
        return new RegexTokenizer(pattern).parse();
    }

    public static boolean visitNodes(Node node, Predicate<Node> predicate) {
        if (predicate.test(node))
            return true;

        var children = node.getChildren();
        if ( children != null )
            return children.stream().anyMatch(x -> visitNodes(x, predicate));

        return false;
    }


    @NotNull
    public Node parse() throws PatternSyntaxException {
        if (parsed == null) {
            parsed = parseAlternation();
            if (!eof())
                throw error("Unexpected character after end of expression");
        }

        return parsed;
    }

    // Helper Methods

    private boolean eof() { return pos >= pattern.length(); }
    private char peek() { return peek(0); }
    private char peek(int ahead) {
        int p = pos + ahead;
        return p < pattern.length() ? pattern.charAt(p) : '\0';
    }
    private char consume() {
        if (eof())
            return '\0';
        return pattern.charAt(pos++);
    }
    private boolean consumeIf(char c) {
        if (!eof() && pattern.charAt(pos) == c) {
            pos++;
            return true;
        }
        return false;
    }

    private String readMatching(Predicate<Character> matcher) {
        int start = pos;
        while(!eof() && matcher.test(peek()))
            pos++;
        return pattern.substring(start, pos);
    }

    private String readMatching(Predicate<Character> matcher, int maxLength) {
        int start = pos;
        while(!eof() && (pos - start < maxLength) && matcher.test(peek()))
            pos++;

        return pattern.substring(start, pos);
    }

    private PatternSyntaxException error(String msg) {
        return new PatternSyntaxException(msg, pattern, pos);
    }

    // Tokenization

    @NotNull
    private Node parseAlternation() throws PatternSyntaxException {
        List<Node> branches = new ArrayList<>();
        branches.add(parseSequence());
        while (consumeIf('|')) {
            branches.add(parseSequence());
        }
        if (branches.size() == 1) return branches.get(0);
        return new AlternationNode(branches);
    }

    private Node parseSequence() throws PatternSyntaxException {
        List<Node> sequence = new ArrayList<>();

        while(!eof()) {
            char c = peek();
            if (c == ')' || c == '|')
                break;
            sequence.add(parseTerm());
        }

        if (sequence.isEmpty())
            return EmptyNode.INSTANCE;
        if (sequence.size() == 1)
            return sequence.get(0);
        return new SequenceNode(sequence);
    }

    private Node parseTerm() throws PatternSyntaxException {
        Node base = parseAtom();
        if (eof()) return base;

        char c = peek();

        if (c == '*' || c == '+' || c == '?') {
            // Simple Quantifiers
            consume();

            int min;
            int max;

            switch(c) {
                case '*':
                    min = 0;
                    max = QuantifierNode.INFINITE;
                    break;
                case '+':
                    min = 1;
                    max = QuantifierNode.INFINITE;
                    break;
                case '?':
                default:
                    min = 0;
                    max = 1;
                    break;
            }

            boolean possessive = consumeIf('+');
            boolean lazy = !possessive && consumeIf('?');

            return new QuantifierNode(base, min, max, lazy, possessive);

        } else if (c == '{') {
            consume();

            String first = readMatching(Character::isDigit);
            if (first.isEmpty())
                throw error("Expected number in quantifier");

            int min = Integer.parseInt(first);
            if (min < 0)
                throw error("Quantifier number out of range");
            int max = min;

            boolean hasComma = consumeIf(',');
            if(hasComma) {
                String second = readMatching(Character::isDigit);
                if (second.isEmpty())
                    max = QuantifierNode.INFINITE;
                else {
                    max = Integer.parseInt(second);
                    if (max < 0)
                        throw error("Quantifier number out of range");
                }
            }

            if (!consumeIf('}'))
                throw error("Unterminated quantifier");

            boolean possessive = consumeIf('+');
            boolean lazy = !possessive && consumeIf('?');

            return new QuantifierNode(base, min, max, lazy, possessive);
        }

        return base;
    }

    private Node parseAtom() throws PatternSyntaxException {
        if (eof())
            throw error("Unexpected end of pattern");

        char c = peek();
        if (c == '.') {
            consume();
            return DotNode.INSTANCE;

        } else if (c == '^' || c == '$') {
            consume();
            return AnchorNode.fromChar(c);

        } else if (c == '\\') {
            return parseEscape();

        } else if (c == '[') {
            return parseCharacterClass();

        } else if (c == '(') {
            return parseGroup();

        } else {
            return parseLiteral();

        }
    }

    private Node parseEscape() throws PatternSyntaxException {
        consume();
        if (eof())
            throw error("Trailing backslash");

        char c = consume();
        String text;

        if (Character.isDigit(c))
            return new ReferenceNode(c);

        switch(c) {
            case 'u':
                // Expect \\uXXXX
                String hex = readMatching(ch -> Character.isDigit(ch) || (ch >= 'a' && ch <= 'f') || (ch >= 'A') && (ch <= 'F'), 4);
                if (hex.length() != 4)
                    throw error("Invalid unicode escape");
                text = "\\u" + hex;
                break;
            default:
                text = "\\" + c;
        }

        return new EscapeNode(text);
    }

    private Node parseLiteral() throws PatternSyntaxException {
        String text = readMatching(ch -> "().|*+?[]{}^$\\".indexOf(ch) == -1);
        if (text.isEmpty())
            return EmptyNode.INSTANCE;
        return new LiteralNode(text);
    }

    private CharacterClassNode parseCharacterClass() throws PatternSyntaxException {
        consume();
        boolean negated = consumeIf('^');
        List<CharacterClassElement> elements = new ArrayList<>();

        while(!eof()) {
            if (peek() == ']') {
                consume();
                return new CharacterClassNode(negated, elements);
            }

            elements.add(parseCharacterClassElement());
        }

        throw error("Unterminated character class");
    }

    private CharacterClassElement parseCharacterClassElement() throws PatternSyntaxException {
        if (peek() == '\\') {
            consume();
            if (eof())
                throw error("Trailing backslash");
            char c = consume();
            return new CharacterLiteralElement(String.valueOf(c));

        } else if (peek() == '[') {
            return parseCharacterClass();

        } else {
            char c = consume();
            if (!eof() && peek() == '-' && peek(1) != ']') {
                // Range a-b
                consume();
                if (eof())
                    throw error("Unterminated character class");

                char b;

                if (peek() == '\\') {
                    Node node = parseEscape();
                    if (!(node instanceof EscapeNode esc))
                        throw error("Expected escape sequence");

                    b = esc.asChar();
                } else
                    b = consume();

                return new CharacterRangeElement(String.valueOf(c), String.valueOf(b));
            }

            return new CharacterLiteralElement(String.valueOf(c));
        }
    }

    private Node parseGroup() throws PatternSyntaxException {
        consume();

        String name = null;
        GroupType type = GroupType.Capturing;

        if (consumeIf('?')) {
            // Extended Group
            char c = peek();

            if (c == ':') {
                consume();
                type = GroupType.NonCapturing;
            } else if (c == '=') {
                consume();
                type = GroupType.PositiveLookahead;
            } else if (c == '!') {
                consume();
                type = GroupType.NegativeLookahead;

            } else if (c == '<') {
                consume();
                c = peek();
                if (c == '=') {
                    consume();
                    type = GroupType.PositiveLookbehind;
                } else if (c == '!') {
                    consume();
                    type = GroupType.NegativeLookbehind;
                } else {
                    name = readMatching(ch -> Character.isLetterOrDigit(ch) || ch == '_');
                    if (!consumeIf('>'))
                        throw error("Unterminated named capturing group");

                    type = GroupType.NamedCapturing;
                }
            } else
                throw error("Unknown group construct");
        }

        Node inner = parseAlternation();
        if (!consumeIf(')'))
            throw error("Expected ')'");

        return new GroupNode(inner, type, name);
    }

    // Node Types

    public sealed interface Node permits AlternationNode, AnchorNode, CharacterClassNode, DotNode, EscapeNode, EmptyNode, GroupNode, LiteralNode, QuantifierNode, ReferenceNode, SequenceNode {
        @Nullable
        default List<Node> getChildren() { return null; }

        default int repetitions() {
            var children = this.getChildren();
            if (children != null)
                return children.stream().mapToInt(Node::repetitions).sum();

            return 1;
        }

        default int starHeight() {
            var children = this.getChildren();
            if (children != null)
                return children.stream().mapToInt(Node::starHeight).max().orElse(0);

            return 0;
        }
    }

    public record AlternationNode(List<Node> branches) implements Node {
        @Override
        public @Nullable List<Node> getChildren() {
            return branches;
        }

        @Override
        public int repetitions() {
            var children = getChildren();
            if (children != null)
                return children.stream().mapToInt(Node::repetitions).max().orElse(1);
            return 1;
        }

        @Override
        public String toString() {
            return branches.stream().map(Object::toString).collect(Collectors.joining("|"));
        }
    }

    public enum AnchorType {
        Start,
        End
    };

    public record AnchorNode(AnchorType type) implements Node {
        public static AnchorNode fromChar(char input) throws PatternSyntaxException {
            return switch (input) {
                case '^' -> new AnchorNode(AnchorType.Start);
                case '$' -> new AnchorNode(AnchorType.End);
                default -> throw new PatternSyntaxException("Invalid anchor character", String.valueOf(input), 0);
            };
        }

        @Override
        public String toString() {
            return type == AnchorType.Start ? "^" : "$";
        }
    }

    public sealed interface CharacterClassElement permits CharacterClassNode, CharacterLiteralElement, CharacterRangeElement {

    }

    public record CharacterLiteralElement(String literal) implements CharacterClassElement {
        @Override
        public String toString() {
            return literal;
        }
    }

    public record CharacterRangeElement(String start, String end) implements CharacterClassElement {
        @Override
        public String toString() {
            return start + "-" + end;
        }
    }

    public record CharacterClassNode(boolean negated, List<CharacterClassElement> elements) implements CharacterClassElement, Node {
        @Override
        public String toString() {
            return "[" + (negated ? "^" : "") + elements.stream().map(Object::toString).collect(Collectors.joining()) + "]";
        }
    }


    public static final class DotNode implements Node {
        public static final DotNode INSTANCE = new DotNode();

        @Override
        public String toString() {
            return ".";
        }
    }

    public record EscapeNode(String text) implements Node {
        @Override
        public String toString() {
            return text;
        }

        public char asChar() {
            if (text.length() == 2 && text.charAt(0) == '\\') {
                char c = text.charAt(1);
                return switch (c) {
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    case 'f' -> '\f';
                    default -> c;
                };
            } else if (text.startsWith("\\u")) {
                String h = text.substring(2);
                int v = Integer.parseInt(h, 16);
                return (char) v;
            }

            throw new PatternSyntaxException("Unsupported escape character", text, 1);
        }
    }

    public static final class EmptyNode implements Node {
        public static final EmptyNode INSTANCE = new EmptyNode();
        @Override
        public String toString() {
            return "";
        }
    }

    public enum GroupType {
        Capturing,
        NonCapturing,
        NamedCapturing,
        PositiveLookahead,
        NegativeLookahead,
        PositiveLookbehind,
        NegativeLookbehind
    }

    public record GroupNode(Node child, GroupType type, String name) implements Node {
        @Override
        public List<Node> getChildren() {
            return List.of(child);
        }

        @Override
        public String toString() {
            String extended = "";
            return "(" + extended + child + ")";
        }
    }

    public record LiteralNode(String literal) implements Node {
        @Override
        public String toString() {
            return literal;
        }
    }

    public record QuantifierNode(Node child, int min, int max, boolean lazy, boolean possessive) implements Node {
        public static final int INFINITE = -1;

        public boolean isInfinite() { return max == INFINITE; }

        @Override
        public int repetitions() {
            if (isInfinite())
                return INFINITE;

            return child.repetitions() * max;
        }

        @Override
        public int starHeight() {
            return child.starHeight() + (isInfinite() ? 1 : 0);
        }

        @Override
        public List<Node> getChildren() {
            return List.of(child);
        }

        @Override
        public String toString() {
            String q;
            if (min == 0 && max == 1)
                q = "?";
            else if (min == 0 && max == INFINITE)
                q = "*";
            else if (min == 1 && max == INFINITE)
                q = "+";
            else {
                StringBuilder sb = new StringBuilder();
                sb.append('{');
                sb.append(min);
                if (max != min) {
                    sb.append(",");
                    if (!isInfinite())
                        sb.append(max);
                }
                sb.append('}');
                q = sb.toString();
            }

            return child + q + (lazy ? "?" : possessive ? "+" : "");
        }
    }

    public record ReferenceNode(char which) implements Node {
        @Override
        public String toString() {
            return "\\" + which;
        }
    }

    public record SequenceNode(List<Node> children) implements Node {
        @Override
        public List<Node> getChildren() {
            return children;
        }

        @Override
        public String toString() {
            return children.stream().map(Object::toString).collect(Collectors.joining(""));
        }
    }

}
