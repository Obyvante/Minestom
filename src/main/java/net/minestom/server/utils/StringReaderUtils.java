package net.minestom.server.utils;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class StringReaderUtils {
    private StringReaderUtils() {
        //no instance
    }

    /**
     * Locate the first unescaped escapable character
     *
     * @param charSequence the sequence to start
     * @param start inclusive start position
     * @param escapable the escapable character to find
     * @param escape escape character
     * @return the index of the first unescaped escapable character or -1 if it doesn't have an end
     */
    public static int nextIndexOfEscapable(CharSequence charSequence, int start, char escapable, char escape) {
        boolean wasEscape = false;
        for (int i = start; i < charSequence.length(); i++) {
            if (wasEscape) {
                wasEscape = false;
            } else {
                final char charAt = charSequence.charAt(i);
                if (charAt == escapable) return i;
                if (charAt == escape) wasEscape = true;
            }
        }
        return -1;
    }

    public static int nextIndexOf(CharSequence charSequence, int start, char c) {
        for (int i = start; i < charSequence.length(); i++) {
            if (charSequence.charAt(i) == c) return i;
        }
        return -1;
    }

    public static int endIndexOfQuotableString(CharSequence charSequence, int start) {
        final char type = charSequence.charAt(start);
        final int offsetStart = start + 1;
        if (type == '\'') {
            return nextIndexOfEscapable(charSequence, offsetStart, '\'', '\\');
        } else if (type == '"') {
            return nextIndexOfEscapable(charSequence, offsetStart, '"', '\\');
        } else {
            int res = nextIndexOf(charSequence, offsetStart, ' ');
            res = res == -1 ? charSequence.length() - 1 : res - 1;
            final int a, b;
            if (((a = nextIndexOf(charSequence, offsetStart, '"')) > -1 && a <= res) ||
                    ((b = nextIndexOf(charSequence, offsetStart, '\'')) > -1 && b <= res)) return -1;
            return res;
        }
    }
}
