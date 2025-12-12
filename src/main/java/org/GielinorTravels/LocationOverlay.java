package org.GielinorTravels;

import net.runelite.api.Perspective;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayUtil;

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
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);

    }

    public void setOverlayImage(BufferedImage newImg)
    {
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
        WorldView wv = plugin.client.getLocalPlayer().getWorldView();
        WorldPoint dest = plugin.getDestination();
        if(!plugin.getIsFound()&& plugin.panel.isInQueue())
            if (wv.contains(dest)) {
                Tile tile = wv.getScene().getTiles()[dest.getPlane()][dest.getX() - wv.getBaseX()][dest.getY() - wv.getBaseY()];
                renderDestTile(g, tile.getLocalLocation());
            }

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

        return null;
    }

    private void renderDestTile(final Graphics2D graphics, final LocalPoint destPoint)
    {
        if (destPoint == null) {
            return;
        }

        final Polygon poly = Perspective.getCanvasTilePoly(plugin.client, destPoint);
        if (poly == null) {
            return;
        }
        OverlayUtil.renderPolygon(graphics, poly, Color.YELLOW);
    }
}
