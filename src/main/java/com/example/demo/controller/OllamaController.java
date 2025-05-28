package com.example.demo.controller;

import com.example.demo.pojo.OllamaRequest;
import com.example.demo.pojo.OllamaResponse;
import com.example.demo.service.OllamaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OllamaController {

    private final OllamaService ollamaService;

    public OllamaController(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }
    @PostMapping("/api/generate")
    public ResponseEntity<OllamaResponse> generateSomething(@RequestBody OllamaRequest request) {
        OllamaResponse response = ollamaService.selectBestSuggestion(request);
        if (response == null) {
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok(response);
    }
}
