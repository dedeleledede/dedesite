package com.zavan.dedesite.controller;

import com.zavan.dedesite.model.SiteSettings;
import com.zavan.dedesite.service.SiteSettingsService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/site-settings")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSiteSettingsController {
    private final SiteSettingsService siteSettingsService;

    public AdminSiteSettingsController(SiteSettingsService siteSettingsService) {
        this.siteSettingsService = siteSettingsService;
    }

    @GetMapping
    public String form(Model model) {
        model.addAttribute("siteSettings", siteSettingsService.get());
        return "admin/site-settings";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("siteSettings") SiteSettings siteSettings,
                       BindingResult bindingResult,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/site-settings";
        }
        siteSettingsService.save(siteSettings);
        redirectAttributes.addFlashAttribute("saved", true);
        return "redirect:/admin/site-settings";
    }
}
