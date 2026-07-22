package com.frezo.qtht.service.impl;

import com.frezo.auth.entity.User;
import com.frezo.auth.repository.UserRepository;
import com.frezo.common.comment.MentionParser;
import com.frezo.common.entity.Comment;
import com.frezo.common.entity.CommentMention;
import com.frezo.common.event.MentionEvent;
import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.common.helper.SystemUtils;
import com.frezo.common.repository.CommentMentionRepository;
import com.frezo.common.repository.CommentRepository;
import com.frezo.common.response.FePage;
import com.frezo.qtht.dto.comment.CommentCreatePayload;
import com.frezo.qtht.dto.comment.CommentDto;
import com.frezo.qtht.dto.comment.CommentUpdatePayload;
import com.frezo.qtht.dto.comment.MentionUserDto;
import com.frezo.qtht.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final CommentRepository commentRepository;
    private final CommentMentionRepository mentionRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public FePage<CommentDto> list(String subjectType, String subjectId, int page, int size) {
        int p = Math.max(page, 0);
        int s = size <= 0 ? 20 : size;
        Page<Comment> result = commentRepository
                .findBySubjectTypeAndSubjectIdAndIsDeletedFalseOrderByCreatedDateAsc(
                        subjectType, subjectId, PageRequest.of(p, s));
        List<CommentDto> content = result.getContent().stream().map(this::toDto).toList();
        return FePage.<CommentDto>builder()
                .content(content)
                .totalElements(result.getTotalElements())
                .totalPages(Math.max(1, result.getTotalPages()))
                .number(result.getNumber())
                .size(result.getSize())
                .build();
    }

    @Override
    @Transactional
    public CommentDto create(CommentCreatePayload payload) {
        User me = currentUser();
        Comment c = Comment.builder()
                .subjectType(payload.getSubjectType())
                .subjectId(payload.getSubjectId())
                .content(payload.getContent())
                .authorId(me.getId())
                .authorUsername(me.getUserName())
                .authorName(me.getName() != null ? me.getName() : me.getUserName())
                .authorAvatar(me.getAvatarUrl())
                .parentId(payload.getParentId())
                .isSystem(false)
                .build();
        c.setId(UUID.randomUUID().toString());
        c = commentRepository.save(c);

        List<String> mentionIds = resolveMentionIds(payload.getContent(), payload.getMentionedUserIds());
        saveMentions(c.getId(), mentionIds);
        if (!mentionIds.isEmpty()) {
            eventPublisher.publishEvent(new MentionEvent(
                    this, c.getId(), c.getSubjectType(), c.getSubjectId(),
                    me.getUserName(), mentionIds, truncate(c.getContent())));
        }
        return toDto(c);
    }

    @Override
    @Transactional
    public CommentDto update(String id, CommentUpdatePayload payload) {
        Comment c = findOwned(id);
        c.setContent(payload.getContent());
        commentRepository.save(c);
        mentionRepository.findByCommentIdAndIsDeletedFalse(id).forEach(m -> {
            m.setIsDeleted(true);
            mentionRepository.save(m);
        });
        List<String> mentionIds = resolveMentionIds(payload.getContent(), payload.getMentionedUserIds());
        saveMentions(c.getId(), mentionIds);
        return toDto(c);
    }

    @Override
    @Transactional
    public void delete(String id) {
        Comment c = findOwned(id);
        c.softDelete(SystemUtils.getCurrentUsername());
        commentRepository.save(c);
    }

    @Override
    public List<MentionUserDto> searchUsers(String q) {
        String needle = q == null ? "" : q.trim().toLowerCase();
        return userRepository.findAll().stream()
                .filter(u -> u.getIsDeleted() == null || Boolean.FALSE.equals(u.getIsDeleted()))
                .filter(u -> u.getStatus() == null || u.getStatus() != 0)
                .filter(u -> needle.isEmpty()
                        || (u.getUserName() != null && u.getUserName().toLowerCase().contains(needle))
                        || (u.getName() != null && u.getName().toLowerCase().contains(needle))
                        || (u.getEmail() != null && u.getEmail().toLowerCase().contains(needle)))
                .limit(10)
                .map(u -> MentionUserDto.builder()
                        .id(u.getId())
                        .username(u.getUserName())
                        .fullName(u.getName() != null ? u.getName() : u.getUserName())
                        .avatar(u.getAvatarUrl())
                        .email(u.getEmail())
                        .build())
                .toList();
    }

    private List<String> resolveMentionIds(String content, List<String> explicitIds) {
        Set<String> ids = new LinkedHashSet<>();
        if (explicitIds != null) ids.addAll(explicitIds);
        for (String username : MentionParser.parseUsernames(content)) {
            userRepository.findByUserName(username).ifPresent(u -> ids.add(u.getId()));
        }
        return new ArrayList<>(ids);
    }

    private void saveMentions(String commentId, List<String> userIds) {
        for (String uid : userIds) {
            User u = userRepository.findById(uid).orElse(null);
            CommentMention m = CommentMention.builder()
                    .commentId(commentId)
                    .mentionedUserId(uid)
                    .mentionedUsername(u != null ? u.getUserName() : null)
                    .build();
            m.setId(UUID.randomUUID().toString());
            mentionRepository.save(m);
        }
    }

    private Comment findOwned(String id) {
        Comment c = commentRepository.findById(id)
                .filter(x -> Boolean.FALSE.equals(x.getIsDeleted()))
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Comment không tồn tại"));
        String me = SystemUtils.getCurrentUsername();
        if (me == null || !me.equalsIgnoreCase(c.getAuthorUsername())) {
            throw new AppException(CommonErrorCode.FORBIDDEN, "Chỉ author mới được sửa/xoá");
        }
        return c;
    }

    private User currentUser() {
        String username = SystemUtils.getCurrentUsername();
        return userRepository.findByUserName(username)
                .orElseThrow(() -> new AppException(CommonErrorCode.UNAUTHORIZED, "Chưa đăng nhập"));
    }

    private CommentDto toDto(Comment c) {
        List<String> mentions = mentionRepository.findByCommentIdAndIsDeletedFalse(c.getId()).stream()
                .map(CommentMention::getMentionedUserId)
                .toList();
        return CommentDto.builder()
                .id(c.getId())
                .content(Boolean.TRUE.equals(c.getIsDeleted()) ? "[Đã xoá]" : c.getContent())
                .authorId(c.getAuthorId())
                .authorName(c.getAuthorName())
                .authorAvatar(c.getAuthorAvatar())
                .mentions(mentions)
                .parentId(c.getParentId())
                .createdAt(c.getCreatedDate() != null ? c.getCreatedDate().format(ISO) : null)
                .updatedAt(c.getUpdatedDate() != null ? c.getUpdatedDate().format(ISO) : null)
                .deleted(Boolean.TRUE.equals(c.getIsDeleted()))
                .isSystem(Boolean.TRUE.equals(c.getIsSystem()))
                .systemAction(c.getSystemAction())
                .build();
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() > 120 ? s.substring(0, 120) + "…" : s;
    }
}
