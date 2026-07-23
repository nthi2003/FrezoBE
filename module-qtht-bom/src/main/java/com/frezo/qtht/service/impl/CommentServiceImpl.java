package com.frezo.qtht.service.impl;

import com.frezo.auth.entity.User;
import com.frezo.auth.repository.UserRepository;
import com.frezo.common.comment.MentionParser;
import com.frezo.common.entity.Comment;
import com.frezo.common.entity.CommentAttachment;
import com.frezo.common.entity.CommentMention;
import com.frezo.common.event.MentionEvent;
import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.common.helper.SystemUtils;
import com.frezo.common.repository.CommentAttachmentRepository;
import com.frezo.common.repository.CommentMentionRepository;
import com.frezo.common.repository.CommentRepository;
import com.frezo.common.response.FePage;
import com.frezo.common.service.MinioService;
import com.frezo.qtht.dto.comment.CommentAttachmentDto;
import com.frezo.qtht.dto.comment.CommentAttachmentInput;
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
import org.springframework.web.multipart.MultipartFile;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    /** 0 = root; reply tối đa tới depth này (5 lớp tổng, khớp FE MAX_COMMENT_DEPTH). */
    private static final int MAX_COMMENT_DEPTH = 4;
    private static final long MAX_ATTACH_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final CommentRepository commentRepository;
    private final CommentAttachmentRepository attachmentRepository;
    private final CommentMentionRepository mentionRepository;
    private final UserRepository userRepository;
    private final MinioService minioService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public FePage<CommentDto> list(String subjectType, String subjectId, int page, int size) {
        int p = Math.max(page, 0);
        int s = size <= 0 ? 50 : Math.min(size, 200);
        Page<Comment> result = commentRepository
                .findBySubjectTypeAndSubjectIdAndIsDeletedFalseOrderByCreatedDateAsc(
                        subjectType, subjectId, PageRequest.of(p, s));
        List<Comment> rows = result.getContent();
        Map<String, List<CommentAttachmentDto>> attByComment = loadAttachments(
                rows.stream().map(Comment::getId).toList());
        List<CommentDto> content = rows.stream()
                .map(c -> toDto(c, attByComment.getOrDefault(c.getId(), List.of())))
                .toList();
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
        String content = payload.getContent() == null ? "" : payload.getContent().trim();
        List<CommentAttachmentInput> atts = payload.getAttachments() == null
                ? List.of() : payload.getAttachments();
        if (content.isEmpty() && atts.isEmpty()) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST,
                    "Bình luận cần nội dung hoặc file đính kèm");
        }
        validateParent(payload.getParentId(), payload.getSubjectType(), payload.getSubjectId());
        Comment c = Comment.builder()
                .subjectType(payload.getSubjectType())
                .subjectId(payload.getSubjectId())
                .content(content.isEmpty() ? "(Đính kèm)" : content)
                .authorId(me.getId())
                .authorUsername(me.getUserName())
                .authorName(me.getName() != null ? me.getName() : me.getUserName())
                .authorAvatar(me.getAvatarUrl())
                .parentId(payload.getParentId())
                .isSystem(false)
                .build();
        c.setId(UUID.randomUUID().toString());
        c = commentRepository.save(c);

        List<CommentAttachmentDto> savedAtts = persistAttachments(c.getId(), atts);

        List<String> mentionIds = resolveMentionIds(content, payload.getMentionedUserIds());
        saveMentions(c.getId(), mentionIds);
        if (!mentionIds.isEmpty()) {
            eventPublisher.publishEvent(new MentionEvent(
                    this, c.getId(), c.getSubjectType(), c.getSubjectId(),
                    me.getUserName(), mentionIds, truncate(c.getContent())));
        }
        return toDto(c, savedAtts);
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
        List<CommentAttachmentDto> atts = loadAttachments(List.of(id))
                .getOrDefault(id, List.of());
        return toDto(c, atts);
    }

    @Override
    @Transactional
    public void delete(String id) {
        Comment c = findOwned(id);
        c.softDelete(SystemUtils.getCurrentUsername());
        commentRepository.save(c);
        attachmentRepository.findByCommentIdAndIsDeletedFalseOrderByCreatedDateAsc(id)
                .forEach(a -> {
                    a.softDelete(SystemUtils.getCurrentUsername());
                    attachmentRepository.save(a);
                });
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

    @Override
    public CommentAttachmentDto uploadAttachment(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST, "Chưa chọn file");
        }
        if (file.getSize() > MAX_ATTACH_BYTES) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST,
                    "File vượt quá 10MB");
        }
        String contentType = normalizeContentType(file.getContentType(), file.getOriginalFilename());
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST,
                    "Chỉ hỗ trợ ảnh (jpg/png/gif/webp), PDF hoặc Word (.doc/.docx)");
        }
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String safeName = original.replaceAll("[^a-zA-Z0-9._\\-\\p{L} ]", "_");
        String objectName = "comments/" + SystemUtils.getCurrentUsername() + "/"
                + System.currentTimeMillis() + "_" + safeName;
        String url = minioService.uploadFile(objectName, file);
        return CommentAttachmentDto.builder()
                .id(UUID.randomUUID().toString())
                .url(url)
                .name(original)
                .contentType(contentType)
                .size(file.getSize())
                .objectName(objectName)
                .build();
    }

    /**
     * Cho phép parentId trỏ comment con (đệ quy nhiều lớp), miễn cùng subject
     * và depth comment mới ≤ MAX_COMMENT_DEPTH.
     */
    private void validateParent(String parentId, String subjectType, String subjectId) {
        if (parentId == null || parentId.isBlank()) {
            return;
        }
        Comment parent = commentRepository.findById(parentId)
                .filter(x -> Boolean.FALSE.equals(x.getIsDeleted()))
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Comment cha không tồn tại"));
        if (!Objects.equals(parent.getSubjectType(), subjectType)
                || !Objects.equals(parent.getSubjectId(), subjectId)) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST,
                    "Comment cha không thuộc cùng đối tượng");
        }
        int parentDepth = computeDepth(parent);
        if (parentDepth >= MAX_COMMENT_DEPTH) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST,
                    "Không thể trả lời sâu hơn " + (MAX_COMMENT_DEPTH + 1) + " lớp bình luận");
        }
    }

    private int computeDepth(Comment comment) {
        int depth = 0;
        String walk = comment.getParentId();
        Set<String> seen = new HashSet<>();
        while (walk != null && !walk.isBlank()) {
            if (!seen.add(walk) || depth > MAX_COMMENT_DEPTH) {
                break;
            }
            depth++;
            Comment ancestor = commentRepository.findById(walk).orElse(null);
            if (ancestor == null) {
                break;
            }
            walk = ancestor.getParentId();
        }
        return depth;
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

    private List<CommentAttachmentDto> persistAttachments(String commentId, List<CommentAttachmentInput> inputs) {
        if (inputs == null || inputs.isEmpty()) return List.of();
        List<CommentAttachmentDto> out = new ArrayList<>();
        for (CommentAttachmentInput in : inputs) {
            if (in == null || in.getUrl() == null || in.getUrl().isBlank()) continue;
            CommentAttachment a = CommentAttachment.builder()
                    .commentId(commentId)
                    .fileName(in.getName() != null ? in.getName() : "file")
                    .fileUrl(in.getUrl())
                    .fileType(in.getContentType())
                    .fileSize(in.getSize())
                    .objectName(in.getObjectName())
                    .build();
            a.setId(UUID.randomUUID().toString());
            attachmentRepository.save(a);
            out.add(CommentAttachmentDto.builder()
                    .id(a.getId())
                    .url(a.getFileUrl())
                    .name(a.getFileName())
                    .contentType(a.getFileType())
                    .size(a.getFileSize())
                    .objectName(a.getObjectName())
                    .build());
        }
        return out;
    }

    private Map<String, List<CommentAttachmentDto>> loadAttachments(List<String> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) return Map.of();
        return attachmentRepository.findByCommentIdInAndIsDeletedFalse(commentIds).stream()
                .collect(Collectors.groupingBy(
                        CommentAttachment::getCommentId,
                        Collectors.mapping(a -> CommentAttachmentDto.builder()
                                .id(a.getId())
                                .url(a.getFileUrl())
                                .name(a.getFileName())
                                .contentType(a.getFileType())
                                .size(a.getFileSize())
                                .build(), Collectors.toList())));
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

    private CommentDto toDto(Comment c, List<CommentAttachmentDto> attachments) {
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
                .attachments(attachments == null ? List.of() : attachments)
                .createdAt(c.getCreatedDate() != null ? c.getCreatedDate().format(ISO) : null)
                .updatedAt(c.getUpdatedDate() != null ? c.getUpdatedDate().format(ISO) : null)
                .deleted(Boolean.TRUE.equals(c.getIsDeleted()))
                .isSystem(Boolean.TRUE.equals(c.getIsSystem()))
                .systemAction(c.getSystemAction())
                .build();
    }

    private static String normalizeContentType(String raw, String filename) {
        if (raw != null && !raw.isBlank() && !"application/octet-stream".equalsIgnoreCase(raw)) {
            return raw.toLowerCase(Locale.ROOT).split(";")[0].trim();
        }
        String name = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".doc")) return "application/msword";
        if (name.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        return raw == null ? "application/octet-stream" : raw.toLowerCase(Locale.ROOT);
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() > 120 ? s.substring(0, 120) + "…" : s;
    }
}
