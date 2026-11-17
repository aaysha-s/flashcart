package com.flashcart.live;

public final class Channels {

    /** Redis pub/sub channel every live-service pod listens to. */
    public static final String LIVE = "flashcart:live";

    private Channels() {
    }
}
