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
package org.willocon.gielinortravels;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.coords.WorldPoint;

public class LocationLoader
{

	public final SSEImageClient imageClient;
	private BufferedImage locationImg;
	private WorldPoint destination;


	public LocationLoader(SSEImageClient imageClient, GielinorTravelsPlugin plugin, GielinorTravelsPanel panel)
	{
		this.imageClient = imageClient;
		//join the queue and wait for image and csv
		long userID = plugin.client.getAccountHash();
		String username = plugin.client.getLocalPlayer().getName();
		imageClient.joinQueue(userID + "", username);
		imageClient.listenForImageEvents(userID + "", panel);
	}

	public BufferedImage getLocationImg()
	{
		return locationImg;
	}

	public WorldPoint getDestination()
	{
		return destination;
	}


	private int[] readWorldPointCSV(String line)
	{
		List<Integer> numbers = new ArrayList<>();
		String[] values = line.split(",");

		// Parse each value as int and add to the list
		for (String value : values)
		{
			numbers.add(Integer.parseInt(value.trim()));
		}
		// Convert List<Integer> to int[]
		return numbers.stream().mapToInt(Integer::intValue).toArray();
	}

	public void loadFromServer(Runnable onComplete)
	{
		imageClient.handleEvent(() -> {
			// This callback runs after the image and CSV are loaded
			this.locationImg = imageClient.getDownloadedImage();
			String csvLine = imageClient.getDownloadedCsv();
			int[] coords = readWorldPointCSV(csvLine);
			this.destination = new WorldPoint(coords[0], coords[1], coords[2]);

			// Notify the caller that data is ready
			if (onComplete != null)
			{
				onComplete.run();
			}
		});
	}
}
