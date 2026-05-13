package com.tcc.pjb.backend.modules.advocacia.entity.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CriptografiaPJBTest {

    @Test
    void hashCpfCnpj_shouldNormalizeDigits_beforeHashing() {
        String formatted = "123.456.789-00";
        String digits = "12345678900";

        assertEquals(digits, CriptografiaPJB.normalizarDocumentoNumerico(formatted));
        assertEquals(CriptografiaPJB.sha256Hex(digits), CriptografiaPJB.hashCpfCnpj(formatted));
    }

    @Test
    void candidateCpfHashes_shouldIncludeCanonicalAndLegacy() {
        String formatted = "123.456.789-00";
        var hashes = CriptografiaPJB.candidateCpfHashes(formatted);

        assertFalse(hashes.isEmpty());
        assertTrue(hashes.contains(CriptografiaPJB.hashCpfCnpj(formatted)));
        assertTrue(hashes.contains(CriptografiaPJB.sha256Hex(formatted.trim())));

        assertTrue(hashes.contains(CriptografiaPJB.sha512Hex("12345678900")));
    }

    @Test
    void gerarHash_shouldBeSha256() {
        String v = "abc";
        assertEquals(CriptografiaPJB.sha256Hex(v), CriptografiaPJB.gerarHash(v));
        assertNotEquals(CriptografiaPJB.sha512Hex(v), CriptografiaPJB.gerarHash(v));
    }
}
