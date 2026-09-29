package com.cricket.controller;

import com.cricket.dto.MatchRequest;
import com.cricket.dto.MatchResponse;
import com.cricket.entity.MatchStatus;
import com.cricket.entity.MatchType;
import com.cricket.service.MatchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping
    public List<MatchResponse> findAll() {
        return matchService.findAll();
    }

    @GetMapping("/{id}")
    public MatchResponse findById(@PathVariable Long id) {
        return matchService.findById(id);
    }

    @GetMapping("/status/{status}")
    public List<MatchResponse> findByStatus(@PathVariable MatchStatus status) {
        return matchService.findByStatus(status);
    }

    @GetMapping("/type/{type}")
    public List<MatchResponse> findByType(@PathVariable MatchType type) {
        return matchService.findByType(type);
    }

    @PostMapping
    public ResponseEntity<MatchResponse> create(@Valid @RequestBody MatchRequest request) {
        MatchResponse response = matchService.create(request);
        return ResponseEntity.created(URI.create("/api/matches/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public MatchResponse update(@PathVariable Long id, @Valid @RequestBody MatchRequest request) {
        return matchService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        matchService.delete(id);
        return ResponseEntity.noContent().build();
    }
}