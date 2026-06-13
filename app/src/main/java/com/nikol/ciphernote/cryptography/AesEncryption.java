package com.nikol.ciphernote.Cryptography;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.security.Provider;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class AesEncryption {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;
    private final SecureRandom secureRandom;
    private final Provider provider;
    private final ThreadLocal<Cipher> cipherWrapper = new ThreadLocal<>();

    public AesEncryption() {
        this(new SecureRandom(), null);
    }

    public AesEncryption(SecureRandom secureRandom, Provider provider) {
        this.secureRandom = secureRandom;
        this.provider = provider;
    }

    public byte[] encrypt(byte[] rawEncryptionKey, byte[] rawData, @Nullable byte[] associatedData) throws Exception {
        if (rawEncryptionKey.length < 32) {
            throw new IllegalArgumentException("key length must be 32 bytes");
        }

        byte[] iv;
        byte[] encrypted;
        try {
            iv = new byte[IV_LENGTH_BYTE];
            secureRandom.nextBytes(iv);

            final Cipher cipherEnc = getCipher();
            cipherEnc.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(rawEncryptionKey, "AES"), new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            if (associatedData != null) {
                cipherEnc.updateAAD(associatedData);
            }

            encrypted = cipherEnc.doFinal(rawData);

            ByteBuffer byteBuffer = ByteBuffer.allocate(1 + iv.length + encrypted.length);
            byteBuffer.put((byte) iv.length);
            byteBuffer.put(iv);
            byteBuffer.put(encrypted);
            return byteBuffer.array();
        } catch (Exception e) {
            throw new Exception("could not encrypt", e);
        }
    }

    public byte[] decrypt(byte[] rawEncryptionKey, byte[] encryptedData, @Nullable byte[] associatedData) throws Exception {
        try {
            int initialOffset = 1;
            int ivLength = encryptedData[0];

            if (ivLength != 12 && ivLength != 16) {
                throw new IllegalStateException("Unexpected iv length");
            }

            final Cipher cipherDec = getCipher();
            cipherDec.init(Cipher.DECRYPT_MODE, new SecretKeySpec(rawEncryptionKey, "AES"),
                    new GCMParameterSpec(TAG_LENGTH_BIT, encryptedData, initialOffset, ivLength));

            if (associatedData != null) {
                cipherDec.updateAAD(associatedData);
            }

            return cipherDec.doFinal(encryptedData, initialOffset + ivLength, encryptedData.length - (initialOffset + ivLength));
        } catch (Exception e) {
            throw new Exception("could not decrypt", e);
        }
    }

    private Cipher getCipher() {
        Cipher cipher = cipherWrapper.get();
        if (cipher == null) {
            try {
                if (provider != null) {
                    cipher = Cipher.getInstance(ALGORITHM, provider);
                } else {
                    cipher = Cipher.getInstance(ALGORITHM);
                }
            } catch (Exception e) {
                throw new IllegalStateException("could not get cipher instance", e);
            }
            cipherWrapper.set(cipher);
        }
        return cipher;
    }
}
