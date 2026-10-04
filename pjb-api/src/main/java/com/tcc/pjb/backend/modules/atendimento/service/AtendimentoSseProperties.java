package com.tcc.pjb.backend.modules.atendimento.service;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.atendimento.sse")
public class AtendimentoSseProperties {

    private int replayBuffer = 200;
    private int maxBatchEvents = 200;
    private int maxPendingBacklog = 2000;
    private long emitterTimeoutMs = 1_800_000L;
    private int maxChannels = 2048;
    private int maxSubscribersPerTopic = 5;
    private int maxChannelsPerFlushCycle = 256;
    private Duration idleChannelTtl = Duration.ofMinutes(5);

    public int getReplayBuffer() {
        return replayBuffer;
    }

    public void setReplayBuffer(int replayBuffer) {
        this.replayBuffer = replayBuffer;
    }

    public int getMaxBatchEvents() {
        return maxBatchEvents;
    }

    public void setMaxBatchEvents(int maxBatchEvents) {
        this.maxBatchEvents = maxBatchEvents;
    }

    public int getMaxPendingBacklog() {
        return maxPendingBacklog;
    }

    public void setMaxPendingBacklog(int maxPendingBacklog) {
        this.maxPendingBacklog = maxPendingBacklog;
    }

    public long getEmitterTimeoutMs() {
        return emitterTimeoutMs;
    }

    public void setEmitterTimeoutMs(long emitterTimeoutMs) {
        this.emitterTimeoutMs = emitterTimeoutMs;
    }

    public int getMaxChannels() {
        return maxChannels;
    }

    public void setMaxChannels(int maxChannels) {
        this.maxChannels = maxChannels;
    }

    public int getMaxSubscribersPerTopic() {
        return maxSubscribersPerTopic;
    }

    public void setMaxSubscribersPerTopic(int maxSubscribersPerTopic) {
        this.maxSubscribersPerTopic = maxSubscribersPerTopic;
    }

    public int getMaxChannelsPerFlushCycle() {
        return maxChannelsPerFlushCycle;
    }

    public void setMaxChannelsPerFlushCycle(int maxChannelsPerFlushCycle) {
        this.maxChannelsPerFlushCycle = maxChannelsPerFlushCycle;
    }

    public Duration getIdleChannelTtl() {
        return idleChannelTtl;
    }

    public void setIdleChannelTtl(Duration idleChannelTtl) {
        this.idleChannelTtl = idleChannelTtl;
    }
}
