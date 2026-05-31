package com.zavan.dedesite.controller;

import com.zavan.dedesite.model.User;
import com.zavan.dedesite.service.ChartCourseService;
import com.zavan.dedesite.service.CurrentUserService;
import com.zavan.dedesite.service.ObservatoryService;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/observatory")
public class ChartCourseController {
    private final CurrentUserService currentUserService;
    private final ObservatoryService observatoryService;
    private final ChartCourseService chartCourseService;

    public ChartCourseController(CurrentUserService currentUserService,
                                 ObservatoryService observatoryService,
                                 ChartCourseService chartCourseService) {
        this.currentUserService = currentUserService;
        this.observatoryService = observatoryService;
        this.chartCourseService = chartCourseService;
    }

    @GetMapping("/launch-windows")
    public String launchWindows(@AuthenticationPrincipal UserDetails userDetails,
                                @RequestParam(required = false) LocalDate week,
                                Model model) {
        User user = currentUserService.requireUser(userDetails);
        LocalDate weekStart = weekStart(week);
        model.addAttribute("weekStart", weekStart);
        model.addAttribute("weekEnd", weekStart.plusDays(6));
        model.addAttribute("previousWeek", weekStart.minusWeeks(1));
        model.addAttribute("nextWeek", weekStart.plusWeeks(1));
        model.addAttribute("launchWindows", observatoryService.findLaunchWindows(user, weekStart, weekStart.plusDays(6)));
        return "observatory/launch-windows";
    }

    @GetMapping("/chart-course")
    public String chartCourse(@AuthenticationPrincipal UserDetails userDetails,
                              @RequestParam(required = false) LocalDate week,
                              @RequestParam(defaultValue = "false") boolean generate,
                              Model model) {
        User user = currentUserService.requireUser(userDetails);
        LocalDate weekStart = weekStart(week);
        model.addAttribute("weekStart", weekStart);
        model.addAttribute("weekEnd", weekStart.plusDays(6));
        model.addAttribute("previousWeek", weekStart.minusWeeks(1));
        model.addAttribute("nextWeek", weekStart.plusWeeks(1));
        if (generate) {
            model.addAttribute("course", chartCourseService.preview(user, weekStart));
        }
        return "observatory/chart-course";
    }

    @PostMapping("/chart-course/accept")
    public String accept(@AuthenticationPrincipal UserDetails userDetails,
                         @RequestParam LocalDate week,
                         @RequestParam(required = false) List<String> selected,
                         RedirectAttributes redirectAttributes) {
        int accepted = chartCourseService.accept(currentUserService.requireUser(userDetails), week, selected);
        redirectAttributes.addFlashAttribute("acceptedCount", accepted);
        return "redirect:/observatory/chart-course?week=" + week + "&generate=true";
    }

    private LocalDate weekStart(LocalDate week) {
        return (week == null ? LocalDate.now() : week)
                .with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
    }
}
