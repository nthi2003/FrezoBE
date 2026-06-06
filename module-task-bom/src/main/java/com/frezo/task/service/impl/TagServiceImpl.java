package com.frezo.task.service.impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.task.dto.request.TagRequest;
import com.frezo.task.dto.response.TagResponse;
import com.frezo.task.entity.Tag;
import com.frezo.task.mapper.TagMapper;
import com.frezo.task.repository.TagRepository;
import com.frezo.task.service.TagService;
import com.frezo.util.web.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {
    private static final String ERROR_CODE_EXISTS = "TAG_CODE_EXISTS";

    private final TagMapper tagMapper;
    private final TagRepository tagRepository;

    @Override
    @Transactional
    public Response<TagResponse> add(TagRequest request) {
        validateRequest(request);
        Tag tag = tagMapper.toEntity(request);
        tag.setIsDeleted(false);
        Tag savedTag = tagRepository.save(tag);
        return Response.ok(tagMapper.toResponse(savedTag));
    }

    @Override
    @Transactional
    public Response<TagResponse> edit(String id, TagRequest request) {
        Tag exist = findEntityById(id);
        if (!exist.getCode().equals(request.getCode())) {
            validateRequest(request);
        }
        tagMapper.updateEntity(request, exist);
        Tag savedTag = tagRepository.save(exist);
        return Response.ok(tagMapper.toResponse(savedTag));
    }

    @Override
    @Transactional
    public Response<Void> delete(String id) {
        Tag tag = findEntityById(id);
        tag.setIsDeleted(true);
        tagRepository.save(tag);
        return Response.ok();
    }

    @Override
    @Transactional(readOnly = true)
    public Response<List<TagResponse>> findAll() {
        List<Tag> tags = tagRepository.findAll();
        return Response.ok(tagMapper.toResponseList(tags));
    }

    private void validateRequest(TagRequest request) {
        if (tagRepository.existsByCode(request.getCode())) {
            throw new QTHTException(ERROR_CODE_EXISTS, "Mã nhãn đã tồn tại: " + request.getCode());
        }
    }

    @Transactional(readOnly = true)
    protected Tag findEntityById(String id) {
        return tagRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new QTHTException("TAG_NOT_FOUND", "Không tìm thấy nhãn với ID: " + id));
    }

}
