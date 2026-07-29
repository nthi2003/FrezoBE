package com.frezo.task.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.task.common.TaskErrorCode;
import com.frezo.task.dto.request.TagRequest;
import com.frezo.task.dto.response.TagResponse;
import com.frezo.task.entity.Tag;
import com.frezo.task.mapper.TagMapper;
import com.frezo.task.repository.TagRepository;
import com.frezo.task.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final TagRepository tagRepository;

    @Override
    @Transactional
    public TagResponse add(TagRequest request) {
        validateRequest(request);
        Tag tag = tagMapper.toEntity(request);
        tag.setIsDeleted(false);
        Tag savedTag = tagRepository.save(tag);
        return tagMapper.toResponse(savedTag);
    }

    @Override
    @Transactional
    public TagResponse edit(String id, TagRequest request) {
        Tag exist = findEntityById(id);
        if (!Objects.equals(exist.getCode(), request.getCode())) {
            validateRequest(request);
        }
        tagMapper.updateEntity(request, exist);
        Tag savedTag = tagRepository.save(exist);
        return tagMapper.toResponse(savedTag);
    }

    @Override
    @Transactional
    public Void delete(String id) {
        Tag tag = findEntityById(id);
        tag.setIsDeleted(true);
        tagRepository.save(tag);
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagResponse> findAll(String category) {
        List<Tag> tags;
        if (category != null && !category.isBlank()) {
            tags = tagRepository.findByCategoryAndIsDeletedFalse(category);
        } else {
            tags = tagRepository.findAll();
        }
        return tagMapper.toResponseList(tags);
    }

    private void validateRequest(TagRequest request) {
        if (tagRepository.existsByCode(request.getCode())) {
            throw new AppException(TaskErrorCode.TAG_CODE_EXISTS, request.getCode());
        }
    }

    @Transactional(readOnly = true)
    protected Tag findEntityById(String id) {
        return tagRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(TaskErrorCode.TAG_NOT_FOUND, id));
    }

}
