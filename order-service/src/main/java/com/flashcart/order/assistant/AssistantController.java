package com.flashcart.order.assistant;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders/assistant")
public class AssistantController {

    private final ShoppingAssistant assistant;

    public AssistantController(ShoppingAssistant assistant) {
        this.assistant = assistant;
    }

    @PostMapping
    public AssistantReply chat(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AssistantRequest request) {
        Long customerId = Long.valueOf(jwt.getSubject());
        return new AssistantReply(assistant.chat(customerId, jwt.getTokenValue(), request.message()));
    }
}
