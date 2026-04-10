package com.taskforge.search.controller;

import com.taskforge.search.dto.SearchResultResponse;
import com.taskforge.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<SearchResultResponse> search(@RequestParam("q") String query) {
        if (query == null || query.trim().length() < 2) {
            return ResponseEntity.badRequest().build();
        }
        SearchResultResponse results = searchService.search(query.trim());
        return ResponseEntity.ok(results);
    }
}
