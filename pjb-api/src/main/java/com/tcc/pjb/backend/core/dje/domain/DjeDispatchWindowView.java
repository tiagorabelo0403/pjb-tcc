package com.tcc.pjb.backend.core.dje.domain;

import java.time.LocalDate;

public record DjeDispatchWindowView(
        LocalDate from,
        LocalDate to,
        int items
) {}
