package com.nikol.ciphernote.Database;

import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.nikol.ciphernote.Model.Notes;
import com.nikol.ciphernote.Model.Profiles;

import java.util.List;

@Dao
public interface MainDAO {
    @Insert(onConflict = REPLACE)
    void insert(Notes notes);

    @Insert(onConflict = REPLACE)
    void insert_profile(Profiles profile);

    @Query("SELECT * FROM notes WHERE user = :username AND deleted = 0 ORDER BY updated_at DESC")
    List<Notes> getAll(String username);

    @Query("SELECT * FROM notes WHERE id = :id")
    Notes getNoteById(String id);

    @Query("SELECT * FROM profiles WHERE username = :username")
    Profiles getProfile(String username);

}
