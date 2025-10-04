package dev.khloeleclair.create.additionallogistics.common;

import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import net.createmod.catnip.data.Glob;
import net.createmod.catnip.data.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public record PatternReplacement(@NotNull Pattern pattern, @NotNull String replacement, boolean stop) {

    static final Pattern NON_CAPTURE_GROUPS = Pattern.compile(Pattern.quote("(?:"));

    public static Pattern compile(@NotNull String input, boolean insensitive) {
        String regex;
        if (input.regionMatches(true, 0, "regex:", 0, 6))
            regex = input.substring(6);

        else {
            regex = Glob.toRegexPattern(input);
            regex = NON_CAPTURE_GROUPS.matcher(regex).replaceAll("(");
            if (regex.startsWith("^") && regex.endsWith("$"))
                regex = regex.substring(1, regex.length() - 1);
        }

        SafeRegex.assertSafe(regex, Config.Common.maxStarHeight.get(), Config.Common.maxRepetitions.get(), Config.Common.allowBackrefs.get());

        return Pattern.compile(regex, insensitive ? Pattern.CASE_INSENSITIVE : 0);
    }

    public static PatternReplacement of(@NotNull String regex) {
        return of(regex, "", false, false);
    }

    public static PatternReplacement of(@NotNull String regex, String replacement) {
        if (replacement == null)
            replacement = "";
        return of(regex, replacement, false, false);
    }

    public static PatternReplacement of(@NotNull String regex, boolean stop) {
        return of(regex, "", stop, false);
    }

    public static PatternReplacement of(@NotNull String regex, String replacement, boolean stop) {
        if (replacement == null)
            replacement = "";
        return of(regex, replacement, stop, false);
    }

    public static PatternReplacement of(@NotNull String regex, boolean stop, boolean insensitive) {
        return of(regex, "", stop, insensitive);
    }

    public static PatternReplacement of(@NotNull String regex, String replacement, boolean stop, boolean insensitive) {
        if (replacement == null)
            replacement = "";

        var pattern = compile(regex, insensitive);

        SafeRegex.assertReplacementSafe(pattern.pattern(), replacement);
        return new PatternReplacement(pattern, replacement, stop);
    }

    @Nullable
    public static PatternReplacement tryOf(@NotNull String regex) {
        try {
            return of(regex);
        } catch(PatternSyntaxException ex) {
            return null;
        }
    }

    @Nullable
    public static PatternReplacement tryOf(@NotNull String regex, @NotNull String replacement) {
        try {
            return of(regex, replacement);
        } catch(PatternSyntaxException ex) {
            CreateAdditionalLogistics.LOGGER.warn("Invalid regular expression: {}", regex, ex);
        }
        return null;
    }

    @Nullable
    public static PatternReplacement tryOf(@NotNull String regex, boolean stop) {
        try {
            return of(regex, stop);
        } catch(PatternSyntaxException ex) {
            return null;
        }
    }

    @Nullable
    public static PatternReplacement tryOf(@NotNull String regex, @NotNull String replacement, boolean stop) {
        try {
            return of(regex, replacement, stop);
        } catch(PatternSyntaxException ex) {
            CreateAdditionalLogistics.LOGGER.warn("Invalid regular expression: {}", regex, ex);
        }
        return null;
    }

    @Nullable
    public static PatternReplacement tryOf(@NotNull String regex, boolean stop, boolean insensitive) {
        try {
            return of(regex, stop, insensitive);
        } catch(PatternSyntaxException ex) {
            return null;
        }
    }

    @Nullable
    public static PatternReplacement tryOf(@NotNull String regex, @NotNull String replacement, boolean stop, boolean insensitive) {
        try {
            return of(regex, replacement, stop, insensitive);
        } catch(PatternSyntaxException ex) {
            CreateAdditionalLogistics.LOGGER.warn("Invalid regular expression: {}", regex, ex);
        }
        return null;
    }

    public String getRegex() {
        return pattern.pattern();
    }

    public Pair<String, Boolean> replace(String input) {
        try {
            var matcher = pattern.matcher(input);
            boolean result = matcher.find();
            if (result) {
                StringBuilder sb = new StringBuilder();
                do {
                    matcher.appendReplacement(sb, replacement);
                    result = matcher.find();
                } while (result);
                matcher.appendTail(sb);
                return Pair.of(sb.toString(), true);
            } else
                return Pair.of(input, false);
        } catch(Exception ex) {
            CreateAdditionalLogistics.LOGGER.warn("Exception processing regular expression replacement for regex '{}' and replacement '{}': ", getRegex(), replacement, ex);
            return Pair.of(input, false);
        }
    }

}
