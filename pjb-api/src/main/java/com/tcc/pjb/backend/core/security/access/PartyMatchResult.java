package com.tcc.pjb.backend.core.security.access;

public sealed interface PartyMatchResult permits PartyMatchResult.Matched, PartyMatchResult.NotMatched {

    record Matched(PartyRole role) implements PartyMatchResult {
    }

    record NotMatched() implements PartyMatchResult {
    }
}
