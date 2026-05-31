package com.zavan.dedesite.controller;

import com.zavan.dedesite.model.Orbit;
import com.zavan.dedesite.model.Star;
import com.zavan.dedesite.model.StarSystem;
import com.zavan.dedesite.model.User;
import com.zavan.dedesite.service.CurrentUserService;
import com.zavan.dedesite.service.PulsarService;
import com.zavan.dedesite.service.StarSystemService;
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
@RequestMapping("/observatory/pulsars")
public class PulsarController {
    private final CurrentUserService currentUserService;
    private final PulsarService pulsarService;
    private final StarSystemService starSystemService;

    public PulsarController(CurrentUserService currentUserService, PulsarService pulsarService, StarSystemService starSystemService) {
        this.currentUserService = currentUserService;
        this.pulsarService = pulsarService;
        this.starSystemService = starSystemService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = currentUserService.requireUser(userDetails);
        Orbit pulsar = new Orbit();
        pulsar.setKind(Orbit.Kind.PULSAR);
        pulsar.setFlexibility(Orbit.Flexibility.FLEXIBLE);
        pulsar.setAutoSchedule(true);
        fillModel(model, user, pulsar, null);
        return "observatory/pulsars";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal UserDetails userDetails,
                         @Valid @ModelAttribute("pulsar") Orbit pulsar,
                         BindingResult bindingResult,
                         @RequestParam(required = false) Long starSystemId,
                         Model model) {
        User user = currentUserService.requireUser(userDetails);
        if (bindingResult.hasErrors()) {
            fillModel(model, user, pulsar, null);
            return "observatory/pulsars";
        }
        pulsarService.save(pulsar, starSystemId, user);
        return "redirect:/observatory/pulsars";
    }

    @GetMapping("/{publicId}/edit")
    public String edit(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID publicId, Model model) {
        User user = currentUserService.requireUser(userDetails);
        fillModel(model, user, pulsarService.getOwned(publicId, user), publicId);
        return "observatory/pulsars";
    }

    @PostMapping("/{publicId}")
    public String update(@AuthenticationPrincipal UserDetails userDetails,
                         @PathVariable UUID publicId,
                         @Valid @ModelAttribute("pulsar") Orbit pulsar,
                         BindingResult bindingResult,
                         @RequestParam(required = false) Long starSystemId,
                         Model model) {
        User user = currentUserService.requireUser(userDetails);
        if (bindingResult.hasErrors()) {
            fillModel(model, user, pulsar, publicId);
            return "observatory/pulsars";
        }
        pulsarService.update(publicId, pulsar, starSystemId, user);
        return "redirect:/observatory/pulsars";
    }

    @PostMapping("/{publicId}/toggle")
    public String toggle(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID publicId) {
        pulsarService.toggle(publicId, currentUserService.requireUser(userDetails));
        return "redirect:/observatory/pulsars";
    }

    @PostMapping("/{publicId}/delete")
    public String delete(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID publicId) {
        pulsarService.delete(publicId, currentUserService.requireUser(userDetails));
        return "redirect:/observatory/pulsars";
    }

    private void fillModel(Model model, User user, Orbit pulsar, UUID editId) {
        model.addAttribute("pulsars", pulsarService.findAll(user));
        model.addAttribute("pulsar", pulsar);
        model.addAttribute("editId", editId);
        model.addAttribute("priorities", Star.Priority.values());
        model.addAttribute("energyTypes", StarSystem.EnergyType.values());
        model.addAttribute("starSystems", starSystemService.findOpen(user));
    }
}
