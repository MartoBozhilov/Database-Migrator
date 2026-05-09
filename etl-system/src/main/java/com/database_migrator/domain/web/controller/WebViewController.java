package com.database_migrator.domain.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Web view controller for Thymeleaf pages
 * All data fetching is done via JavaScript calling existing REST API endpoints
 */
@Controller
public class WebViewController {

    @GetMapping("/")
    public String index() {
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/home")
    public String home() {
        return "home";
    }

    @GetMapping("/connectors")
    public String connectors() {
        return "connectors";
    }

    @GetMapping("/connectors/{id}")
    public String connectorDetail(@PathVariable Long id) {
        return "connector-detail";
    }

    @GetMapping("/scans")
    public String scans() {
        return "scans";
    }

    @GetMapping("/scans/{id}")
    public String scanDetail(@PathVariable Long id) {
        return "scan-detail";
    }

    @GetMapping("/transformations")
    public String transformations() {
        return "transformations";
    }

    @GetMapping("/transformations/{id}")
    public String transformationDetail(@PathVariable Long id) {
        return "transformation-detail";
    }

    @GetMapping("/cycles")
    public String cycles() {
        return "cycles";
    }

    @GetMapping("/cycles/{id}")
    public String cycleDetail(@PathVariable Long id) {
        return "cycle-detail";
    }

    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }
}
