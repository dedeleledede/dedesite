package com.zavan.dedesite.controller;

import com.zavan.dedesite.model.StarSystem;
import com.zavan.dedesite.model.User;
import com.zavan.dedesite.service.CometService;
import com.zavan.dedesite.service.StarSystemService;
import com.zavan.dedesite.service.CurrentUserService;
import com.zavan.dedesite.service.StarService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
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
@RequestMapping("/observatory/star-systems")
public class StarSystemController {
    private final CurrentUserService currentUserService;
    private final StarSystemService starSystemService;
    private final StarService starService;
    private final CometService cometService;

    public StarSystemController(CurrentUserService currentUserService, StarSystemService starSystemService, StarService starService, CometService cometService) {
        this.currentUserService = currentUserService;
        this.starSystemService = starSystemService;
        this.starService = starService;
        this.cometService = cometService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = currentUserService.requireUser(userDetails);
        fillModel(model, user, new StarSystem(), null);
        return "observatory/star-systems";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal UserDetails userDetails,
                         @Valid @ModelAttribute("starSystem") StarSystem starSystem,
                         BindingResult bindingResult,
                         Model model) {
        User user = currentUserService.requireUser(userDetails);
        if (bindingResult.hasErrors()) {
            fillModel(model, user, starSystem, null);
            return "observatory/star-systems";
        }
        starSystemService.save(starSystem, user);
        return "redirect:/observatory/star-systems";
    }

    @GetMapping("/{publicId}")
    public String detail(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID publicId, Model model) {
        User user = currentUserService.requireUser(userDetails);
        StarSystem starSystem = starSystemService.getOwned(publicId, user);
        model.addAttribute("starSystem", starSystem);
        model.addAttribute("stars", starService.findForStarSystem(user, starSystem));
        model.addAttribute("comets", cometService.findForStarSystem(user, starSystem));
        model.addAttribute("openStarCount", starService.countOpen(user, starSystem));
        model.addAttribute("nextStar", starService.nextOpen(user, starSystem).orElse(null));
        return "observatory/star-system-detail";
    }

    @GetMapping("/{publicId}/edit")
    public String edit(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID publicId, Model model) {
        User user = currentUserService.requireUser(userDetails);
        fillModel(model, user, starSystemService.getOwned(publicId, user), publicId);
        return "observatory/star-systems";
    }

    @PostMapping("/{publicId}")
    public String update(@AuthenticationPrincipal UserDetails userDetails,
                         @PathVariable UUID publicId,
                         @Valid @ModelAttribute("starSystem") StarSystem starSystem,
                         BindingResult bindingResult,
                         Model model) {
        User user = currentUserService.requireUser(userDetails);
        if (bindingResult.hasErrors()) {
            fillModel(model, user, starSystem, publicId);
            return "observatory/star-systems";
        }
        starSystemService.update(publicId, starSystem, user);
        return "redirect:/observatory/star-systems";
    }

    @PostMapping("/{publicId}/archive")
    public String archive(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID publicId) {
        starSystemService.archive(publicId, currentUserService.requireUser(userDetails));
        return "redirect:/observatory/star-systems";
    }

    @PostMapping("/{publicId}/delete")
    public String delete(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID publicId) {
        starSystemService.delete(publicId, currentUserService.requireUser(userDetails));
        return "redirect:/observatory/star-systems";
    }

    private void fillModel(Model model, User user, StarSystem starSystem, UUID editId) {
        var starSystems = starSystemService.findAll(user);
        Map<Long, Long> openStarCounts = starSystems.stream()
                .collect(Collectors.toMap(StarSystem::getId, item -> starService.countOpen(user, item)));
        Map<Long, Long> totalStarCounts = starSystems.stream()
                .collect(Collectors.toMap(StarSystem::getId, item -> starService.countAll(user, item)));
        Map<Long, String> nextStars = starSystems.stream()
                .collect(Collectors.toMap(StarSystem::getId, item -> starService.nextOpen(user, item).map(star -> star.getTitle()).orElse("none")));
        model.addAttribute("starSystems", starSystems);
        model.addAttribute("starSystem", starSystem);
        model.addAttribute("editId", editId);
        model.addAttribute("statuses", StarSystem.Status.values());
        model.addAttribute("priorities", StarSystem.Priority.values());
        model.addAttribute("energyTypes", StarSystem.EnergyType.values());
        model.addAttribute("openStarCounts", openStarCounts);
        model.addAttribute("totalStarCounts", totalStarCounts);
        model.addAttribute("nextStars", nextStars);
    }
}
