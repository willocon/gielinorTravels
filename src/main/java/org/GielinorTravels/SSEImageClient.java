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
            .connectTimeout(0, java.util.concurrent.TimeUnit.MINUTES)
            .readTimeout(0, java.util.concurrent.TimeUnit.MINUTES)
            .writeTimeout(0, java.util.concurrent.TimeUnit.MINUTES)
            .callTimeout(0, java.util.concurrent.TimeUnit.MINUTES)
            .build();
    private static final Gson gson = new Gson();

    // CHANGE THIS TO SERVER: https://gielinortravels.containers.uwcs.co.uk
    private static final String BASE_URL = "https://gielinortravels.containers.uwcs.co.uk";

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

    //Sends POST /leave
    public void leaveQueue(String userId) throws Exception {
        String json = "{\"user_id\":\"" + userId + "\"}";

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"), json
        );

        Request request = new Request.Builder()
                .url(BASE_URL + "/leave")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            assert response.body() != null;
            System.out.println("Left: " + response.body().string());
        }
    }

    // Sends POST /completed
    public void SendCompleted(String userId, String username, GielinorTravelsPlugin plugin) throws Exception {
        String json = "{\"user_id\":\"" + userId + "\" , \"username\":\"" + username + "\"}";

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"), json
        );

        Request request = new Request.Builder()
                .url(BASE_URL + "/completed")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            assert response.body() != null;
            String responseJson = response.body().string();
            System.out.println("Completed: " + responseJson);
            JsonObject obj = gson.fromJson(responseJson, JsonObject.class);

            if(!obj.has("score")) {
                System.out.println("No score in response, user already completed");
                return;
            }
            String score = obj.get("score").getAsString();
            System.out.println("Score received: " + score);
            plugin.displayScore(score);

        }
    }

    // Opens SSE event stream
    public void listenForImageEvents(String userId, GielinorTravelsPanel panel) {
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
                        panel.onTenMinute();
                    }
                }
            }
        });
    }

    // Handles incoming SSE JSON event using Gson
    private void handleEvent(String json) {
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


        } catch (Exception e) {
            e.printStackTrace();
        }
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
        return downloadedCsv;
    }


}
