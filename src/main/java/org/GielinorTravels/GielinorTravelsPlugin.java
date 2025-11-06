package org.GielinorTravels;

import com.google.inject.Provides;
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
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;

@Slf4j
@PluginDescriptor(
	name = "Example"
)
public class GielinorTravelsPlugin extends Plugin
{
	@Inject
	private Client client;

    @Inject
    private ClientThread clientThread;

	@Inject
	private GielinorTravelsConfig config;

    @Inject
    private ClientToolbar clientToolbar;

    private NavigationButton navButton;
    private GielinorTravelsPanel panel;

	@Override
	protected void startUp() throws Exception
	{
        panel = new GielinorTravelsPanel(this,config);

        final BufferedImage icon = ImageUtil.loadImageResource(GielinorTravelsPlugin.class, "/icon.png");

        navButton = NavigationButton.builder()
                .tooltip("Example Panel")
                .icon(icon)
                .priority(5)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);

		log.info("Example started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
        clientToolbar.removeNavigation(navButton);
        panel = null;
        log.info("Example stopped!");
	}
    final WorldPoint destination = new WorldPoint(3245,3225,0);

	@Subscribe
	public void onGameTick(GameTick tick)
    {
            final WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();
            if (playerPos.equals(destination))
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Reached destination Tile!: " + playerPos, null);
	}

    public void showLocation() {
        clientThread.invoke(() -> {
            if (client.getLocalPlayer() == null) {
//            panel.setLocationText("Player not found.");
                return;
            }
            WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();
            String coords = String.format("X: %d, Y: %d, Plane: %d", playerPos.getX(), playerPos.getY(), playerPos.getPlane());
//        panel.setLocationText(coords);
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Current co-ord: " + coords, null);

        });
    }

	@Provides
    GielinorTravelsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GielinorTravelsConfig.class);
	}
}
