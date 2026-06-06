package com.frezo.common.constant;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WebSocketChannels {

    public static final String TOPIC_PREFIX = "/topic";
    public static final String QUEUE_PREFIX = "/queue";
    public static final String USER_PREFIX = "/user";

    public static class Task {
        public static final String ALL = TOPIC_PREFIX + "/tasks";
        public static final String USER_TASK = QUEUE_PREFIX + "/tasks";
    }
    public static class Notify {
        public static final String SYSTEM = TOPIC_PREFIX + "/system-notifications";
        public static final String USER_NOTIF = QUEUE_PREFIX + "/notifications";
        public static final String PROFILE_UPDATE = TOPIC_PREFIX + "/profile-updates";
        public static final String RECORD_NOTICE = TOPIC_PREFIX + "/record-notifications"; // /thong-bao-ho-so
    }

    public static Map<String, List<ChannelInfo>> getChannels() {
        return Map.of(
            "task", List.of(
                new ChannelInfo(Task.ALL, "All tasks updates"),
                new ChannelInfo(Task.USER_TASK, "Individual user tasks", true)
            ),
            "notification", List.of(
                new ChannelInfo(Notify.SYSTEM, "System-wide notifications"),
                new ChannelInfo(Notify.USER_NOTIF, "Personal notifications", true),
                new ChannelInfo(Notify.PROFILE_UPDATE, "Profile changes"),
                new ChannelInfo(Notify.RECORD_NOTICE, "Record/Dossier notifications")
            )
        );
    }

    public record ChannelInfo(String destination, String description, boolean isUserPersonal) {
        public ChannelInfo(String destination, String description) {
            this(destination, description, false);
        }
    }
}
