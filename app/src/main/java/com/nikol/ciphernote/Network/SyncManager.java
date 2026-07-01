package com.nikol.ciphernote.Network;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import android.util.Base64;
import android.util.Log;

import com.nikol.ciphernote.Database.RoomDB;
import com.nikol.ciphernote.Model.Notes;
import com.nikol.ciphernote.Network.Req.DeleteNoteRequest;
import com.nikol.ciphernote.Network.Req.UpsertNoteRequest;
import com.nikol.ciphernote.Network.Res.NotesResponse;
import com.nikol.ciphernote.Cryptography.SessionManager;
import com.nikol.ciphernote.LoginActivity;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SyncManager {
    private static final String TAG = "SyncManager";
    private final Context context;
    private final RoomDB database;

    public SyncManager(Context context) {
        this.context = context;
        this.database = RoomDB.getInstance(context);
    }

    public void syncNotes(Runnable onComplete) {
        RetrofitClient.getApiService().getNotes().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<NotesResponse> call, Response<NotesResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    new Thread(() -> {
                        List<NoteDto> serverNotes = response.body().notes;
                        for (NoteDto dto : serverNotes) {
                            Notes localNote = database.mainDAO().getNoteById(dto.id);
                            if (localNote == null || dto.updatedAt > localNote.updatedAt) {
                                // update local from server
                                Notes note = new Notes();
                                note.id = dto.id;
                                note.setUser(dto.username);
                                note.updatedAt = dto.updatedAt;
                                note.deleted = dto.deleted;
                                note.title = Base64.decode(dto.titleCiphertext, Base64.NO_WRAP);
                                note.note = Base64.decode(dto.contentCiphertext, Base64.NO_WRAP);
                                note.key = Base64.decode(dto.wrappedNoteKey, Base64.NO_WRAP);
                                database.mainDAO().insert(note);
                            }
                        }
                        if (onComplete != null) onComplete.run();
                    }).start();
                } else {
                    Log.e(TAG, "Failed to fetch notes: " + response.code());

                    if (response.code() == 401 || response.code() == 403) {
                        handleExpiredToken();
                        return;
                    }

                    if (onComplete != null) onComplete.run();
                }
            }

            @Override
            public void onFailure(Call<NotesResponse> call, Throwable t) {
                Log.e(TAG, "Sync failed", t);
                if (onComplete != null) onComplete.run();
            }
        });
    }

    public void upsertNote(Notes note) {
        UpsertNoteRequest request = new UpsertNoteRequest(
                note.id,
                Base64.encodeToString(note.title, Base64.NO_WRAP),
                Base64.encodeToString(note.note, Base64.NO_WRAP),
                Base64.encodeToString(note.key, Base64.NO_WRAP),
                note.updatedAt
        );

        RetrofitClient.getApiService().upsertNote(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Failed to upsert note: " + response.code());

                    if (response.code() == 401 || response.code() == 403) {
                        handleExpiredToken();
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Upsert failed", t);
            }
        });
    }

    public void deleteNote(Notes note) {
        DeleteNoteRequest request = new DeleteNoteRequest(note.id, System.currentTimeMillis());
        RetrofitClient.getApiService().deleteNote(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Failed to delete note: " + response.code());

                    if (response.code() == 401 || response.code() == 403) {
                        handleExpiredToken();
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Delete failed", t);
            }
        });
    }

    private void handleExpiredToken() {
        SessionManager.getInstance().clear();
        RetrofitClient.setAuthToken(null);

        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        Toast.makeText(context, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
    }
}
