package edu.ucsd.cse110.sharednotes.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import edu.ucsd.cse110.sharednotes.model.Note;
import edu.ucsd.cse110.sharednotes.model.NoteDatabase;
import edu.ucsd.cse110.sharednotes.model.NoteRepository;

public class NoteViewModel extends AndroidViewModel {
    private final NoteRepository repo;
    private LiveData<Note> note;

    public NoteViewModel(@NonNull Application application) {
        super(application);
        var context = application.getApplicationContext();
        var db = NoteDatabase.provide(context);
        var dao = db.getDao();
        this.repo = new NoteRepository(dao);
    }

    public LiveData<Note> getNote(String title) {
        // The returned live data should update whenever there is a change in
        // the database, or when the server returns a newer version of the note.
        // Polling interval: 3s.
        if (note == null) {
            note = repo.getSynced(title);
        }
        return note;
    }

    public void save(Note note) {
        repo.upsertSynced(note);
    }
}
