package com.zavan.dedesite.controller;

import com.zavan.dedesite.model.Star;
import com.zavan.dedesite.model.User;
import com.zavan.dedesite.service.StarSystemService;
import com.zavan.dedesite.service.CurrentUserService;
import com.zavan.dedesite.service.StarService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.UUID;

@Controller
@RequestMapping("/observatory/stars")
public class StarController {
    private final CurrentUserService currentUserService;
    private final StarService starService;
    private final StarSystemService starSystemService;

    public StarController(CurrentUserService currentUserService, StarService starService, StarSystemService starSystemService) {
        this.currentUserService = currentUserService;
        this.starService = starService;
        this.starSystemService = starSystemService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails userDetails,
                       @RequestParam(required = false) Star.Status status,
                       Model model) {
        User user = currentUserService.requireUser(userDetails);
        fillModel(model, user, new Star(), null, status);
        return "observatory/stars";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal UserDetails userDetails,
                         @Valid @ModelAttribute("star") Star star,
                         BindingResult bindingResult,
                         @RequestParam(required = false) Long starSystemId,
                         Model model) {
        User user = currentUserService.requireUser(userDetails);
        if (bindingResult.hasErrors()) {
            fillModel(model, user, star, null, null);
            return "observatory/stars";
        }
        starService.save(star, starSystemId, user);
        return "redirect:/observatory/stars";
    }

    @GetMapping("/{publicId}/edit")
    public String edit(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID publicId, Model model) {
        User user = currentUserService.requireUser(userDetails);
        fillModel(model, user, starService.getOwned(publicId, user), publicId, null);
        return "observatory/stars";
    }

    @PostMapping("/{publicId}")
    public String update(@AuthenticationPrincipal UserDetails userDetails,
                         @PathVariable UUID publicId,
                         @Valid @ModelAttribute("star") Star star,
                         BindingResult bindingResult,
                         @RequestParam(required = false) Long starSystemId,
                         Model model) {
        User user = currentUserService.requireUser(userDetails);
        if (bindingResult.hasErrors()) {
            fillModel(model, user, star, publicId, null);
            return "observatory/stars";
        }
        starService.update(publicId, star, starSystemId, user);
        return "redirect:/observatory/stars";
    }

    @PostMapping("/{publicId}/complete")
    public String complete(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID publicId) {
        starService.complete(publicId, currentUserService.requireUser(userDetails));
        return "redirect:/observatory/stars";
    }

    @PostMapping("/{publicId}/delete")
    public String delete(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID publicId) {
        starService.delete(publicId, currentUserService.requireUser(userDetails));
        return "redirect:/observatory/stars";
    }

    private void fillModel(Model model, User user, Star star, UUID editId, Star.Status filterStatus) {
        model.addAttribute("stars", filterStatus == null ? starService.findAll(user) : starService.findByStatus(user, filterStatus));
        model.addAttribute("star", star);
        model.addAttribute("editId", editId);
        model.addAttribute("filterStatus", filterStatus);
        model.addAttribute("statuses", Star.Status.values());
        model.addAttribute("priorities", Star.Priority.values());
        model.addAttribute("starSystems", starSystemService.findOpen(user));
    }
}
