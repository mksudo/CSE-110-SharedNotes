package edu.ucsd.cse110.sharednotes.model;

import android.util.Log;

import com.google.gson.Gson;

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
     *
     * The /echo/{msg} endpoint always just returns {"message": msg}.
     */
    public void echo(String msg) {
        // URLs cannot contain spaces, so we replace them with %20.
        msg = msg.replace(" ", "%20");

        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/echo/" + msg)
                .method("GET", null)
                .build();

        try (var response = client.newCall(request).execute()) {
            assert response.body() != null;
            var body = response.body().string();
            Log.i("ECHO", body);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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

    public void putNote(Note note) {
        String encodedTitle = note.title.replace(" ", "%20");

        String noteJson = new Gson().toJson(note);

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
}
