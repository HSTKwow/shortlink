package com.hstk.shortlink.controller;

import com.hstk.shortlink.common.ApiResponse;
import com.hstk.shortlink.model.dto.CreateShortLinkRequest;
import com.hstk.shortlink.service.ShortLinkService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
public class ShortLinkController {
    private final ShortLinkService shortLinkService;

    public ShortLinkController(ShortLinkService shortLinkService) {
        this.shortLinkService = shortLinkService;
    }

    @PostMapping("/api/short-links")
    public ApiResponse<Map<String, String>> createShortLink(@Valid @RequestBody CreateShortLinkRequest request){
        String shortCode=shortLinkService.createShortLink(request.getOriginalUrl(),request.getExpireTime());

        return ApiResponse.success(Map.of(
                "shortCode",shortCode,
                "shortUrl","http:localhost:8080/"+shortCode
        ));
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode){
        String originalUrl=shortLinkService.getOriginalUrl(shortCode);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .build();

    }
}
