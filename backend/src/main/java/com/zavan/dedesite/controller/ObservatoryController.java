package com.zavan.dedesite.controller;

import com.zavan.dedesite.model.User;
import com.zavan.dedesite.service.CurrentUserService;
import com.zavan.dedesite.service.ObservatoryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/observatory")
public class ObservatoryController {
    private final CurrentUserService currentUserService;
    private final ObservatoryService observatoryService;

    public ObservatoryController(CurrentUserService currentUserService, ObservatoryService observatoryService) {
        this.currentUserService = currentUserService;
        this.observatoryService = observatoryService;
    }

    @GetMapping
    public String missionControl(@AuthenticationPrincipal UserDetails userDetails,
                                 @RequestParam(defaultValue = "24") String timeFormat,
                                 Model model) {
        User user = currentUserService.requireUser(userDetails);
        addClockModel(model, timeFormat);
        model.addAttribute("observatoryService", observatoryService);
        model.addAttribute("mission", observatoryService.missionControl(user));
        return "observatory/index";
    }

    @GetMapping("/sky-map")
    public String skyMap(@AuthenticationPrincipal UserDetails userDetails,
                         @RequestParam(defaultValue = "24") String timeFormat,
                         Model model) {
        User user = currentUserService.requireUser(userDetails);
        addClockModel(model, timeFormat);
        model.addAttribute("observatoryService", observatoryService);
        model.addAttribute("skyMap", observatoryService.skyMap(user));
        return "observatory/sky-map";
    }

    private void addClockModel(Model model, String timeFormat) {
        boolean twelveHourClock = observatoryService.useTwelveHourClock(timeFormat);
        model.addAttribute("twelveHourClock", twelveHourClock);
        model.addAttribute("timeFormat", observatoryService.timeFormatLabel(twelveHourClock));
        model.addAttribute("timeFormatToggle", observatoryService.oppositeTimeFormat(twelveHourClock));
    }
}
