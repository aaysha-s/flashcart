package com.flashcart.inventory.web.dto;

import jakarta.validation.constraints.Positive;

public record ReserveRequest(@Positive int quantity) {}