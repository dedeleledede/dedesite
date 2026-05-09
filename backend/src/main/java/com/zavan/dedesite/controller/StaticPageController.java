package com.zavan.dedesite.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StaticPageController {

    @GetMapping("/projects")
    public String projects(Model model) {
        return "projects";
    }

    @GetMapping("/music")
    public String music(Model model) {
        model.addAttribute("pageTitle", "music");
        model.addAttribute("pageText", "notes, listening logs, piano things, and music thoughts will live here.");
        return "placeholder";
    }

    @GetMapping("/gerp")
    public String gerp(Model model) {
        model.addAttribute("pageTitle", "gerp");
        model.addAttribute("pageText", "gerp gets its own page soon.");
        return "placeholder";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("pageTitle", "profile");
        model.addAttribute("pageText", "computer science, software, art, comics, music, games, and personal notes.");
        return "placeholder";
    }

    @GetMapping("/chat")
    public String chat(Model model) {
        model.addAttribute("pageTitle", "chat");
        model.addAttribute("pageText", "chat is not wired yet.");
        return "placeholder";
    }

    @GetMapping("/themes")
    public String themes(Model model) {
        model.addAttribute("pageTitle", "themes");
        model.addAttribute("pageText", "theme controls and visual experiments will live here.");
        return "placeholder";
    }

    @GetMapping("/anonymous")
    public String anonymous(Model model) {
        model.addAttribute("pageTitle", "anonymous");
        model.addAttribute("pageText", "anonymous messages and drawings will live here.");
        return "placeholder";
    }

    @GetMapping("/apps")
    public String apps(Model model) {
        model.addAttribute("pageTitle", "apps");
        model.addAttribute("pageText", "small programs, calculators, and experiments will live here.");
        return "placeholder";
    }
}
