package com.example.myapplication.Data;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import com.example.myapplication.Model.Note;

import java.util.ArrayList;
import java.util.List;

public class NoteRepository extends SQLiteOpenHelper {

    private static final String DB_NAME = "notes.db";
    private static final int DB_VERSION = 2;

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
                        "createdAt INTEGER)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS notes");
        onCreate(db);
    }

    public long insert(Note note) {
        ContentValues cv = new ContentValues();
        cv.put("content", note.content);
        cv.put("summary", note.summary);
        cv.put("keywords", note.keywords);
        cv.put("createdAt", note.createdAt);
        return getWritableDatabase().insert("notes", null, cv);
    }

    public void delete(long id) {
        getWritableDatabase().delete("notes", "id=?", new String[]{String.valueOf(id)});
    }

    public List<Note> getAll() {
        return search("");
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
            Note n = new Note();
            n.id = c.getLong(0);
            n.content = c.getString(1);
            n.summary = c.getString(2);
            n.keywords = c.getString(3);
            n.createdAt = c.getLong(4);
            list.add(n);
        }
        c.close();
        return list;
    }

    public Note getById(long id) {
        Cursor c = getReadableDatabase()
                .rawQuery("SELECT * FROM notes WHERE id=?", new String[]{String.valueOf(id)});
        if (!c.moveToFirst()) return null;

        Note n = new Note();
        n.id = c.getLong(0);
        n.content = c.getString(1);
        n.summary = c.getString(2);
        n.keywords = c.getString(3);
        n.createdAt = c.getLong(4);
        c.close();
        return n;
    }
}