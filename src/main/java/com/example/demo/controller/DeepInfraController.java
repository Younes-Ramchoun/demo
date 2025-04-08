package com.example.demo.controller;

import com.example.demo.service.DeepInfraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/chat")
public class DeepInfraController {

    @Autowired
    private DeepInfraService deepInfraService;

    @PostMapping
    public Map<String, Object> chat(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        // Appel du service pour envoyer la requête au modèle AI
        return deepInfraService.sendPrompt(prompt);
    }
}
