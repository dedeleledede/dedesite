package com.zavan.dedesite.controller;

import com.zavan.dedesite.model.Comet;
import com.zavan.dedesite.model.User;
import com.zavan.dedesite.service.CometService;
import com.zavan.dedesite.service.StarSystemService;
import com.zavan.dedesite.service.CurrentUserService;
import com.zavan.dedesite.service.ObservatoryService;
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
@RequestMapping("/observatory/comets")
public class CometController {
    private final CurrentUserService currentUserService;
    private final CometService cometService;
    private final StarSystemService starSystemService;
    private final ObservatoryService observatoryService;

    public CometController(CurrentUserService currentUserService, CometService cometService, StarSystemService starSystemService, ObservatoryService observatoryService) {
        this.currentUserService = currentUserService;
        this.cometService = cometService;
        this.starSystemService = starSystemService;
        this.observatoryService = observatoryService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails userDetails,
                       @RequestParam(defaultValue = "24") String timeFormat,
                       Model model) {
        User user = currentUserService.requireUser(userDetails);
        fillModel(model, user, new Comet(), null, timeFormat);
        return "observatory/comets";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal UserDetails userDetails,
                         @Valid @ModelAttribute("comet") Comet comet,
                         BindingResult bindingResult,
                         @RequestParam(required = false) Long starSystemId,
                         @RequestParam(defaultValue = "24") String timeFormat,
                         Model model) {
        User user = currentUserService.requireUser(userDetails);
        if (bindingResult.hasErrors()) {
            fillModel(model, user, comet, null, timeFormat);
            return "observatory/comets";
        }
        cometService.save(comet, starSystemId, user);
        return "redirect:/observatory/comets";
    }

    @GetMapping("/{publicId}/edit")
    public String edit(@AuthenticationPrincipal UserDetails userDetails,
                       @PathVariable UUID publicId,
                       @RequestParam(defaultValue = "24") String timeFormat,
                       Model model) {
        User user = currentUserService.requireUser(userDetails);
        fillModel(model, user, cometService.getOwned(publicId, user), publicId, timeFormat);
        return "observatory/comets";
    }

    @PostMapping("/{publicId}")
    public String update(@AuthenticationPrincipal UserDetails userDetails,
                         @PathVariable UUID publicId,
                         @Valid @ModelAttribute("comet") Comet comet,
                         BindingResult bindingResult,
                         @RequestParam(required = false) Long starSystemId,
                         @RequestParam(defaultValue = "24") String timeFormat,
                         Model model) {
        User user = currentUserService.requireUser(userDetails);
        if (bindingResult.hasErrors()) {
            fillModel(model, user, comet, publicId, timeFormat);
            return "observatory/comets";
        }
        cometService.update(publicId, comet, starSystemId, user);
        return "redirect:/observatory/comets";
    }

    @PostMapping("/{publicId}/delete")
    public String delete(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID publicId) {
        cometService.delete(publicId, currentUserService.requireUser(userDetails));
        return "redirect:/observatory/comets";
    }

    private void fillModel(Model model, User user, Comet comet, UUID editId, String timeFormat) {
        boolean twelveHourClock = observatoryService.useTwelveHourClock(timeFormat);
        model.addAttribute("comets", cometService.findAll(user));
        model.addAttribute("comet", comet);
        model.addAttribute("editId", editId);
        model.addAttribute("types", Comet.Type.values());
        model.addAttribute("priorities", Comet.Priority.values());
        model.addAttribute("starSystems", starSystemService.findOpen(user));
        model.addAttribute("observatoryService", observatoryService);
        model.addAttribute("twelveHourClock", twelveHourClock);
        model.addAttribute("timeFormat", observatoryService.timeFormatLabel(twelveHourClock));
        model.addAttribute("timeFormatToggle", observatoryService.oppositeTimeFormat(twelveHourClock));
    }
}
