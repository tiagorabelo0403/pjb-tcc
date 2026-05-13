package com.tcc.pjb.backend.platform.devtools;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class UiPolicySignatureTool {

    public static void main(String[] args) throws Exception {
        if (args == null || args.length < 2) {
            System.err.println("usage: UiPolicySignatureTool <resourcePathOrFile> <masterKeyBase64>");
            System.exit(2);
        }
        String loc = requiredArg(args, 0, "resourcePathOrFile");
        String masterKeyBase64 = requiredArg(args, 1, "masterKeyBase64");
        byte[] master = Base64.getDecoder().decode(masterKeyBase64.trim());
        byte[] derived = null;
        try {
            derived = deriveKeyBytes(master);
            Key key = new SecretKeySpec(derived, "HmacSHA256");
            byte[] data = read(loc);
            byte[] sig = hmac(key, data);
            System.out.println(Base64.getEncoder().encodeToString(sig));
            Arrays.fill(sig, (byte) 0);
        } finally {
            if (derived != null) Arrays.fill(derived, (byte) 0);
            Arrays.fill(master, (byte) 0);
        }
    }

    private static byte[] deriveKeyBytes(byte[] master) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(master, "HmacSHA256"));
        return mac.doFinal("PJB-UI-POLICY-SIGN-v1".getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] hmac(Key key, byte[] msg) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        return mac.doFinal(msg);
    }

    private static byte[] read(String loc) throws Exception {
        if (loc.startsWith("classpath:")) {
            String p = loc.substring("classpath:".length());
            if (p.startsWith("/")) p = p.substring(1);
            ClassLoader cl = UiPolicySignatureTool.class.getClassLoader();
            try (InputStream in = cl.getResourceAsStream(p)) {
                if (in == null) throw new IllegalArgumentException("resource not found: " + loc);
                return in.readAllBytes();
            }
        }
        return java.nio.file.Files.readAllBytes(java.nio.file.Path.of(loc));
    }
    private static String requiredArg(String[] args, int index, String name) {
        if (args == null || index < 0 || index >= args.length) {
            throw new IllegalArgumentException(name + " is required");
        }
        return Objects.requireNonNull(args[index], name);
    }

}
