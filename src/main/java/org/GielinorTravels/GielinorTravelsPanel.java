package org.GielinorTravels;

import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

public class GielinorTravelsPanel extends PluginPanel
{

    private final GielinorTravelsPlugin plugin;
    private LocationLoader location;

    private boolean inQueue = false;

    private final JPanel textPanel = new JPanel();
    private final JButton startButton = new JButton("Join Lobby");
    private final JLabel picLabel = new JLabel();
    private final JPanel buttonPanel = new JPanel();
    private final JLabel topLabel = new JLabel("Gielinor Travels");

    public GielinorTravelsPanel(GielinorTravelsPlugin plugin, GielinorTravelsConfig config){
        super();
        this.plugin = plugin;

        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        topLabel.setForeground(Color.LIGHT_GRAY);
        topLabel.setHorizontalAlignment(SwingConstants.CENTER);
        textPanel.setBorder(new EmptyBorder(0,0,0,0));
        textPanel.setLayout(new GridLayout(2,1,5,5));
        textPanel.add(topLabel, BorderLayout.NORTH);

        JPanel picPanel = new JPanel();
        picPanel.setBorder(new EmptyBorder(0,0,0,0));
        picPanel.setLayout(new GridLayout(1, 1, 5, 5));

        picPanel.add(picLabel,BorderLayout.NORTH);

        JLabel infoLabel = new JLabel("<html>Join a lobby to be matched with a random location in Gielinor.<br>" +
                "Every 10 minutes, your destination will update to a new location!</html>");
        infoLabel.setForeground(Color.LIGHT_GRAY);
        textPanel.add(infoLabel, BorderLayout.CENTER);

        buttonPanel.setBorder(new EmptyBorder(0,0,0,0));
        buttonPanel.setLayout(new GridLayout(2,1,5,5));


        startButton.addActionListener(this::onStartButtonClicked);


        buttonPanel.add(startButton);

        add(textPanel, BorderLayout.NORTH);
        add(picPanel,BorderLayout.CENTER);
        add(buttonPanel,BorderLayout.SOUTH);
    }

    private void onStartButtonClicked(ActionEvent e){
        location = new LocationLoader(plugin, this);
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

    private void removeOverlayButtonClicked(ActionEvent e){
        plugin.hideOverlay();
    }

    private void onStopButtonClicked(ActionEvent e){
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

    public boolean isInQueue(){ return inQueue; }

    private void setScaledImage(BufferedImage locationImg){
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

    public void onSSE(){
        location.loadFromServer();
        JLabel destLabel = new JLabel("<html>Travel to destination!</html>");
        destLabel.setForeground(Color.LIGHT_GRAY);
        textPanel.removeAll();
        topLabel.setForeground(Color.LIGHT_GRAY);
        topLabel.setHorizontalAlignment(SwingConstants.CENTER);
        textPanel.add(topLabel, BorderLayout.NORTH);
        textPanel.add(destLabel, BorderLayout.CENTER);
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
        setScaledImage(location.getLocationImg());
        plugin.setDestination(location.getDestination());
        plugin.changeOverlayImage(location.getLocationImg());
        plugin.showOverlay();
    }

    public void panelSendCompleted(String userid, String playerName, GielinorTravelsPlugin plugin) {
        try {
            location.imageClient.SendCompleted(
                    userid,
                    playerName,
                    plugin
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        picLabel.removeAll();
        picLabel.setIcon(null);
        picLabel.revalidate();
        picLabel.repaint();
        JLabel waitLabel = new JLabel("<html>Waiting for next destination update in 10 minutes.</html>");
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

    public void leaveQueue() {
        inQueue = false;
        try {
            long userID = plugin.client.getAccountHash();
            location.imageClient.leaveQueue(userID+"");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}