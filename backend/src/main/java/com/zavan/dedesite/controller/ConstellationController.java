package com.zavan.dedesite.controller;

import com.zavan.dedesite.model.Constellation;
import com.zavan.dedesite.model.User;
import com.zavan.dedesite.service.CometService;
import com.zavan.dedesite.service.ConstellationService;
import com.zavan.dedesite.service.CurrentUserService;
import com.zavan.dedesite.service.StarService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.stream.Collectors;
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

@Controller
@RequestMapping("/observatory/constellations")
public class ConstellationController {
    private final CurrentUserService currentUserService;
    private final ConstellationService constellationService;
    private final StarService starService;
    private final CometService cometService;

    public ConstellationController(CurrentUserService currentUserService, ConstellationService constellationService, StarService starService, CometService cometService) {
        this.currentUserService = currentUserService;
        this.constellationService = constellationService;
        this.starService = starService;
        this.cometService = cometService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = currentUserService.requireUser(userDetails);
        fillModel(model, user, new Constellation(), null);
        return "observatory/constellations";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal UserDetails userDetails,
                         @Valid @ModelAttribute("constellation") Constellation constellation,
                         BindingResult bindingResult,
                         Model model) {
        User user = currentUserService.requireUser(userDetails);
        if (bindingResult.hasErrors()) {
            fillModel(model, user, constellation, null);
            return "observatory/constellations";
        }
        constellationService.save(constellation, user);
        return "redirect:/observatory/constellations";
    }

    @GetMapping("/{id}")
    public String detail(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long id, Model model) {
        User user = currentUserService.requireUser(userDetails);
        Constellation constellation = constellationService.getOwned(id, user);
        model.addAttribute("constellation", constellation);
        model.addAttribute("stars", starService.findForConstellation(user, constellation));
        model.addAttribute("comets", cometService.findForConstellation(user, constellation));
        model.addAttribute("openStarCount", starService.countOpen(user, constellation));
        model.addAttribute("nextStar", starService.nextOpen(user, constellation).orElse(null));
        return "observatory/constellation-detail";
    }

    @GetMapping("/{id}/edit")
    public String edit(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long id, Model model) {
        User user = currentUserService.requireUser(userDetails);
        fillModel(model, user, constellationService.getOwned(id, user), id);
        return "observatory/constellations";
    }

    @PostMapping("/{id}")
    public String update(@AuthenticationPrincipal UserDetails userDetails,
                         @PathVariable Long id,
                         @Valid @ModelAttribute("constellation") Constellation constellation,
                         BindingResult bindingResult,
                         Model model) {
        User user = currentUserService.requireUser(userDetails);
        if (bindingResult.hasErrors()) {
            fillModel(model, user, constellation, id);
            return "observatory/constellations";
        }
        constellationService.update(id, constellation, user);
        return "redirect:/observatory/constellations";
    }

    @PostMapping("/{id}/archive")
    public String archive(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long id) {
        constellationService.archive(id, currentUserService.requireUser(userDetails));
        return "redirect:/observatory/constellations";
    }

    @PostMapping("/{id}/delete")
    public String delete(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long id) {
        constellationService.delete(id, currentUserService.requireUser(userDetails));
        return "redirect:/observatory/constellations";
    }

    private void fillModel(Model model, User user, Constellation constellation, Long editId) {
        var constellations = constellationService.findAll(user);
        Map<Long, Long> openStarCounts = constellations.stream()
                .collect(Collectors.toMap(Constellation::getId, item -> starService.countOpen(user, item)));
        Map<Long, String> nextStars = constellations.stream()
                .collect(Collectors.toMap(Constellation::getId, item -> starService.nextOpen(user, item).map(star -> star.getTitle()).orElse("none")));
        model.addAttribute("constellations", constellations);
        model.addAttribute("constellation", constellation);
        model.addAttribute("editId", editId);
        model.addAttribute("statuses", Constellation.Status.values());
        model.addAttribute("priorities", Constellation.Priority.values());
        model.addAttribute("energyTypes", Constellation.EnergyType.values());
        model.addAttribute("openStarCounts", openStarCounts);
        model.addAttribute("nextStars", nextStars);
    }
}
