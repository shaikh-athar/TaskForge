package com.taskforge.page.controller;

import com.taskforge.page.dto.UpdatePageRequest;
import com.taskforge.page.model.Page;
import com.taskforge.page.service.PageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PageController {

    private final PageService pageService;

    @PostMapping("/projects/{projectIdOrKey}/pages")
    public ResponseEntity<Page> createPage(
            @RequestAttribute("tenantId") UUID tenantId,
            @PathVariable String projectIdOrKey,
            @RequestBody Page page) {
        return ResponseEntity.ok(pageService.createPage(tenantId, projectIdOrKey, page));
    }

    @GetMapping("/projects/{projectIdOrKey}/pages")
    public ResponseEntity<List<Page>> getPages(
            @PathVariable String projectIdOrKey) {
        return ResponseEntity.ok(pageService.getPagesByProject(projectIdOrKey));
    }

    @GetMapping("/pages/{pageId}")
    public ResponseEntity<Page> getPage(@PathVariable UUID pageId) {
        return ResponseEntity.ok(pageService.getPage(pageId));
    }

    @PatchMapping("/pages/{pageId}")
    public ResponseEntity<Page> updatePage(
            @PathVariable UUID pageId,
            @RequestBody UpdatePageRequest request) {
        return ResponseEntity.ok(pageService.updatePage(pageId, request));
    }

    @DeleteMapping("/pages/{pageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePage(@PathVariable UUID pageId) {
        pageService.deletePage(pageId);
    }
}
