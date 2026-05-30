package com.zavan.dedesite.controller;

import com.zavan.dedesite.model.User;
import com.zavan.dedesite.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user,
                               HttpServletRequest request,
                               HttpServletResponse response,
                               Model model) {
        if (hasRegistrationCookie(request)) {
            model.addAttribute("user", new User());
            model.addAttribute("error", "Este navegador ja criou uma conta.");
            return "register";
        }
        try {
            userService.registerUser(user);
            response.addHeader("Set-Cookie", "dedesite_registered=1; Max-Age=31536000; Path=/; HttpOnly; SameSite=Lax");
            return "redirect:/login?registered";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    private boolean hasRegistrationCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return false;
        }
        for (var cookie : request.getCookies()) {
            if ("dedesite_registered".equals(cookie.getName())) {
                return true;
            }
        }
        return false;
    }
}
