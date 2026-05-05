package com.zavan.dedesite.controller;

import com.zavan.dedesite.service.GuestbookService;
import com.zavan.dedesite.service.PostService;
import com.zavan.dedesite.service.SiteStatusService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final GuestbookService guestbookService;
    private final PostService postService;
    private final SiteStatusService siteStatusService;

    public HomeController(GuestbookService guestbookService,
                          PostService postService,
                          SiteStatusService siteStatusService) {
        this.guestbookService = guestbookService;
        this.postService = postService;
        this.siteStatusService = siteStatusService;
    }

    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        model.addAttribute("visitorNumber", siteStatusService.getVisitorNumber(session));
        model.addAttribute("lastUpdateDistance", siteStatusService.getLastUpdateDistance());
        model.addAttribute("latestGuestbookEntry", guestbookService.getLatestEntry().orElse(null));
        model.addAttribute("latestPost", postService.getLatestPost().orElse(null));
        return "index";
    }
}
