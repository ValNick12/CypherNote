package com.nikol.ciphernote;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.nikol.ciphernote.Model.Notes;
import com.nikol.ciphernote.Model.Profiles;
import com.nikol.ciphernote.Cryptography.SessionManager;

public class NotesTakingActivity extends AppCompatActivity {

    private EditText editText_title, editText_notes;
    private Notes note;
    private boolean isOldNote = false;
    private Profiles profile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notes_taking);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageView imageView_save = findViewById(R.id.imageView_save);
        editText_title = findViewById(R.id.editText_title);
        editText_notes = findViewById(R.id.editText_notes);
        profile = (Profiles) getIntent().getSerializableExtra("user");

        Notes oldNote = (Notes) getIntent().getSerializableExtra("old_note");
        if (oldNote != null) {
            note = oldNote;
            editText_title.setText(note.getTitle());
            editText_notes.setText(note.getNote());
            isOldNote = true;
        } else {
            note = new Notes();
            note.initializeNewNote();
            isOldNote = false;
        }

        imageView_save.setOnClickListener(v -> {
            if (SessionManager.getInstance().getMasterKey() == null) {
                Toast.makeText(NotesTakingActivity.this, "Session expired! Please log in again.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            String title = editText_title.getText().toString();
            String note_text = editText_notes.getText().toString();
            if (note_text.isEmpty()) {
                Toast.makeText(NotesTakingActivity.this, "Please write a note!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isOldNote && note == null){
                note = new Notes();
                note.initializeNewNote();
            }

            note.setTitle(title);
            note.setNote(note_text);
            note.updatedAt = System.currentTimeMillis();
            
            if (profile != null) {
                note.setUser(profile.getUsername());
            } else {
                Log.e("NotesTakingActivity", "Profile is null, cannot set user for note");
            }

            Intent intent = new Intent();
            intent.putExtra("note", note);
            setResult(NotesTakingActivity.RESULT_OK, intent);
            finish();
        });
    }
}
