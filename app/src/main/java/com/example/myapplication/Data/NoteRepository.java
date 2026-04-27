package com.example.myapplication.Data;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import com.example.myapplication.Model.Note;

import java.util.ArrayList;
import java.util.List;

public class NoteRepository extends SQLiteOpenHelper {

    private static final String DB_NAME = "notes.db";
    // CHANGEMENT : Passage de la version 2 à 3 pour ajouter la colonne isFavorite
    private static final int DB_VERSION = 3;

    public NoteRepository(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE notes(" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "content TEXT," +
                        "summary TEXT," +
                        "keywords TEXT," +
                        "createdAt INTEGER," +
                        "isFavorite INTEGER DEFAULT 0)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE notes ADD COLUMN isFavorite INTEGER DEFAULT 0");
        }
    }

    public long insert(Note note) {
        ContentValues cv = new ContentValues();
        cv.put("content", note.content);
        cv.put("summary", note.summary);
        cv.put("keywords", note.keywords);
        cv.put("createdAt", note.createdAt);
        cv.put("isFavorite", note.isFavorite ? 1 : 0);
        return getWritableDatabase().insert("notes", null, cv);
    }

    public void delete(long id) {
        getWritableDatabase().delete("notes", "id=?", new String[]{String.valueOf(id)});
    }

    public void toggleFavorite(long id, boolean isFavorite) {
        ContentValues cv = new ContentValues();
        cv.put("isFavorite", isFavorite ? 1 : 0);
        getWritableDatabase().update("notes", cv, "id=?", new String[]{String.valueOf(id)});
    }

    public List<Note> getAll() {
        return search("");
    }

    public List<Note> getFavorites() {
        List<Note> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM notes WHERE isFavorite = 1 ORDER BY createdAt DESC", null);
        while (c.moveToNext()) {
            list.add(cursorToNote(c));
        }
        c.close();
        return list;
    }

    public List<Note> search(String query) {
        List<Note> list = new ArrayList<>();
        String selection = null;
        String[] selectionArgs = null;

        if (query != null && !query.isEmpty()) {
            selection = "content LIKE ? OR summary LIKE ? OR keywords LIKE ?";
            String wildQuery = "%" + query + "%";
            selectionArgs = new String[]{wildQuery, wildQuery, wildQuery};
        }

        Cursor c = getReadableDatabase().query(
                "notes", 
                null, 
                selection, 
                selectionArgs, 
                null, 
                null, 
                "createdAt DESC"
        );

        while (c.moveToNext()) {
            list.add(cursorToNote(c));
        }
        c.close();
        return list;
    }

    public Note getById(long id) {
        Cursor c = getReadableDatabase()
                .rawQuery("SELECT * FROM notes WHERE id=?", new String[]{String.valueOf(id)});
        if (!c.moveToFirst()) return null;
        Note n = cursorToNote(c);
        c.close();
        return n;
    }

    private Note cursorToNote(Cursor c) {
        Note n = new Note();
        n.id = c.getLong(0);
        n.content = c.getString(1);
        n.summary = c.getString(2);
        n.keywords = c.getString(3);
        n.createdAt = c.getLong(4);
        n.isFavorite = c.getInt(5) == 1;
        return n;
    }
}