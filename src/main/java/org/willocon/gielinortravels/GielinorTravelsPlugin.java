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

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.time.LocalTime;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Gielinor Travels",
	description = "A location-based puzzle and time trial plugin for Old School RuneScape!",
	tags = {"gielinor", "travels", "location", "puzzle", "time trial", "overlay", "geoguessr"}
)
public class GielinorTravelsPlugin extends Plugin
{
	public GielinorTravelsPanel panel;
	@Inject
	Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private GielinorTravelsConfig config;
	@Inject
	private ClientToolbar clientToolbar;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private LocationOverlay overlay;
	@Inject
	private SSEImageClient sseImageClient;
	private NavigationButton navButton;
	private WorldPoint destination;

	//Overlay logic
	private boolean showOverlayImage = false;
	private int ticksRemaining;

	// Found logic
	private boolean isFound = false;

	// 2 tick in a second error prevention
	private boolean tickLock = false;

	// tick counter for time until next location update
	private long tickCounter = 0;


	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);

		panel = new GielinorTravelsPanel(this, sseImageClient);

		final BufferedImage icon = ImageUtil.loadImageResource(GielinorTravelsPlugin.class, "/icon.png");

		navButton = NavigationButton.builder()
			.tooltip("Gielinor Travels")
			.icon(icon)
			.priority(5)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);

		destination = new WorldPoint(3245, 3225, 0);

		log.info("Example started!");
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		clientToolbar.removeNavigation(navButton);
		panel.leaveQueue();
		panel = null;
		log.info("Example stopped!");
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		tickCounter++;
		if (panel.isInQueue() && !tickLock && LocalTime.now().getSecond() == 3 && LocalTime.now().getMinute() % 10 == 0)
		{
			panel.setTimeUntilNext(600);
			panel.onSSE();
			tickLock = true;
		}
		else
		{
			tickLock = false;
		}
		final WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();
		if (!isFound)
		{
			if (playerPos.equals(destination))
			{
				isFound = true;
				// send some sort of packet to the server notifying it's been found
				panel.panelSendCompleted(
					client.getAccountHash() + "",
					client.getLocalPlayer().getName(),
					this
				);
			}
		}
		if (showOverlayImage)
		{
			ticksRemaining--;
			if (ticksRemaining <= 0)
			{
				showOverlayImage = false;
				isFound = false;
			}
		}
		panel.updateTimeUntilNext(tickCounter);
	}

	public void displayScore(String score)
	{
		clientThread.invoke(() -> client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "You reached the destination! You got " + score + " points!", null));
	}

	// legacy method, kept for possible future use
	public void showLocation()
	{
		clientThread.invoke(() -> {
			if (client.getLocalPlayer() == null)
			{
				return;
			}
			WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();
			String coords = String.format("X: %d, Y: %d, Plane: %d", playerPos.getX(), playerPos.getY(), playerPos.getPlane());
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Current co-ord: " + coords, null);

		});
	}

	public WorldPoint getDestination()
	{
		return destination;
	}

	public void setDestination(WorldPoint newDestination)
	{
		destination = newDestination;
	}

	public boolean getIsFound()
	{
		return isFound;
	}

	public void showOverlay()
	{
		ticksRemaining = config.ticksToDisplay()+2;
		showOverlayImage = true;
	}

	public void hideOverlay()
	{
		showOverlayImage = false;
	}

	public boolean isOverlayVisible()
	{
		return showOverlayImage;
	}

	public void changeOverlayImage(BufferedImage newImg)
	{
		overlay.setOverlayImage(newImg);
	}


	@Provides
	GielinorTravelsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GielinorTravelsConfig.class);
	}
}
