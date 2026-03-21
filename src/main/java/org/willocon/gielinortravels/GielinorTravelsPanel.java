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

import org.apache.commons.lang3.StringUtils;
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
import net.runelite.client.util.LinkBrowser;

public class GielinorTravelsPanel extends PluginPanel
{

	private final GielinorTravelsPlugin plugin;
	private final SSEImageClient sseImageClient;
	private final JPanel textPanel = new JPanel();
	private final JButton startButton = new JButton("Join Game");
	private final JButton linkButton = new JButton("View Leaderboard");
	private final JLabel picLabel = new JLabel();
	private final JPanel buttonPanel = new JPanel();
	private final JLabel topLabel = new JLabel("<html><style> p {text-align: center;}h1 {text-align: center;}</style><h1><u>Gielinor Travels</u></h1><p>Join a game to be matched with a random location in Gielinor.<br>Every 10 minutes, your destination will update to a new location!<br>The aim of the game is to be the first to reach the location shown in the image.<br>The quicker you reach the destination, the more points you will receive!<br>You can see your scores on the online leaderboard by clicking the link button below!</p></html>");
	private final JLabel waitLabel = new JLabel("<html><style> p {text-align: center;}h1 {text-align: center;}</style><h1><u>Gielinor Travels</u></h1><p>Waiting for server to send destination...<br>If this takes a while, leave and rejoin.</p></html>");
	private LocationLoader location;
	private boolean inQueue = false;
	private int timeUntilNext = 0;
	private int gameDifficulty = 0;
	// clock array turns 5 ticks into 3 second intervals for when to update the time until next label
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
		textPanel.setLayout(new GridLayout(1, 1, 5, 5));
		textPanel.add(topLabel, BorderLayout.NORTH);

		JPanel picPanel = new JPanel();
		picPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		picPanel.setLayout(new GridLayout(1, 1, 5, 5));

		picPanel.add(picLabel, BorderLayout.NORTH);

		buttonPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		buttonPanel.setLayout(new GridLayout(3, 1, 5, 5));


		startButton.addActionListener(this::onStartButtonClicked);
		linkButton.addActionListener(e -> LinkBrowser.browse("https://gielinortravels.containers.uwcs.co.uk/leaderboard"));


		buttonPanel.add(startButton);
		buttonPanel.add(linkButton);

		add(textPanel, BorderLayout.NORTH);
		add(picPanel, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	private void onStartButtonClicked(ActionEvent e)
	{
		location = new LocationLoader(sseImageClient, plugin, this);
		inQueue = true;

		waitLabel.setForeground(Color.LIGHT_GRAY);
		textPanel.removeAll();
		textPanel.add(waitLabel, BorderLayout.NORTH);
		textPanel.revalidate();
		textPanel.repaint();

		buttonPanel.removeAll();
		JButton stopButton = new JButton("Leave Game");
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
		textPanel.removeAll();
		topLabel.setForeground(Color.LIGHT_GRAY);
		topLabel.setHorizontalAlignment(SwingConstants.CENTER);
		textPanel.add(topLabel, BorderLayout.NORTH);
		textPanel.revalidate();
		textPanel.repaint();

		buttonPanel.removeAll();
		buttonPanel.add(startButton);
		buttonPanel.add(linkButton);
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

		String timeStr = formatTime(timeUntilNext);
		JLabel timeLabel = new JLabel("<html><style> p {text-align: center;}h1 {text-align: center;}</style><h1><u>Gielinor Travels</u></h1><p>Next destination update in: " + timeStr + "</p><p>Difficulty: " + StringUtils.repeat("*",gameDifficulty) +"</p></html>");

		textPanel.removeAll();
		textPanel.add(timeLabel, BorderLayout.NORTH);
		textPanel.revalidate();
		textPanel.repaint();

		buttonPanel.removeAll();
		JButton stopButton = new JButton("Leave Game");
		stopButton.addActionListener(this::onStopButtonClicked);
		JButton overlayButton = new JButton("Remove Overlay Image Early");
		overlayButton.addActionListener(this::removeOverlayButtonClicked);
		JButton showImageAgainButton = new JButton("Show Overlay Image Again");
		showImageAgainButton.addActionListener(e -> plugin.showOverlay());
		buttonPanel.add(stopButton);
		buttonPanel.add(overlayButton);
		buttonPanel.add(showImageAgainButton);
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
		JLabel timeLabel = new JLabel("<html><style> p {text-align: center;}h1 {text-align: center;}</style><h1><u>Gielinor Travels</u></h1><p>Next destination update in: " + timeStr + "</p></html>");

		waitLabel.setForeground(Color.LIGHT_GRAY);
		textPanel.removeAll();
		textPanel.add(timeLabel, BorderLayout.NORTH);
		textPanel.revalidate();
		textPanel.repaint();

		buttonPanel.removeAll();
		JButton stopButton = new JButton("Leave Game");
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

	public void setDifficulty(int difficulty)
	{
		gameDifficulty = difficulty;
	}

	public void updateTimeUntilNext(long tick)
	{
		int index = (int) (tick % 5);
		if (clockArray[index] == 1)
		{
			timeUntilNext--;
			if (isInQueue() && timeUntilNext >= 0)
			{
				String timeStr = formatTime(timeUntilNext);
				JLabel timeLabel = new JLabel("<html><style> p {text-align: center;}h1 {text-align: center;}</style><h1><u>Gielinor Travels</u></h1><p>Next destination update in: " + timeStr + "</p><p>Difficulty: " + StringUtils.repeat("*",gameDifficulty) +"</p></html>");

				timeLabel.setForeground(Color.LIGHT_GRAY);
				textPanel.removeAll();
				textPanel.add(timeLabel, BorderLayout.NORTH);
				textPanel.revalidate();
				textPanel.repaint();

				if (timeUntilNext == 60)
				{
					plugin.oneMinuteWarning();
				}
			}
		}
		if (isInQueue() && timeUntilNext == -1)
		{
			timeUntilNext = 600;
			onSSE();
		}
	}

	private String formatTime(int seconds)
	{
		int minutes = seconds / 60;
		int secs = seconds % 60;
		return String.format("%d:%02d", minutes, secs);
	}
}