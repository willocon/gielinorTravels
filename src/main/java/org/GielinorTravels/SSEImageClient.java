/*
 * Copyright (c) 2026, Will O'Connor <william.oconnor13@hotmail.co.uk>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.GielinorTravels;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSource;

public class SSEImageClient
{

	private static final OkHttpClient client = new OkHttpClient.Builder()
		.build();
	private static final Gson gson = new Gson();

	// CHANGE THIS TO SERVER: https://gielinortravels.containers.uwcs.co.uk
	private static final String BASE_URL = "https://gielinortravels.containers.uwcs.co.uk";

	private static BufferedImage downloadedImage;
	private static String downloadedCsv;

	// Downloads image from URL
	public static BufferedImage loadImage(String url) throws IOException
	{
		return ImageIO.read(new URL(url));
	}

	public static String loadCsv(String url) throws IOException
	{
		Request request = new Request.Builder().url(url).build();
		Response response = client.newCall(request).execute();

		if (!response.isSuccessful())
		{
			throw new IOException("Failed to download CSV: " + response);
		}

		byte[] data = response.body().bytes();
		return new String(data, StandardCharsets.UTF_8);
	}

	// Sends POST /join
	public void joinQueue(String userId, String username) throws Exception
	{
		String json = "{\"user_id\":\"" + userId + "\" , \"username\":\"" + username + "\"}";

		RequestBody body = RequestBody.create(
			MediaType.parse("application/json"), json
		);

		Request request = new Request.Builder()
			.url(BASE_URL + "/join")
			.post(body)
			.build();

		try (Response response = client.newCall(request).execute())
		{
			assert response.body() != null;
			System.out.println("Joined: " + response.body().string());
		}
	}

	//Sends POST /leave
	public void leaveQueue(String userId) throws Exception
	{
		String json = "{\"user_id\":\"" + userId + "\"}";

		RequestBody body = RequestBody.create(
			MediaType.parse("application/json"), json
		);

		Request request = new Request.Builder()
			.url(BASE_URL + "/leave")
			.post(body)
			.build();

		try (Response response = client.newCall(request).execute())
		{
			assert response.body() != null;
			System.out.println("Left: " + response.body().string());
		}
	}

	// Sends POST /completed
	public void SendCompleted(String userId, String username, GielinorTravelsPlugin plugin) throws Exception
	{
		String json = "{\"user_id\":\"" + userId + "\" , \"username\":\"" + username + "\"}";

		RequestBody body = RequestBody.create(
			MediaType.parse("application/json"), json
		);

		Request request = new Request.Builder()
			.url(BASE_URL + "/completed")
			.post(body)
			.build();

		try (Response response = client.newCall(request).execute())
		{
			assert response.body() != null;
			String responseJson = response.body().string();
			System.out.println("Completed: " + responseJson);
			JsonObject obj = gson.fromJson(responseJson, JsonObject.class);

			if (!obj.has("score"))
			{
				System.out.println("No score in response, user already completed");
				return;
			}
			String score = obj.get("score").getAsString();
			System.out.println("Score received: " + score);
			plugin.displayScore(score);

		}
	}

	// Opens SSE event stream
	public void listenForImageEvents(String userId, GielinorTravelsPanel panel)
	{
		Request request = new Request.Builder()
			.url(BASE_URL + "/events?user_id=" + userId)
			.get()
			.build();

		client.newCall(request).enqueue(new Callback()
		{

			@Override
			public void onFailure(Call call, IOException e)
			{
				System.err.println("SSE connection failed: " + e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				System.out.println("Connected to SSE... waiting for image events");

				assert response.body() != null;
				BufferedSource source = response.body().source();

				while (!source.exhausted())
				{
					String line = source.readUtf8Line();

					if (line == null)
					{
						continue;
					}

					if (line.startsWith("data: "))
					{
						String json = line.substring(6);
						System.out.println("Received SSE: " + json);

						//handleEvent();
						panel.onSSE();
					}
				}
			}
		});
	}

	// Handles incoming SSE JSON event using Gson
	public void handleEvent()
	{
		try
		{

			String imageRelative = "/images/screenshot.png";
			String csvRelative = "/images/coords.csv";

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


		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public BufferedImage getDownloadedImage()
	{
		return downloadedImage;
	}

	public String getDownloadedCsv()
	{
		return downloadedCsv;
	}


}
