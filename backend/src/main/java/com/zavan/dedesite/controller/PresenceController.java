package com.zavan.dedesite.controller;

import com.zavan.dedesite.service.SiteStatusService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class PresenceController {

    private final SiteStatusService siteStatusService;

    public PresenceController(SiteStatusService siteStatusService) {
        this.siteStatusService = siteStatusService;
    }

    @PostMapping("/presence/ping")
    public ResponseEntity<Void> ping(HttpServletRequest request, HttpServletResponse response) {
        siteStatusService.getVisitorNumber(request, response);
        return ResponseEntity.noContent().build();
    }
}
