package net.createmod.catnip.data;

import java.util.regex.PatternSyntaxException;

public final class Glob {
    private Glob() {}

    public static String toRegexPattern(String glob) throws PatternSyntaxException {
        if (glob == null) throw new NullPointerException("glob");
        StringBuilder regex = new StringBuilder("^");
        boolean group = false;
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            switch (c) {
                case '*': regex.append(".*"); break;
                case '?': regex.append('.'); break;
                case '{':
                    if (group) throw new PatternSyntaxException("Cannot nest groups", glob, i);
                    group = true; regex.append("(?:"); break;
                case '}':
                    if (!group) regex.append("\\}");
                    else { group = false; regex.append(')'); }
                    break;
                case ',': regex.append(group ? '|' : ','); break;
                case '[': {
                    int end = findClassEnd(glob, i + 1);
                    if (end < 0) throw new PatternSyntaxException("Missing ']'", glob, i);
                    String body = glob.substring(i + 1, end);
                    if (body.isEmpty() || "!".equals(body)) throw new PatternSyntaxException("Empty character class", glob, i);
                    regex.append('[');
                    if (body.charAt(0) == '!') regex.append('^').append(body.substring(1));
                    else regex.append(body.charAt(0) == '^' ? "\\^" + body.substring(1) : body);
                    regex.append(']');
                    i = end;
                    break;
                }
                case '\\':
                    if (++i >= glob.length()) throw new PatternSyntaxException("No character to escape", glob, i - 1);
                    regex.append('\\').append(glob.charAt(i)); break;
                default:
                    if (".^$+()|".indexOf(c) >= 0) regex.append('\\');
                    regex.append(c);
            }
        }
        if (group) throw new PatternSyntaxException("Missing '}'", glob, glob.length() - 1);
        return regex.append('$').toString();
    }

    private static int findClassEnd(String glob, int from) {
        boolean escaped = false;
        for (int i = from; i < glob.length(); i++) {
            char c = glob.charAt(i);
            if (c == ']' && !escaped) return i;
            escaped = c == '\\' && !escaped;
            if (c != '\\') escaped = false;
        }
        return -1;
    }

    public static String toRegexPattern(String glob, String fallback) {
        try { return toRegexPattern(glob); } catch (PatternSyntaxException ignored) { return fallback; }
    }
}
