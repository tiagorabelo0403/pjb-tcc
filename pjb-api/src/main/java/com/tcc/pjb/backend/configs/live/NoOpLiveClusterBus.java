package com.tcc.pjb.backend.configs.live;

import java.util.function.Consumer;

public class NoOpLiveClusterBus implements LiveClusterBus {

    @Override
    public void publish(String namespace, LiveClusterEvent event) {
    }

    @Override
    public void registerHandler(String namespace, Consumer<LiveClusterEvent> handler) {
    }

    @Override
    public boolean enabled() {
        return false;
    }
}
