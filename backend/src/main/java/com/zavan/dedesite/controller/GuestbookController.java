package com.zavan.dedesite.controller;

import com.zavan.dedesite.model.GuestbookEntry;
import com.zavan.dedesite.service.GuestbookService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/guestbook")
public class GuestbookController {

    private final GuestbookService guestbookService;

    public GuestbookController(GuestbookService guestbookService) {
        this.guestbookService = guestbookService;
    }

    @GetMapping
    public String showGuestbook(Model model) {
        if (!model.containsAttribute("guestbookEntry")) {
            model.addAttribute("guestbookEntry", new GuestbookEntry());
        }
        model.addAttribute("entries", guestbookService.getEntries());
        return "guestbook";
    }

    @PostMapping
    public String signGuestbook(@Valid @ModelAttribute GuestbookEntry guestbookEntry,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("entries", guestbookService.getEntries());
            return "guestbook";
        }

        guestbookService.saveEntry(guestbookEntry);
        redirectAttributes.addFlashAttribute("guestbookSaved", true);
        return "redirect:/guestbook";
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteEntry(@PathVariable Long id) {
        guestbookService.deleteEntryById(id);
        return "redirect:/guestbook";
    }
}
