package com.zavan.dedesite.controller;

import com.zavan.dedesite.service.GuestbookService;
import com.zavan.dedesite.service.LastFmService;
import com.zavan.dedesite.service.PostService;
import com.zavan.dedesite.service.SiteStatusService;
import com.zavan.dedesite.service.SiteSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final GuestbookService guestbookService;
    private final PostService postService;
    private final SiteStatusService siteStatusService;
    private final LastFmService lastFmService;
    private final SiteSettingsService siteSettingsService;

    public HomeController(GuestbookService guestbookService,
                          PostService postService,
                          SiteStatusService siteStatusService,
                          LastFmService lastFmService,
                          SiteSettingsService siteSettingsService) {
        this.guestbookService = guestbookService;
        this.postService = postService;
        this.siteStatusService = siteStatusService;
        this.lastFmService = lastFmService;
        this.siteSettingsService = siteSettingsService;
    }

    @GetMapping("/")
    public String home(Model model, HttpServletRequest request, HttpServletResponse response) {
        long visitorNumber = siteStatusService.getVisitorNumber(request, response);
        model.addAttribute("visitorNumber", visitorNumber);
        model.addAttribute("onlineVisitors", siteStatusService.getOnlineVisitors(visitorNumber));
        model.addAttribute("lastUpdateDistance", siteStatusService.getLastUpdateDistance());
        model.addAttribute("latestGuestbookEntry", guestbookService.getLatestEntry().orElse(null));
        model.addAttribute("latestPost", postService.getLatestPost().orElse(null));
        model.addAttribute("lastFmTrack", lastFmService.getLatestTrack().orElse(null));
        model.addAttribute("siteSettings", siteSettingsService.get());
        return "index";
    }
}
