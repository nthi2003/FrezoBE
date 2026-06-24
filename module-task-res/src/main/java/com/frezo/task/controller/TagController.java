package com.frezo.task.controller;

import com.frezo.task.dto.request.TagRequest;
import com.frezo.task.dto.response.TagResponse;
import com.frezo.task.service.TagService;
import com.frezo.util.web.Response;
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
    public Response<TagResponse> add(@RequestBody TagRequest request) {
        return tagService.add(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit an existing tag")
    public Response<TagResponse> edit(@PathVariable String id, @RequestBody TagRequest request) {
        return tagService.edit(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a tag by ID")
    public Response<Void> delete(@PathVariable String id) {
        return tagService.delete(id);
    }

    @GetMapping
    @Operation(summary = "Get all tags, optionally filtered by category")
    public Response<List<TagResponse>> findAll(@RequestParam(required = false) String category) {
        return tagService.findAll(category);
    }
}
