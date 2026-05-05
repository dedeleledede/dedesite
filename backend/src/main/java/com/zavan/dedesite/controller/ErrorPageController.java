package com.zavan.dedesite.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;

@Controller
public class ErrorPageController implements ErrorController {

    private final ErrorAttributes errorAttributes;

    public ErrorPageController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    @RequestMapping("/error")
    public String error(HttpServletRequest request, Model model) {
        ErrorAttributeOptions options = ErrorAttributeOptions.of(
                ErrorAttributeOptions.Include.MESSAGE,
                ErrorAttributeOptions.Include.EXCEPTION
        );
        Map<String, Object> attributes = errorAttributes.getErrorAttributes(new ServletWebRequest(request), options);

        model.addAttribute("status", attributes.get("status"));
        model.addAttribute("error", attributes.get("error"));
        model.addAttribute("message", attributes.get("message"));
        model.addAttribute("exception", attributes.get("exception"));
        model.addAttribute("path", attributes.get("path"));
        model.addAttribute("timestamp", attributes.get("timestamp"));
        return "error";
    }
}
