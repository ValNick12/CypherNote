package com.nikol.ciphernote.Database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.nikol.ciphernote.Model.Notes;
import com.nikol.ciphernote.Model.Profiles;

@Database(entities = {Profiles.class, Notes.class}, version = 5, exportSchema = false)
public abstract class RoomDB extends RoomDatabase {
    private static volatile RoomDB database;
    private static final String DATABASE_NAME = "CipherNote";

    public static RoomDB getInstance(Context context) {
        if (database == null) {
            synchronized (RoomDB.class) {
                if (database == null) {
                    database = Room.databaseBuilder(context.getApplicationContext(),
                                    RoomDB.class, DATABASE_NAME)
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return database;
    }

    public abstract MainDAO mainDAO();
}



