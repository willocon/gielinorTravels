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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.Perspective;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public class LocationOverlay extends Overlay
{

	private final GielinorTravelsPlugin plugin;
	private BufferedImage image;

	@Inject
	private GielinorTravelsConfig config;

	@Inject
	public LocationOverlay(GielinorTravelsPlugin plugin)
	{
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);

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

	public void setOverlayImage(BufferedImage newImg)
	{
		int width = config.overlaySize();
		image = scaleImage(newImg, width, width);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		WorldView wv = plugin.client.getLocalPlayer().getWorldView();
		WorldPoint dest = plugin.getDestination();
		if (!plugin.getIsFound() && plugin.panel.isInQueue())
		{
			if (wv.contains(dest))
			{
				Tile tile = wv.getScene().getTiles()[dest.getPlane()][dest.getX() - wv.getBaseX()][dest.getY() - wv.getBaseY()];
				renderDestTile(g, tile.getLocalLocation());
			}
		}

		if (!plugin.isOverlayVisible() || image == null)
		{
			return null;
		}

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
		if (destPoint == null)
		{
			return;
		}

		final Polygon poly = Perspective.getCanvasTilePoly(plugin.client, destPoint);
		if (poly == null)
		{
			return;
		}
		OverlayUtil.renderPolygon(graphics, poly, Color.YELLOW);
	}
}
