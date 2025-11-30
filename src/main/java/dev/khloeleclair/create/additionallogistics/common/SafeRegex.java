package dev.khloeleclair.create.additionallogistics.common;

import com.google.common.base.Ascii;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import net.createmod.catnip.data.Glob;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SafeRegex {

    private static final Cache<String, CachedGlobResult> GLOB_CACHE = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(Duration.ofMinutes(30))
            .build();

    private static final Cache<String, CachedRegexResult> REGEX_CACHE = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(Duration.ofMinutes(30))
            .build();

    private static final Cache<ObjectObjectImmutablePair<String,String>,CachedReplacementResult> REPLACEMENT_CACHE = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(Duration.ofMinutes(30))
            .build();


    private record CachedGlobResult(@Nullable String regex, @Nullable PatternSyntaxException error) {}

    private record CachedReplacementResult(@Nullable PatternSyntaxException error) {
        boolean isError() { return this.error != null; }
    }

    public static final class CachedRegexResult {
        @Nullable
        public final Pattern pattern;
        public final int starHeight;
        public final int repetitions;
        public final boolean hasBackref;
        @Nullable
        public final PatternSyntaxException error;

        CachedRegexResult(Pattern pattern, int starHeight, int repetitions, boolean hasBackref) {
            this.pattern = pattern;
            this.starHeight = starHeight;
            this.repetitions = repetitions;
            this.hasBackref = hasBackref;
            this.error = null;
        }

        CachedRegexResult(PatternSyntaxException error) {
            this.pattern = null;
            this.starHeight = 0;
            this.repetitions = 0;
            this.hasBackref = false;
            this.error = error;
        }

        public boolean isError() {
            return this.error != null;
        }
    }

    public static void assertReplacementSafe(@NotNull String regex, @NotNull String replacement) {
        var key = ObjectObjectImmutablePair.of(regex, replacement);
        CachedReplacementResult cached = REPLACEMENT_CACHE.getIfPresent(key);

        if (cached == null) {
            try {
                var rcache = processRegex(regex);
                if (rcache.isError())
                    throw rcache.error;

                var pattern = rcache.pattern;
                var matcher = pattern.matcher("");

                int groups = matcher.groupCount();
                Set<String> named = pattern.namedGroups().keySet();

                int pos = 0;
                while (pos < replacement.length()) {
                    char next = replacement.charAt(pos);
                    if (next == '\\') {
                        pos++;
                        if (pos == replacement.length())
                            throw new PatternSyntaxException("character to be escaped is missing", replacement, pos);

                        pos++;

                    } else if (next == '$') {
                        pos++;

                        if (pos == replacement.length())
                            throw new PatternSyntaxException("Illegal group reference: group index is missing", replacement, pos);

                        next = replacement.charAt(pos);
                        if (next == '{') {
                            // named group
                            pos++;
                            int begin = pos;
                            while(pos < replacement.length()) {
                                next = replacement.charAt(pos);
                                if (Ascii.isLowerCase(next) || Ascii.isUpperCase(next) || (next >= '0' && next <= '9')) {
                                    pos++;
                                } else
                                    break;
                            }

                            if (begin == pos)
                                throw new PatternSyntaxException("named capturing group has 0 length name", replacement, pos);

                            if (next != '}')
                                throw new PatternSyntaxException("named capturing group is missing trailing '}'", replacement, pos);

                            String group = replacement.substring(begin, pos);
                            if (! named.contains(group))
                                throw new PatternSyntaxException("Group with name {" + group + "} does not exist", replacement, pos);

                            pos++;

                        } else {
                            // Numbered group
                            int num = next - '0';
                            if (num < 0 || num > 9)
                                throw new PatternSyntaxException("Illegal group reference", replacement, pos);
                            if (num > groups)
                                throw new PatternSyntaxException("Group '" + num + "' does not exist", replacement, pos);

                            pos++;
                            // We don't really care if there's two or more digits, because that won't break things
                            // if things go wrong. We just care about the first one working.
                        }

                    } else
                        pos++;
                }

                cached = new CachedReplacementResult(null);

            } catch (PatternSyntaxException ex) {
                cached = new CachedReplacementResult(ex);
            }

            REPLACEMENT_CACHE.put(key, cached);
        }

        if (cached.isError())
            throw cached.error;
    }

    @NotNull
    private static CachedRegexResult processRegex(@NotNull String regex) {
        CachedRegexResult cached = null;

        if (regex != null) {
            cached = REGEX_CACHE.getIfPresent(regex);
        }

        if (cached == null) {
            try {
                var pattern = Pattern.compile(regex);

                RegexTokenizer.Node node = RegexTokenizer.parse(regex);

                cached = new CachedRegexResult(
                        pattern,
                        node.starHeight(),
                        node.repetitions(),
                        RegexTokenizer.visitNodes(node, x -> x instanceof RegexTokenizer.ReferenceNode)
                );

            } catch (PatternSyntaxException ex) {
                cached = new CachedRegexResult(ex);
            }

            REGEX_CACHE.put(regex, cached);
        }

        return cached;
    }

    /// Assert that the provided regular expression is valid and probably safe to run. This checks that the regular
    /// expression has valid syntax, that its star height does not exceed the provided value, and that the number
    /// of repetitions does not exceed the provided value. It also ensures backreferences are not present
    /// unless they're allowed.
    ///
    /// Throws a PatternSyntaxException if a problem is detected.
    public static CachedRegexResult assertSafe(@NotNull String regex, int starHeightLimit, int repetitionLimit, boolean allowBackreference) throws PatternSyntaxException {
        CachedRegexResult cached = processRegex(regex);

        if (cached.isError())
            throw cached.error;

        if (cached.starHeight > starHeightLimit)
            throw new PatternSyntaxException("Unsafe regex: star height (" + cached.starHeight + ") exceeds limit (" + starHeightLimit + ")", regex, 0);

        if (cached.repetitions > repetitionLimit)
            throw new PatternSyntaxException("Unsafe regex: potential repetitions (" + cached.repetitions + ") exceed limit (" + repetitionLimit + ")", regex, 0);

        if (!allowBackreference && cached.hasBackref)
            throw new PatternSyntaxException("Unsafe regex: usage of backref", regex, 0);

        return cached;
    }

    public static boolean isSafe(String regex, int starHeightLimit, int repetitionLimit, boolean allowBackreference) {

        try {
            assertSafe(regex, starHeightLimit, repetitionLimit, allowBackreference);
        } catch(PatternSyntaxException ex) {
            // TODO: Log?
            return false;
        }

        return true;

    }

    private static String cachedToRegexPattern(String address) {
        CachedGlobResult cached = null;

        if (address != null) {
            cached = GLOB_CACHE.getIfPresent(address);
        }

        if (cached == null) {
            try {
                cached = new CachedGlobResult(Glob.toRegexPattern(address), null);

            } catch (PatternSyntaxException ex) {
                cached = new CachedGlobResult(null, ex);
            }

            GLOB_CACHE.put(address, cached);
        }

        return cached.regex;
    }

    /// An optimized version of PackageItem.matchAddress that uses caching and optimized logic.
    public static boolean matchAddress(String boxAddress, String address) {
        if (address.isBlank())
            return boxAddress.isBlank();
        if (address.equals("*") || boxAddress.equals("*"))
            return true;

        try {
            var info = processRegex(cachedToRegexPattern(address));
            if (info.isError())
                throw info.error;
            else if (info.pattern != null && info.pattern.matcher(boxAddress).matches())
                return true;
        } catch(PatternSyntaxException ignore) {

        }

        try {
            var info = processRegex(cachedToRegexPattern(boxAddress));
            if (info.isError())
                throw info.error;
            else if (info.pattern != null && info.pattern.matcher(address).matches())
                return true;
        } catch(PatternSyntaxException ignore) {

        }

        return false;
    }

}
