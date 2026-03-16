package com.docflow.infra.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    @GetMapping({"/", "/login", "/app", "/app/**"})
    public String forwardSpaRoutes() {
        return "forward:/index.html";
    }
}
