package com.frezo.task.service;

import com.frezo.task.dto.request.TagRequest;
import com.frezo.task.dto.response.TagResponse;
import com.frezo.util.web.Response;

import java.util.List;

public interface TagService {
    Response<TagResponse> add(TagRequest request);

    Response<TagResponse> edit(String id, TagRequest request);

    Response<Void> delete(String id);

    Response<List<TagResponse>> findAll(String category);
}
