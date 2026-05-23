package com.example.server.utils;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SurrogateUtils {

    public static boolean isValidString(String str) {
        if (str == null || str.isEmpty()) {
            return true;
        }

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            if (Character.isHighSurrogate(c)) {
                if (i + 1 >= str.length() || !Character.isLowSurrogate(str.charAt(i + 1))) {
                    log.warn("Found orphan high surrogate at position {}", i);
                    return false;
                }
                i++;
            } else if (Character.isLowSurrogate(c)) {
                log.warn("Found orphan low surrogate at position {}", i);
                return false;
            }
        }

        return true;
    }

    public static String cleanSurrogates(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            if (Character.isHighSurrogate(c)) {
                if (i + 1 < str.length() && Character.isLowSurrogate(str.charAt(i + 1))) {
                    result.append(c);
                    result.append(str.charAt(i + 1));
                    i++;
                } else {
                    log.warn("Replacing orphan high surrogate with replacement character");
                    result.append('\uFFFD');
                }
            } else if (Character.isLowSurrogate(c)) {
                log.warn("Replacing orphan low surrogate with replacement character");
                result.append('\uFFFD');
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    public static int countCodePoints(String str) {
        if (str == null || str.isEmpty()) {
            return 0;
        }
        return str.codePointCount(0, str.length());
    }

    public static boolean containsSurrogatePairs(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        for (int i = 0; i < str.length(); i++) {
            if (Character.isHighSurrogate(str.charAt(i))) {
                return true;
            }
        }

        return false;
    }

    public static List<Integer> getCodePoints(String str) {
        List<Integer> codePoints = new ArrayList<>();

        if (str == null || str.isEmpty()) {
            return codePoints;
        }

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            if (Character.isHighSurrogate(c) && i + 1 < str.length() && Character.isLowSurrogate(str.charAt(i + 1))) {
                codePoints.add(Character.toCodePoint(c, str.charAt(i + 1)));
                i++;
            } else {
                codePoints.add((int) c);
            }
        }

        return codePoints;
    }
}