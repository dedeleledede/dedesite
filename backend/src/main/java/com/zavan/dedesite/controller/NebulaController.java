package com.zavan.dedesite.controller;

import com.zavan.dedesite.model.Star;
import com.zavan.dedesite.model.User;
import com.zavan.dedesite.service.CurrentUserService;
import com.zavan.dedesite.service.StarService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/observatory/nebula")
public class NebulaController {
    private final CurrentUserService currentUserService;
    private final StarService starService;

    public NebulaController(CurrentUserService currentUserService, StarService starService) {
        this.currentUserService = currentUserService;
        this.starService = starService;
    }

    @GetMapping
    public String nebula(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = currentUserService.requireUser(userDetails);
        model.addAttribute("stars", starService.findByStatus(user, Star.Status.NEBULA));
        return "observatory/nebula";
    }
}
