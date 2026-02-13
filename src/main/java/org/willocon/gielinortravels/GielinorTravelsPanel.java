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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.PluginPanel;

public class GielinorTravelsPanel extends PluginPanel
{

	private final GielinorTravelsPlugin plugin;
	private final SSEImageClient sseImageClient;
	private final JPanel textPanel = new JPanel();
	private final JButton startButton = new JButton("Join Lobby");
	private final JLabel picLabel = new JLabel();
	private final JPanel buttonPanel = new JPanel();
	private final JLabel topLabel = new JLabel("Gielinor Travels");
	private LocationLoader location;
	private boolean inQueue = false;
	private int timeUntilNext = 0;
	private final int[] clockArray = {0, 1, 0, 1, 1};

	public GielinorTravelsPanel(GielinorTravelsPlugin plugin, SSEImageClient sseImageClient)
	{
		super();
		this.plugin = plugin;
		this.sseImageClient = sseImageClient;

		setLayout(new BorderLayout(0, 10));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		topLabel.setForeground(Color.LIGHT_GRAY);
		topLabel.setHorizontalAlignment(SwingConstants.CENTER);
		textPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		textPanel.setLayout(new GridLayout(2, 1, 5, 5));
		textPanel.add(topLabel, BorderLayout.NORTH);

		JPanel picPanel = new JPanel();
		picPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		picPanel.setLayout(new GridLayout(1, 1, 5, 5));

		picPanel.add(picLabel, BorderLayout.NORTH);

		JLabel infoLabel = new JLabel("<html>Join a lobby to be matched with a random location in Gielinor.<br>" +
			"Every 10 minutes, your destination will update to a new location!</html>");
		infoLabel.setForeground(Color.LIGHT_GRAY);
		textPanel.add(infoLabel, BorderLayout.CENTER);

		buttonPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		buttonPanel.setLayout(new GridLayout(2, 1, 5, 5));


		startButton.addActionListener(this::onStartButtonClicked);


		buttonPanel.add(startButton);

		add(textPanel, BorderLayout.NORTH);
		add(picPanel, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	private void onStartButtonClicked(ActionEvent e)
	{
		location = new LocationLoader(sseImageClient, plugin, this);
		inQueue = true;


		JLabel waitLabel = new JLabel("<html>Waiting for server to send destination.</html>");
		waitLabel.setForeground(Color.LIGHT_GRAY);
		textPanel.removeAll();
		topLabel.setForeground(Color.LIGHT_GRAY);
		topLabel.setHorizontalAlignment(SwingConstants.CENTER);
		textPanel.add(topLabel, BorderLayout.NORTH);
		textPanel.add(waitLabel, BorderLayout.CENTER);
		textPanel.revalidate();
		textPanel.repaint();

		buttonPanel.removeAll();
		JButton stopButton = new JButton("Leave Lobby");
		stopButton.addActionListener(this::onStopButtonClicked);

		buttonPanel.add(stopButton);
		buttonPanel.revalidate();
		buttonPanel.repaint();
	}

	private void removeOverlayButtonClicked(ActionEvent e)
	{
		plugin.hideOverlay();
	}

	private void onStopButtonClicked(ActionEvent e)
	{
		leaveQueue();
		picLabel.removeAll();
		picLabel.setIcon(null);
		picLabel.revalidate();
		picLabel.repaint();
		JLabel infoLabel = new JLabel("<html>Join a lobby to be matched with a random location in Gielinor.<br>" +
			"Every 10 minutes, your destination will update to a new location!</html>");
		infoLabel.setForeground(Color.LIGHT_GRAY);
		textPanel.removeAll();
		topLabel.setForeground(Color.LIGHT_GRAY);
		topLabel.setHorizontalAlignment(SwingConstants.CENTER);
		textPanel.add(topLabel, BorderLayout.NORTH);
		textPanel.add(infoLabel, BorderLayout.CENTER);
		textPanel.revalidate();
		textPanel.repaint();

		buttonPanel.removeAll();
		buttonPanel.add(startButton);
		buttonPanel.revalidate();
		buttonPanel.repaint();

		plugin.setDestination(null);
	}

	public boolean isInQueue()
	{
		return inQueue;
	}

	private void setScaledImage(BufferedImage locationImg)
	{
		// Sidebar width is ~220px depending on RuneLite scaling,
		// so resize to fit the panel width while keeping aspect ratio.
		int panelWidth = 220;
		int imgWidth = locationImg.getWidth();
		int imgHeight = locationImg.getHeight();
		double scale = (double) panelWidth / imgWidth;

		int newWidth = (int) (imgWidth * scale);
		int newHeight = (int) (imgHeight * scale);

		Image scaled = locationImg.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
		picLabel.setIcon(new ImageIcon(scaled));

		picLabel.revalidate();
		picLabel.repaint();
	}

	public void onSSE()
	{
		location.loadFromServer(() -> {
			// This callback runs when the image and destination are loaded
			java.awt.EventQueue.invokeLater(() -> {
				setScaledImage(location.getLocationImg());
				plugin.setDestination(location.getDestination());
				plugin.changeOverlayImage(location.getLocationImg());
				plugin.showOverlay();
			});
		});

		//JLabel destLabel = new JLabel("<html>Travel to destination!</html>");
		String timeStr = formatTime(timeUntilNext);
		JLabel timeLabel = new JLabel("<html>Next destination update in: " + timeStr + "</html>");
		//destLabel.setForeground(Color.LIGHT_GRAY);
		textPanel.removeAll();
		topLabel.setForeground(Color.LIGHT_GRAY);
		topLabel.setHorizontalAlignment(SwingConstants.CENTER);
		textPanel.add(topLabel, BorderLayout.NORTH);
		//textPanel.add(destLabel, BorderLayout.CENTER);
		textPanel.add(timeLabel, BorderLayout.SOUTH);
		textPanel.revalidate();
		textPanel.repaint();

		buttonPanel.removeAll();
		JButton stopButton = new JButton("Leave Lobby");
		stopButton.addActionListener(this::onStopButtonClicked);
		JButton overlayButton = new JButton("Remove Overlay Image Early");
		overlayButton.addActionListener(this::removeOverlayButtonClicked);
		buttonPanel.add(stopButton);
		buttonPanel.add(overlayButton);
		buttonPanel.revalidate();
		buttonPanel.repaint();
	}

	public void panelSendCompleted(String userid, String playerName, GielinorTravelsPlugin plugin)
	{
		location.imageClient.SendCompleted(
			userid,
			playerName,
			plugin
		);
		picLabel.removeAll();
		picLabel.setIcon(null);
		picLabel.revalidate();
		picLabel.repaint();
		String timeStr = formatTime(timeUntilNext);
		JLabel waitLabel = new JLabel("<html>Next destination update in: " + timeStr + "</html>");
		waitLabel.setForeground(Color.LIGHT_GRAY);
		textPanel.removeAll();
		topLabel.setForeground(Color.LIGHT_GRAY);
		topLabel.setHorizontalAlignment(SwingConstants.CENTER);
		textPanel.add(topLabel, BorderLayout.NORTH);
		textPanel.add(waitLabel, BorderLayout.CENTER);
		textPanel.revalidate();
		textPanel.repaint();

		buttonPanel.removeAll();
		JButton stopButton = new JButton("Leave Lobby");
		stopButton.addActionListener(this::onStopButtonClicked);
		buttonPanel.add(stopButton);
		buttonPanel.revalidate();
		buttonPanel.repaint();
	}

	public void leaveQueue()
	{
		inQueue = false;
		plugin.hideOverlay();
		long userID = plugin.client.getAccountHash();
		location.imageClient.leaveQueue(userID + "");
	}

	public void setTimeUntilNext(int seconds)
	{
		timeUntilNext = seconds;
	}

	public void updateTimeUntilNext(long tick)
	{
		int index = (int) (tick % 5);
		if (clockArray[index] == 1)
		{
			timeUntilNext--;
			if (isInQueue()&& timeUntilNext >= 0)
			{
				String timeStr = formatTime(timeUntilNext);
				JLabel timeLabel = new JLabel("<html>Next destination update in: " + timeStr + "</html>");
				timeLabel.setForeground(Color.LIGHT_GRAY);
				textPanel.removeAll();
				topLabel.setForeground(Color.LIGHT_GRAY);
				topLabel.setHorizontalAlignment(SwingConstants.CENTER);
				textPanel.add(topLabel, BorderLayout.NORTH);
				textPanel.add(timeLabel, BorderLayout.CENTER);
				textPanel.revalidate();
				textPanel.repaint();
			}
		}
	}

	private String formatTime(int seconds)
	{
		int minutes = seconds / 60;
		int secs = seconds % 60;
		return String.format("%d:%02d", minutes, secs);
	}
}