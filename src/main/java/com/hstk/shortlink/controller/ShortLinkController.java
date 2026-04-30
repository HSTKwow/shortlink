package com.hstk.shortlink.controller;

import com.hstk.shortlink.common.ApiResponse;
import com.hstk.shortlink.common.PageResult;
import com.hstk.shortlink.model.dto.CreateShortLinkRequest;
import com.hstk.shortlink.model.dto.ShortLinkStatsResponse;
import com.hstk.shortlink.model.dto.UpdateShortLinkStatusRequest;
import com.hstk.shortlink.model.entity.ShortLink;
import com.hstk.shortlink.model.entity.ShortLinkVisitLog;
import com.hstk.shortlink.service.ShortLinkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
public class ShortLinkController {
    private final ShortLinkService shortLinkService;

    public ShortLinkController(ShortLinkService shortLinkService) {
        this.shortLinkService = shortLinkService;
    }

    @PostMapping("/api/short-links")
    public ApiResponse<Map<String, String>> createShortLink(@Valid @RequestBody CreateShortLinkRequest request){
        String shortCode=shortLinkService.createShortLink(request.getOriginalUrl(),request.getExpireTime());
        log.info("post");
        return ApiResponse.success(Map.of(
                "shortCode",shortCode,
                "shortUrl","http://localhost:8080/"+shortCode
        ));
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode,
                                         HttpServletRequest request){
        log.info("get");
        String ip=request.getRemoteAddr();
        String userAgent=request.getHeader("User-Agent");
        String referer=request.getHeader("Referer");
        String originalUrl=shortLinkService.getOriginalUrl(shortCode,ip,userAgent,referer);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .build();

    }

    @GetMapping("/api/short-links/{shortCode}")
    public ApiResponse<ShortLink>getShortLinkDetail(@PathVariable String shortCode){
        ShortLink shortLink=shortLinkService.getShortLink(shortCode);
        return ApiResponse.success(shortLink);
    }

    @PatchMapping("/api/short-links/{shortCode}/status")
    public ApiResponse<Void> updateStatus(@PathVariable String shortCode,
                                          @Valid @RequestBody UpdateShortLinkStatusRequest request){
        shortLinkService.updateStatus(shortCode,request.getStatus());
        //log.info("updateStatus");
        return ApiResponse.success(null);
    }

    @GetMapping("/api/short-links/{shortCode}/visits")
    public ApiResponse<List<ShortLinkVisitLog>>getVisitLogs(@PathVariable String shortCode,
                                                            @RequestParam(required = false) Integer limit){
       //log.info("get visits");

        List<ShortLinkVisitLog> shortLinkVisitLogs=shortLinkService.getVisitLogs(shortCode,limit);
        return ApiResponse.success(shortLinkVisitLogs);

    }

    @GetMapping("/api/short-links")
    public ApiResponse<PageResult<ShortLink>>listShotLinks(@RequestParam(required = false)Integer page,
                                                           @RequestParam(required = false)Integer pageSize,
                                                           @RequestParam(required = false)Integer status){
        PageResult<ShortLink>result=shortLinkService.listShortLinks(page,pageSize,status);

        return ApiResponse.success(result);

    }

    @GetMapping("/api/short-links/{shortCode}/stats")
    public ApiResponse<ShortLinkStatsResponse> getStats(@PathVariable String shortCode) {
        ShortLinkStatsResponse stats = shortLinkService.getStats(shortCode);
        return ApiResponse.success(stats);
    }





}
