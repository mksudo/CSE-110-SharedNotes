package edu.ucsd.cse110.sharednotes.model;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.HashMap;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

public class NoteRepository {
    private final NoteDao dao;
    private final Map<String, MutableLiveData<Note>> noteCache;
    private ScheduledFuture<?> poller; // what could this be for... hmm?

    public NoteRepository(NoteDao dao) {
        this.dao = dao;
        this.noteCache = new HashMap<>();
    }

    // Synced Methods
    // ==============

    /**
     * This is where the magic happens. This method will return a LiveData object that will be
     * updated when the note is updated either locally or remotely on the server. Our activities
     * however will only need to observe this one LiveData object, and don't need to care where
     * it comes from!
     * <p>
     * This method will always prefer the newest version of the note.
     *
     * @param title the title of the note
     * @return a LiveData object that will be updated when the note is updated locally or remotely.
     */
    public LiveData<Note> getSynced(String title) {
        var note = new MediatorLiveData<Note>();

        Observer<Note> updateFromRemote = theirNote -> {
            var ourNote = note.getValue();
            if (theirNote == null) return; // do nothing
            if (ourNote == null || ourNote.version < theirNote.version) {
                upsertLocal(theirNote, false);
            }
        };

        // If we get a local update, pass it on.
        note.addSource(getLocal(title), note::postValue);
        // If we get a remote update, update the local version (triggering the above observer)
        note.addSource(getRemote(title), updateFromRemote);

        return note;
    }

    public void upsertSynced(Note note) {
        upsertLocal(note);
        upsertRemote(note);
    }

    // Local Methods
    // =============

    public LiveData<Note> getLocal(String title) {
        return dao.get(title);
    }

    public LiveData<List<Note>> getAllLocal() {
        return dao.getAll();
    }

    public void upsertLocal(Note note, boolean incrementVersion) {
        // We don't want to increment when we sync from the server, just when we save.
        if (incrementVersion) note.version = note.version + 1;
        note.version = note.version + 1;
        dao.upsert(note);
    }

    public void upsertLocal(Note note) {
        upsertLocal(note, true);
    }

    public void deleteLocal(Note note) {
        dao.delete(note);
    }

    public boolean existsLocal(String title) {
        return dao.exists(title);
    }

    // Remote Methods
    // ==============

    public LiveData<Note> getRemote(String title) {
        // Cancel any previous poller if it exists.
        if (this.poller != null && !this.poller.isCancelled()) {
            poller.cancel(true);
        }

        // Set up a background thread that will poll the server every 3 seconds.

        // You may (but don't have to) want to cache the LiveData's for each title, so that
        // you don't create a new polling thread every time you call getRemote with the same title.
        // You don't need to worry about killing background threads.

        var noteLiveData = noteCache.getOrDefault(title, null);

        if (noteLiveData != null) return noteLiveData;

        Note note = null;

        try {
            note = NoteAPI.provide().getNoteAsync(title).get(10, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException exception) {
            exception.printStackTrace();
        }

        if (note == null) note = new Note(title, "");

        noteLiveData = new MutableLiveData<>();
        noteLiveData.postValue(note);

        noteCache.put(note.title, noteLiveData);

        var executor = Executors.newSingleThreadScheduledExecutor();
        var finalNoteLiveData = noteLiveData;
        executor.scheduleAtFixedRate(() -> {
            try {
                finalNoteLiveData.postValue(NoteAPI.provide().getNoteAsync(title).get(10, TimeUnit.SECONDS));
            } catch (ExecutionException | InterruptedException | TimeoutException exception) {
                exception.printStackTrace();
            }
        }, 0, 3000, TimeUnit.MILLISECONDS);

        return noteLiveData;
    }

    public void upsertRemote(Note note) {
        NoteAPI.provide().putNoteAsync(note);
    }
}
