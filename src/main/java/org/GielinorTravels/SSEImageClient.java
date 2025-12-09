package org.GielinorTravels;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import okio.BufferedSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class SSEImageClient {

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(11, java.util.concurrent.TimeUnit.MINUTES)
            .readTimeout(11, java.util.concurrent.TimeUnit.MINUTES)
            .writeTimeout(11, java.util.concurrent.TimeUnit.MINUTES)
            .callTimeout(11, java.util.concurrent.TimeUnit.MINUTES)
            .build();
    private static final Gson gson = new Gson();

    // CHANGE THIS TO SERVER
    private static final String BASE_URL = "http://127.0.0.1:5000";

    private static BufferedImage downloadedImage;
    private static String downloadedCsv;

    // Sends POST /join
    public void joinQueue(String userId) throws Exception {
        String json = "{\"user_id\":\"" + userId + "\"}";

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"), json
                );

        Request request = new Request.Builder()
                .url(BASE_URL + "/join")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            assert response.body() != null;
            System.out.println("Joined: " + response.body().string());
        }
    }

    // Sends POST /completed
    public void SendCompleted(String userId, long ticks, String username) throws Exception {
        String json = "{\"user_id\":\"" + userId + "\" , \"ticks\":\"" + ticks + "\", \"username\":\"" + username + "\"}";

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"), json
        );

        Request request = new Request.Builder()
                .url(BASE_URL + "/completed")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            assert response.body() != null;
            System.out.println("Completed: " + response.body().string());
        }
    }

    // Opens SSE event stream
    public void listenForImageEvents(String userId) {
        Request request = new Request.Builder()
                .url(BASE_URL + "/events?user_id=" + userId)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                System.err.println("SSE connection failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                System.out.println("Connected to SSE... waiting for image events");

                assert response.body() != null;
                BufferedSource source = response.body().source();

                while (!source.exhausted()) {
                    String line = source.readUtf8Line();

                    if (line == null) continue;

                    if (line.startsWith("data: ")) {
                        String json = line.substring(6);
                        System.out.println("Received SSE: " + json);

                        handleEvent(json);
                    }
                }
            }
        });
    }

    public static boolean eventHandled = false;
    // Handles incoming SSE JSON event using Gson
    private static void handleEvent(String json) {
        try {
            JsonObject obj = gson.fromJson(json, JsonObject.class);

            String imageRelative = obj.get("image_url").getAsString();
            String csvRelative = obj.get("csv_url").getAsString();

            String imageUrl = BASE_URL + imageRelative;
            String csvUrl = BASE_URL + csvRelative;

            System.out.println("Loading image: " + imageUrl);
            System.out.println("Downloading CSV: " + csvUrl);

            // image
            downloadedImage = loadImage(imageUrl);
            System.out.println("Loaded image: " + downloadedImage.getWidth() + "x" + downloadedImage.getHeight());

            // csv
            downloadedCsv = loadCsv(csvUrl);
            System.out.println("Loaded csv data: " + downloadedCsv);

            eventHandled = true;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean getEventHandled() {
        return eventHandled;
    }


    // Downloads image from URL
    public static BufferedImage loadImage(String url) throws IOException {
        return ImageIO.read(new URL(url));
    }


    public static String loadCsv(String url) throws IOException {
        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new IOException("Failed to download CSV: " + response);
        }

        byte[] data = response.body().bytes();
        return new String(data, StandardCharsets.UTF_8);
    }

    public BufferedImage getDownloadedImage() { return downloadedImage; }

    public String getDownloadedCsv()
    {
        eventHandled = false;
        return downloadedCsv;
    }

}
