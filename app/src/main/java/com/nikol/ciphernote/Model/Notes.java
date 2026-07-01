package com.nikol.ciphernote.Model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.nikol.ciphernote.Cryptography.AesEncryption;
import com.nikol.ciphernote.Cryptography.SessionManager;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.UUID;

@Entity(tableName = "notes")
public class Notes implements Serializable {
    private static final AesEncryption aesEncryption = new AesEncryption();

    @PrimaryKey
    @NonNull
    public String id = "";
    @ColumnInfo(name = "title")
    public byte[] title;
    @ColumnInfo(name = "note")
    public byte[] note;
    @ColumnInfo(name = "user")
    String user = "";
    @ColumnInfo(name = "key")
    public byte[] key;
    @ColumnInfo(name = "updated_at")
    public long updatedAt;
    @ColumnInfo(name = "deleted")
    public int deleted = 0;
    @Ignore
    private transient byte[] cachedDecryptedKey;

    public Notes(){
    }

    public void initializeNewNote() {
        this.id = UUID.randomUUID().toString();
        this.updatedAt = System.currentTimeMillis();

        byte[] masterKey = SessionManager.getInstance().getMasterKey();
        if (masterKey == null) return;

        byte[] rawKey = generateRawKey();
        try {
            this.key = aesEncryption.encrypt(masterKey, rawKey, null);
            this.cachedDecryptedKey = rawKey;
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize note key", e);
        }
    }

    private byte[] getDecryptedKey() {
        if (cachedDecryptedKey != null) return cachedDecryptedKey;
        if (this.key == null) return null;

        byte[] masterKey = SessionManager.getInstance().getMasterKey();
        if (masterKey == null) return null;

        try {
            cachedDecryptedKey = aesEncryption.decrypt(masterKey, this.key, null);
            return cachedDecryptedKey;
        } catch (Exception e) {
            return null;
        }
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        if (title == null) {
            this.title = null;
            return;
        }
        try {
            byte[] rawKey = getDecryptedKey();
            if (rawKey == null) throw new RuntimeException("Key not available");
            this.title = aesEncryption.encrypt(rawKey, title.getBytes(StandardCharsets.UTF_8), null);
        } catch (Exception e) {
            throw new RuntimeException("Didn't set title", e);
        }
    }

    public String getTitle() {
        if (this.title == null) return "";
        try {
            byte[] rawKey = getDecryptedKey();
            if (rawKey == null) return "Locked"; 
            return new String(aesEncryption.decrypt(rawKey, this.title, null), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return new String(this.title, StandardCharsets.UTF_8);
        }
    }

    public void setNote(String note) {
        if (note == null) {
            this.note = null;
            return;
        }
        try {
            byte[] rawKey = getDecryptedKey();
            if (rawKey == null) throw new RuntimeException("Key not available");
            this.note = aesEncryption.encrypt(rawKey, note.getBytes(StandardCharsets.UTF_8), null);
        } catch (Exception e) {
            throw new RuntimeException("Didn't set note", e);
        }
    }

    public String getNote() {
        if (this.note == null) return "";
        try {
            byte[] rawKey = getDecryptedKey();
            if (rawKey == null) return "Locked";
            return new String(aesEncryption.decrypt(rawKey, this.note, null), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return new String(this.note, StandardCharsets.UTF_8);
        }
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public static byte[] generateRawKey() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[16];
        secureRandom.nextBytes(key);
        return key;
    }
}
