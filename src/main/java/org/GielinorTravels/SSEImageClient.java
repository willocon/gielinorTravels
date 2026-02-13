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
import javax.imageio.ImageIO;
import javax.inject.Inject;
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

	private final OkHttpClient client;
	private final Gson gson;

	@Inject
	public SSEImageClient(OkHttpClient client, Gson gson)
	{
		this.client = client;
		this.gson = gson;
	}

	// CHANGE THIS TO SERVER: https://gielinortravels.containers.uwcs.co.uk
	private static final String BASE_URL = "https://gielinortravels.containers.uwcs.co.uk";

	private static BufferedImage downloadedImage;
	private static String downloadedCsv;

	// Downloads image from URL
	public static BufferedImage loadImage(String url) throws IOException
	{
		return ImageIO.read(new URL(url));
	}

	// Sends POST /join
	public void joinQueue(String userId, String username)
	{
		String json = "{\"user_id\":\"" + userId + "\" , \"username\":\"" + username + "\"}";

		RequestBody body = RequestBody.create(
			MediaType.parse("application/json"), json
		);

		Request request = new Request.Builder()
			.url(BASE_URL + "/join")
			.post(body)
			.build();

		client.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				System.err.println("Failed to join queue: " + e.getMessage());
				e.printStackTrace();
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				if (response.body() != null)
				{
					System.out.println("Joined: " + response.body().string());
				}
				response.close();
			}
		});
	}

	//Sends POST /leave
	public void leaveQueue(String userId)
	{
		String json = "{\"user_id\":\"" + userId + "\"}";

		RequestBody body = RequestBody.create(
			MediaType.parse("application/json"), json
		);

		Request request = new Request.Builder()
			.url(BASE_URL + "/leave")
			.post(body)
			.build();

		client.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				System.err.println("Failed to leave queue: " + e.getMessage());
				e.printStackTrace();
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				if (response.body() != null)
				{
					System.out.println("Left: " + response.body().string());
				}
				response.close();
			}
		});
	}

	// Sends POST /completed
	public void SendCompleted(String userId, String username, GielinorTravelsPlugin plugin)
	{
		String json = "{\"user_id\":\"" + userId + "\" , \"username\":\"" + username + "\"}";

		RequestBody body = RequestBody.create(
			MediaType.parse("application/json"), json
		);

		Request request = new Request.Builder()
			.url(BASE_URL + "/completed")
			.post(body)
			.build();

		client.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				System.err.println("Failed to send completed: " + e.getMessage());
				e.printStackTrace();
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				if (response.body() != null)
				{
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
				response.close();
			}
		});
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
						JsonObject obj = gson.fromJson(json, JsonObject.class);
						if (obj.has("time"))
						{
							int secondsUntilNext = 600 - obj.get("time").getAsInt();
							System.out.println("Seconds until next event: " + secondsUntilNext);
							panel.setTimeUntilNext(secondsUntilNext);
						}
						panel.onSSE();
					}
				}
			}
		});
	}

	// Handles incoming SSE JSON event using Gson - downloads image and CSV asynchronously
	public void handleEvent(Runnable onComplete)
	{
		String imageRelative = "/images/screenshot.png";
		String csvRelative = "/images/coords.csv";

		String imageUrl = BASE_URL + imageRelative;
		String csvUrl = BASE_URL + csvRelative;

		System.out.println("Loading image: " + imageUrl);
		System.out.println("Downloading CSV: " + csvUrl);

		// Download CSV asynchronously
		Request csvRequest = new Request.Builder().url(csvUrl).build();
		client.newCall(csvRequest).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				System.err.println("Failed to download CSV: " + e.getMessage());
				e.printStackTrace();
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				if (response.body() != null)
				{
					downloadedCsv = response.body().string();
					System.out.println("Loaded csv data: " + downloadedCsv);

					// Now download the image asynchronously
					try
					{
						downloadedImage = loadImage(imageUrl);
						System.out.println("Loaded image: " + downloadedImage.getWidth() + "x" + downloadedImage.getHeight());

						// Call the completion callback
						if (onComplete != null)
						{
							onComplete.run();
						}
					}
					catch (IOException e)
					{
						System.err.println("Failed to download image: " + e.getMessage());
						e.printStackTrace();
					}
				}
				response.close();
			}
		});
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
