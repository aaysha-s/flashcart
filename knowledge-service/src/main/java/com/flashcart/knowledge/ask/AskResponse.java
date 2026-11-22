package com.flashcart.knowledge.ask;

import java.util.List;

public record AskResponse(String answer, boolean grounded, List<SourceRef> sources) {}