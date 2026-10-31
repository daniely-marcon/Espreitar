package com.example.espreitar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PaginaInicialController {
    @GetMapping("/")
    public String inicio() {
        return "redirect:/Espreitar/html/index.html";
    }
}
