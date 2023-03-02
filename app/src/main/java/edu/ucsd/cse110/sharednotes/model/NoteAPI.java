package edu.ucsd.cse110.sharednotes.model;

import android.util.Log;

import androidx.annotation.AnyThread;
import androidx.annotation.WorkerThread;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class NoteAPI {
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private volatile static NoteAPI instance = null;

    private final OkHttpClient client;

    public NoteAPI() {
        this.client = new OkHttpClient();
    }

    public static NoteAPI provide() {
        if (instance == null) {
            instance = new NoteAPI();
        }
        return instance;
    }

    /**
     * An example of sending a GET request to the server.
     * <p>
     * The /echo/{msg} endpoint always just returns {"message": msg}.
     * <p>
     * This method should can be called on a background thread (Android
     * disallows network requests on the main thread).
     */
    @WorkerThread
    public String echo(String msg) {
        // URLs cannot contain spaces, so we replace them with %20.
        String encodedMsg = msg.replace(" ", "%20");

        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/echo/" + encodedMsg)
                .method("GET", null)
                .build();

        try (var response = client.newCall(request).execute()) {
            assert response.body() != null;
            var body = response.body().string();
            Log.i("ECHO", body);
            return body;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @WorkerThread
    public Note getNote(String title) {
        String encodedTitle = title.replace(" ", "%20");

        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/notes/" + encodedTitle)
                .get()
                .build();

        try (var response = client.newCall(request).execute()) {
            assert response.body() != null;
            var body = response.body().string();
            Log.i(NoteAPI.class.getName(), "get note with title [" + title + "] => " + body);
            return Note.fromJSON(body);
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    @AnyThread
    public Future<Note> getNoteAsync(String title) {
        var executor = Executors.newSingleThreadExecutor();
        return executor.submit(() -> getNote(title));
    }

    @WorkerThread
    public void putNote(Note note) {
        String encodedTitle = note.title.replace(" ", "%20");

        String noteJson = note.toJSON();

        var requestBody = RequestBody.create(noteJson, JSON);

        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/notes/" + encodedTitle)
                .put(requestBody)
                .build();

        try (var response = client.newCall(request).execute()) {
            assert response.body() != null;
            var body = response.body().string();
            Log.i(NoteAPI.class.getName(), "put note " + noteJson + "; response is " + body);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @AnyThread
    public void putNoteAsync(Note note) {
        var executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> putNote(note));
    }
}
