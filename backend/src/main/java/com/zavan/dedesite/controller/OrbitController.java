package com.zavan.dedesite.controller;

import com.zavan.dedesite.model.Orbit;
import com.zavan.dedesite.model.User;
import com.zavan.dedesite.service.CurrentUserService;
import com.zavan.dedesite.service.ObservatoryService;
import com.zavan.dedesite.service.OrbitService;
import jakarta.validation.Valid;
import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;
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

@Controller
@RequestMapping("/observatory/orbits")
public class OrbitController {
    private final CurrentUserService currentUserService;
    private final OrbitService orbitService;
    private final ObservatoryService observatoryService;

    public OrbitController(CurrentUserService currentUserService, OrbitService orbitService, ObservatoryService observatoryService) {
        this.currentUserService = currentUserService;
        this.orbitService = orbitService;
        this.observatoryService = observatoryService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails userDetails,
                       @RequestParam(defaultValue = "24") String timeFormat,
                       Model model) {
        User user = currentUserService.requireUser(userDetails);
        fillModel(model, user, new Orbit(), null, timeFormat);
        return "observatory/orbits";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal UserDetails userDetails,
                         @Valid @ModelAttribute("orbit") Orbit orbit,
                         BindingResult bindingResult,
                         @RequestParam(required = false) List<DayOfWeek> dayOfWeeks,
                         @RequestParam(defaultValue = "24") String timeFormat,
                         Model model) {
        User user = currentUserService.requireUser(userDetails);
        if (bindingResult.hasErrors()) {
            fillModel(model, user, orbit, null, timeFormat);
            return "observatory/orbits";
        }
        orbitService.saveForDays(orbit, dayOfWeeks, user);
        return "redirect:/observatory/orbits";
    }

    @GetMapping("/{publicId}/edit")
    public String edit(@AuthenticationPrincipal UserDetails userDetails,
                       @PathVariable UUID publicId,
                       @RequestParam(defaultValue = "24") String timeFormat,
                       Model model) {
        User user = currentUserService.requireUser(userDetails);
        fillModel(model, user, orbitService.getOwned(publicId, user), publicId, timeFormat);
        return "observatory/orbits";
    }

    @PostMapping("/{publicId}")
    public String update(@AuthenticationPrincipal UserDetails userDetails,
                         @PathVariable UUID publicId,
                         @Valid @ModelAttribute("orbit") Orbit orbit,
                         BindingResult bindingResult,
                         @RequestParam(required = false) List<DayOfWeek> dayOfWeeks,
                         @RequestParam(defaultValue = "24") String timeFormat,
                         Model model) {
        User user = currentUserService.requireUser(userDetails);
        if (bindingResult.hasErrors()) {
            fillModel(model, user, orbit, publicId, timeFormat);
            return "observatory/orbits";
        }
        if (dayOfWeeks != null && !dayOfWeeks.isEmpty()) {
            orbit.setDayOfWeek(dayOfWeeks.getFirst());
        }
        orbitService.update(publicId, orbit, user);
        return "redirect:/observatory/orbits";
    }

    @PostMapping("/{publicId}/toggle")
    public String toggle(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID publicId) {
        orbitService.toggle(publicId, currentUserService.requireUser(userDetails));
        return "redirect:/observatory/orbits";
    }

    @PostMapping("/{publicId}/delete")
    public String delete(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID publicId) {
        orbitService.delete(publicId, currentUserService.requireUser(userDetails));
        return "redirect:/observatory/orbits";
    }

    private void fillModel(Model model, User user, Orbit orbit, UUID editId, String timeFormat) {
        boolean twelveHourClock = observatoryService.useTwelveHourClock(timeFormat);
        model.addAttribute("orbits", orbitService.findAll(user));
        model.addAttribute("orbit", orbit);
        model.addAttribute("editId", editId);
        model.addAttribute("days", DayOfWeek.values());
        model.addAttribute("categories", Orbit.Category.values());
        model.addAttribute("kinds", List.of(Orbit.Kind.LOCKED, Orbit.Kind.REST, Orbit.Kind.MAINTENANCE, Orbit.Kind.ECLIPSE));
        model.addAttribute("colors", List.of(
                new OrbitColor("#b8b0a2", "Ash"),
                new OrbitColor("#b31312", "Signal red"),
                new OrbitColor("#f4eedf", "Paper"),
                new OrbitColor("#66717e", "Steel"),
                new OrbitColor("#879c74", "Moss"),
                new OrbitColor("#8d6b94", "Bruise"),
                new OrbitColor("#4f7f86", "Petrol"),
                new OrbitColor("#d17a52", "Rust"),
                new OrbitColor("#887f58", "Brass"),
                new OrbitColor("#7d5f5f", "Dust rose"),
                new OrbitColor("#576b8a", "Blueprint"),
                new OrbitColor("#7f6d54", "Cardboard"),
                new OrbitColor("#5f765a", "Fern"),
                new OrbitColor("#9b7250", "Copper"),
                new OrbitColor("#6e627f", "Dusk"),
                new OrbitColor("#3f6e70", "Deep sea")
        ));
        model.addAttribute("observatoryService", observatoryService);
        model.addAttribute("twelveHourClock", twelveHourClock);
        model.addAttribute("timeFormat", observatoryService.timeFormatLabel(twelveHourClock));
        model.addAttribute("timeFormatToggle", observatoryService.oppositeTimeFormat(twelveHourClock));
    }

    public record OrbitColor(String value, String label) {}
}
