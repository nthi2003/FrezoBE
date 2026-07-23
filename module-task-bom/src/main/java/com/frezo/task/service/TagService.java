package com.frezo.task.service;

import com.frezo.task.dto.request.TagRequest;
import com.frezo.task.dto.response.TagResponse;

import java.util.List;

public interface TagService {
    TagResponse add(TagRequest request);

    TagResponse edit(String id, TagRequest request);

    Void delete(String id);

    List<TagResponse> findAll(String category);
}
