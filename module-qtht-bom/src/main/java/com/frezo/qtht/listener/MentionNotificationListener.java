package com.frezo.qtht.listener;

import com.frezo.auth.repository.UserRepository;
import com.frezo.common.event.MentionEvent;
import com.frezo.common.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MentionNotificationListener {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @EventListener
    public void onMention(MentionEvent event) {
        try {
            List<String> usernames = new ArrayList<>();
            for (String uid : event.getMentionedUserIds()) {
                userRepository.findById(uid).ifPresent(u -> {
                    if (u.getUserName() != null
                            && !u.getUserName().equalsIgnoreCase(event.getAuthorUsername())) {
                        usernames.add(u.getUserName());
                    }
                });
            }
            notificationService.notifyMany(
                    usernames,
                    "Bạn được nhắc đến trong comment",
                    event.getPreview(),
                    "COMMENT_MENTION",
                    event.getSubjectType(),
                    event.getSubjectId(),
                    mentionDeepLink(event.getSubjectType(), event.getSubjectId()),
                    event.getAuthorUsername(),
                    false);
        } catch (Exception e) {
            log.warn("[Mention] notify failed: {}", e.getMessage());
        }
    }

    private static String mentionDeepLink(String subjectType, String subjectId) {
        if (subjectType == null || subjectId == null) return "/comments";
        return "/comments?subjectType=" + subjectType + "&subjectId=" + subjectId;
    }
}
