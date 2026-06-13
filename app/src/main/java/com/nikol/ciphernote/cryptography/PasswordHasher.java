package com.nikol.ciphernote.Cryptography;

import android.util.Base64;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public final class PasswordHasher {
    private static final SecureRandom RNG = new SecureRandom();

    private static final int MEMORY_KIB = 32768; // 32 MiB
    private static final int ITERATIONS = 3;
    private static final int PARALLELISM = 1;

    private static final int SALT_LEN = 16;
    private static final int HASH_LEN = 32;

    private PasswordHasher() {}

    public static String hash(String password) {
        byte[] salt = new byte[SALT_LEN];
        RNG.nextBytes(salt);

        byte[] hash = argon2id(password, salt, MEMORY_KIB, ITERATIONS, PARALLELISM, HASH_LEN);

        String saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP);
        String hashB64 = Base64.encodeToString(hash, Base64.NO_WRAP);

        return "$argon2id$v=19$m=" + MEMORY_KIB + ",t=" + ITERATIONS + ",p=" + PARALLELISM
                + "$" + saltB64
                + "$" + hashB64;
    }

    public static byte[] deriveKey(String password, byte[] salt) {
        return argon2id(password, salt, MEMORY_KIB, ITERATIONS, PARALLELISM, 32);
    }

    public static String generateDeterministicServerHash(String password, String username) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] salt = digest.digest(username.getBytes(StandardCharsets.UTF_8));
            byte[] truncatedSalt = new byte[SALT_LEN];
            System.arraycopy(salt, 0, truncatedSalt, 0, SALT_LEN);

            byte[] hash = argon2id(password, truncatedSalt, MEMORY_KIB, ITERATIONS, PARALLELISM, HASH_LEN);
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (Exception e) {
            throw new RuntimeException("Could not generate server hash", e);
        }
    }

    private static byte[] argon2id(
            String password,
            byte[] salt,
            int memoryKiB,
            int iterations,
            int parallelism,
            int outLen
    ) {
        Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withMemoryAsKB(memoryKiB)
                .withIterations(iterations)
                .withParallelism(parallelism)
                .withSalt(salt)
                .build();

        Argon2BytesGenerator gen = new Argon2BytesGenerator();
        gen.init(params);

        byte[] out = new byte[outLen];
        byte[] pwdBytes = password.getBytes(StandardCharsets.UTF_8);
        gen.generateBytes(pwdBytes, out);

        java.util.Arrays.fill(pwdBytes, (byte) 0);
        return out;
    }
}
