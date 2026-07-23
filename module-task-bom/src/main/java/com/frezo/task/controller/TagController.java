package com.frezo.task.controller;

import com.frezo.task.dto.request.TagRequest;
import com.frezo.task.dto.response.TagResponse;
import com.frezo.task.service.TagService;
import com.frezo.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/task/tag")
@RequiredArgsConstructor
@Tag(name = "Tag API", description = "Tag Management APIs")
public class TagController {

    private final TagService tagService;

    @PostMapping
    @Operation(summary = "Add a new tag")
    public ApiResponse<TagResponse> add(@RequestBody TagRequest request) {
        return ApiResponse.ok(tagService.add(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit an existing tag")
    public ApiResponse<TagResponse> edit(@PathVariable String id, @RequestBody TagRequest request) {
        return ApiResponse.ok(tagService.edit(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a tag by ID")
    public ApiResponse<Void> delete(@PathVariable String id) {
        return ApiResponse.ok(tagService.delete(id));
    }

    @GetMapping
    @Operation(summary = "Get all tags, optionally filtered by category")
    public ApiResponse<List<TagResponse>> findAll(@RequestParam(required = false) String category) {
        return ApiResponse.ok(tagService.findAll(category));
    }
}
