package com.tcc.pjb.backend.service.ministro.crypto;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.ministro.PlenarioVirtualVoto;

@Service
public class HomomorphicVoteService {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };
    private static final List<String> OPTIONS = List.of("ACOMPANHA_RELATOR", "DIVERGE", "PARCIAL", "ABSTENCAO");

    private final ObjectMapper objectMapper;

    public HomomorphicVoteService(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public HomomorphicBallot sealBallot(String sessaoCodigo, Long ministroId, String opcaoNormalizada) {
        PaillierKeyPair keyPair = keyPair(sessaoCodigo);
        int optionIndex = optionIndex(opcaoNormalizada);
        ArrayList<String> encryptedVector = new ArrayList<>(OPTIONS.size());
        for (int i = 0; i < OPTIONS.size(); i++) {
            BigInteger message = BigInteger.valueOf(i == optionIndex ? 1L : 0L);
            BigInteger random = deterministicRandom(sessaoCodigo, ministroId, i, keyPair.publicKey.n());
            encryptedVector.add(encrypt(message, random, keyPair.publicKey).toString());
        }
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("vector", encryptedVector);
        payload.put("options", OPTIONS);
        payload.put("iat", Instant.now().getEpochSecond());
        String blob = encodeJson(payload);
        String commitment = Hashes.sha256Hex(sessaoCodigo + "|" + ministroId + "|" + opcaoNormalizada + "|" + blob);
        String proofHash = Hashes.sha256Hex("PJB-HOMO-2026|" + commitment + "|" + optionIndex + "|" + sessaoCodigo);
        return new HomomorphicBallot(blob, commitment, proofHash);
    }

    public AggregateView openAggregate(String sessaoCodigo, List<PlenarioVirtualVoto> votos) {
        PaillierKeyPair keyPair = keyPair(sessaoCodigo);
        List<BigInteger> aggregateCipher = zeroVector(keyPair.publicKey);
        int considered = 0;
        for (PlenarioVirtualVoto voto : votos) {
            if (voto.getHomomorphicTallyBlob() == null || voto.getHomomorphicTallyBlob().isBlank()) {
                continue;
            }
            List<BigInteger> ballot = decodeVector(voto.getHomomorphicTallyBlob());
            for (int i = 0; i < aggregateCipher.size(); i++) {
                aggregateCipher.set(i, add(aggregateCipher.get(i), ballot.get(i), keyPair.publicKey));
            }
            considered++;
        }
        LinkedHashMap<String, Integer> tally = new LinkedHashMap<>();
        for (int i = 0; i < OPTIONS.size(); i++) {
            int count = decrypt(aggregateCipher.get(i), keyPair).intValueExact();
            tally.put(OPTIONS.get(i), count);
        }
        String aggregateHash = Hashes.sha256Hex(sessaoCodigo + "|" + tally + "|" + considered);
        return new AggregateView(considered, tally, aggregateHash, encodeVector(aggregateCipher));
    }

    public List<String> options() {
        return OPTIONS;
    }

    private List<BigInteger> zeroVector(PublicKey publicKey) {
        ArrayList<BigInteger> vector = new ArrayList<>(OPTIONS.size());
        for (int i = 0; i < OPTIONS.size(); i++) {
            vector.add(encrypt(BigInteger.ZERO, BigInteger.ONE, publicKey));
        }
        return vector;
    }

    private String encodeVector(List<BigInteger> vector) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("vector", vector.stream().map(BigInteger::toString).toList());
        payload.put("options", OPTIONS);
        return encodeJson(payload);
    }

    @SuppressWarnings("unchecked")
    private List<BigInteger> decodeVector(String blob) {
        Map<String, Object> payload = decodeJson(blob);
        Object raw = payload.get("vector");
        if (!(raw instanceof List<?> list) || list.size() != OPTIONS.size()) {
            throw new IllegalArgumentException("Ballot homomórfico inválido.");
        }
        ArrayList<BigInteger> out = new ArrayList<>(OPTIONS.size());
        for (Object item : list) {
            out.add(new BigInteger(String.valueOf(item)));
        }
        return out;
    }

    private int optionIndex(String value) {
        String normalized = value == null ? "ABSTENCAO" : value.trim().toUpperCase(Locale.ROOT);
        int idx = OPTIONS.indexOf(normalized);
        return idx >= 0 ? idx : OPTIONS.indexOf("ABSTENCAO");
    }

    private PaillierKeyPair keyPair(String sessaoCodigo) {
        try {
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(Hashes.sha256(("PJB-HOMO-SESSION|" + sessaoCodigo).getBytes(StandardCharsets.UTF_8)));
            BigInteger p = BigInteger.probablePrime(256, random);
            BigInteger q = BigInteger.probablePrime(256, random);
            BigInteger n = p.multiply(q);
            BigInteger nsquare = n.multiply(n);
            BigInteger lambda = lcm(p.subtract(BigInteger.ONE), q.subtract(BigInteger.ONE));
            BigInteger g = n.add(BigInteger.ONE);
            BigInteger mu = lambda.modInverse(n);
            return new PaillierKeyPair(new PublicKey(n, nsquare, g), new PrivateKey(lambda, mu));
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar chave homomórfica da sessão.", e);
        }
    }

    private BigInteger deterministicRandom(String sessaoCodigo, Long ministroId, int index, BigInteger n) {
        byte[] seed = Hashes.sha256((sessaoCodigo + "|" + ministroId + "|" + index + "|R").getBytes(StandardCharsets.UTF_8));
        BigInteger candidate = new BigInteger(1, seed).mod(n.subtract(BigInteger.TWO)).add(BigInteger.TWO);
        while (!candidate.gcd(n).equals(BigInteger.ONE)) {
            candidate = candidate.add(BigInteger.ONE).mod(n.subtract(BigInteger.TWO)).add(BigInteger.TWO);
        }
        return candidate;
    }

    private BigInteger encrypt(BigInteger message, BigInteger random, PublicKey publicKey) {
        return publicKey.g().modPow(message, publicKey.nsquare())
                .multiply(random.modPow(publicKey.n(), publicKey.nsquare()))
                .mod(publicKey.nsquare());
    }

    private BigInteger add(BigInteger left, BigInteger right, PublicKey publicKey) {
        return left.multiply(right).mod(publicKey.nsquare());
    }

    private BigInteger decrypt(BigInteger cipher, PaillierKeyPair keyPair) {
        BigInteger n = keyPair.publicKey.n();
        BigInteger u = cipher.modPow(keyPair.privateKey.lambda(), keyPair.publicKey.nsquare()).subtract(BigInteger.ONE).divide(n);
        return u.multiply(keyPair.privateKey.mu()).mod(n);
    }

    private BigInteger lcm(BigInteger left, BigInteger right) {
        return left.multiply(right).divide(left.gcd(right));
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar ballot homomórfico.", e);
        }
    }

    private Map<String, Object> decodeJson(String value) {
        try {
            return objectMapper.readValue(URL_DECODER.decode(value), MAP_TYPE);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao ler ballot homomórfico.", e);
        }
    }

    public record HomomorphicBallot(String ballotBlob, String commitmentHash, String proofHash) {
    }

    public record AggregateView(int votosConsiderados,
                                Map<String, Integer> tally,
                                String aggregateHash,
                                String encryptedAggregateBlob) {
    }

    public record PublicKey(BigInteger n, BigInteger nsquare, BigInteger g) {
    }

    public record PrivateKey(BigInteger lambda, BigInteger mu) {
    }

    public record PaillierKeyPair(PublicKey publicKey, PrivateKey privateKey) {
    }
}
