package org.GielinorTravels;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;

public class LocationOverlay extends Overlay {

    private final GielinorTravelsPlugin plugin;
    private BufferedImage image;

    @Inject
    public LocationOverlay(GielinorTravelsPlugin plugin)
    {
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT); // or DYNAMIC, ABOVE_WIDGETS, etc.
        setLayer(OverlayLayer.ABOVE_WIDGETS);

        BufferedImage raw = ImageUtil.loadImageResource(GielinorTravelsPlugin.class, "/locations/1/1.png");
        setOverlayImage(raw);
    }

    public void setOverlayImage(BufferedImage newImg){
        image = scaleImage(newImg, 500, 500);
    }

    private static BufferedImage scaleImage(BufferedImage src, int width, int height)
    {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(src, 0, 0, width, height, null);
        g2.dispose();
        return scaled;
    }

    @Override
    public Dimension render(Graphics2D g)
    {
        if (!plugin.isOverlayVisible() || image == null)
            return null;

        int x = 20;
        int y = 20;

        g.setColor(new Color(0, 0, 0, 120)); // black, 120 alpha
        g.fillRoundRect(x - 4, y - 4, image.getWidth() + 8, image.getHeight() + 8, 12, 12);

        g.drawImage(image, x, y, null);

        g.setColor(Color.YELLOW);
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(x - 4, y - 4, image.getWidth() + 8, image.getHeight() + 8, 12, 12);

        return new Dimension(image.getWidth(), image.getHeight());
    }
}
