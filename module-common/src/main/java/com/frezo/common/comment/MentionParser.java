package com.frezo.common.comment;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stateless helper — parse {@code @username} từ nội dung comment.
 */
public final class MentionParser {

    private static final Pattern MENTION = Pattern.compile("@([A-Za-z0-9._-]+)");

    private MentionParser() {}

    public static List<String> parseUsernames(String content) {
        if (content == null || content.isBlank()) return List.of();
        Set<String> found = new LinkedHashSet<>();
        Matcher m = MENTION.matcher(content);
        while (m.find()) {
            found.add(m.group(1));
        }
        return new ArrayList<>(found);
    }
}
