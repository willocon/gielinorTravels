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
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.time.LocalTime;

@Slf4j
@PluginDescriptor(
	name = "Example"
)
public class GielinorTravelsPlugin extends Plugin
{
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

    private NavigationButton navButton;
    public GielinorTravelsPanel panel;

    private WorldPoint destination;

    //Overlay logic
    private boolean showOverlayImage = false;
    private int ticksRemaining;

    // Found logic
    private boolean isFound = false;

    // 2 tick in a second error prevention
    private boolean tickLock = false;



    @Override
	protected void startUp()
	{
        overlayManager.add(overlay);

        panel = new GielinorTravelsPanel(this,config);

        final BufferedImage icon = ImageUtil.loadImageResource(GielinorTravelsPlugin.class, "/icon.png");

        navButton = NavigationButton.builder()
                .tooltip("Example Panel")
                .icon(icon)
                .priority(5)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);

        destination = new WorldPoint(3245,3225,0);

		log.info("Example started!");
	}

	@Override
	protected void shutDown() throws Exception
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

        if (panel.isInQueue() && !tickLock &&LocalTime.now().getSecond() == 1 && LocalTime.now().getMinute() % 10 == 0)
        {
            panel.onSSE();
            tickLock = true;
        }
        else
        {
            tickLock = false;
        }
        final WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();
        if (!isFound) {
            if (playerPos.equals(destination)) {
                isFound = true;
                // send some sort of packet to the server notifying it's been found
                try {
                    panel.panelSendCompleted(
                            client.getAccountHash() + "",
                            client.getLocalPlayer().getName(),
                            this
                    );
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        if (showOverlayImage){
            ticksRemaining--;
            if (ticksRemaining <= 0){
                showOverlayImage = false;
                isFound=false;
            }
        }
	}

    public void displayScore(String score)
    {
        clientThread.invoke(() -> client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "You reached the destination! You got " + score + " points!", null));
    }

    // legacy method, kept for possible future use
    public void showLocation()
    {
        clientThread.invoke(() -> {
            if (client.getLocalPlayer() == null) {
                return;
            }
            WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();
            String coords = String.format("X: %d, Y: %d, Plane: %d", playerPos.getX(), playerPos.getY(), playerPos.getPlane());
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Current co-ord: " + coords, null);

        });
    }

    public void setDestination(WorldPoint newDestination)
    {
        destination = newDestination;
    }

    public WorldPoint getDestination()
    {
        return destination;
    }

    public boolean getIsFound()
    {
        return isFound;
    }

    public void showOverlay()
    {
        showOverlayImage = true;
        ticksRemaining = 10;
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
