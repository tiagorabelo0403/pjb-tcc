package com.tcc.pjb.backend.service.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tcc.pjb.backend.model.entity.ui.UiStateHistory;
import com.tcc.pjb.backend.model.entity.ui.UiSubjectType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UiHistoryServiceTopicsTest {

    @Test
    void shouldPrioritizeInboxAndAlsoPublishProcessTopic() {
        UiStateHistory event = new UiStateHistory(
                UUID.randomUUID(),
                UiSubjectType.INBOX,
                10L,
                null,
                "adv:1",
                "EVENT",
                null,
                null,
                "[]",
                "[]",
                null,
                null,
                "msg",
                Instant.now()
        );

        assertEquals(List.of("HIST:INBOX:adv:1", "HIST:10"), UiHistoryService.liveTopics(event));
    }

    @Test
    void shouldPrioritizeWorkItemAndAlsoPublishProcessTopic() {
        UiStateHistory event = new UiStateHistory(
                UUID.randomUUID(),
                UiSubjectType.WORKITEM,
                20L,
                33L,
                null,
                "EVENT",
                null,
                null,
                "[]",
                "[]",
                null,
                null,
                "msg",
                Instant.now()
        );

        assertEquals(List.of("HIST:WORKITEM:33", "HIST:20"), UiHistoryService.liveTopics(event));
    }
}
