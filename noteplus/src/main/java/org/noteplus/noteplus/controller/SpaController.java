package org.noteplus.noteplus.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {

    @RequestMapping(value = {
        "/", "/login", "/register", "/notes", "/notes/**",
        "/categories", "/categories/**", "/learning-paths",
        "/learning-paths/**", "/settings", "/settings/**",
        "/forgot-password", "/reset-password"
    })
    public String spa() {
        return "forward:/index.html";
    }
}
