package com.zavan.dedesite.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class SocialMetaAdvice {

    @Value("${app.public-url:http://localhost:6969}")
    private String publicUrl;

    @ModelAttribute("siteName")
    public String siteName() {
        return "dedesite";
    }

    @ModelAttribute("siteDescription")
    public String siteDescription() {
        return "Esther's personal website: blog, projects, guestbook, music notes, experiments, and site machinery.";
    }

    @ModelAttribute("siteUrl")
    public String siteUrl(HttpServletRequest request) {
        return absoluteUrl(request.getRequestURI());
    }

    @ModelAttribute("siteImage")
    public String siteImage() {
        return absoluteUrl("/images/dedesite-embed.svg");
    }

    private String absoluteUrl(String path) {
        String base = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return base + normalizedPath;
    }
}
