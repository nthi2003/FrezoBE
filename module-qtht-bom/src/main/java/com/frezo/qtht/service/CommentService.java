package com.frezo.qtht.service;

import com.frezo.common.response.FePage;
import com.frezo.qtht.dto.comment.CommentCreatePayload;
import com.frezo.qtht.dto.comment.CommentDto;
import com.frezo.qtht.dto.comment.CommentUpdatePayload;
import com.frezo.qtht.dto.comment.MentionUserDto;

import java.util.List;

public interface CommentService {

    FePage<CommentDto> list(String subjectType, String subjectId, int page, int size);

    CommentDto create(CommentCreatePayload payload);

    CommentDto update(String id, CommentUpdatePayload payload);

    void delete(String id);

    List<MentionUserDto> searchUsers(String q);
}
