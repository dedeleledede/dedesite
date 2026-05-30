package com.zavan.dedesite.controller;

import com.zavan.dedesite.model.User;
import com.zavan.dedesite.service.CurrentUserService;
import com.zavan.dedesite.service.ObservatorySearchService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/observatory/search")
public class ObservatorySearchController {
    private final CurrentUserService currentUserService;
    private final ObservatorySearchService observatorySearchService;

    public ObservatorySearchController(CurrentUserService currentUserService, ObservatorySearchService observatorySearchService) {
        this.currentUserService = currentUserService;
        this.observatorySearchService = observatorySearchService;
    }

    @GetMapping
    public String search(@AuthenticationPrincipal UserDetails userDetails,
                         @RequestParam(required = false) String q,
                         Model model) {
        User user = currentUserService.requireUser(userDetails);
        model.addAttribute("query", q);
        model.addAttribute("results", observatorySearchService.search(user, q));
        return "observatory/search";
    }
}
