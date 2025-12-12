package org.GielinorTravels;

import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

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

    private final JButton showButton = new JButton("Show Location");
    private final JButton startButton = new JButton("Join Lobby");
    private final JLabel picLabel = new JLabel();
    private final JPanel buttonPanel = new JPanel();


    public GielinorTravelsPanel(GielinorTravelsPlugin plugin, GielinorTravelsConfig config){
        super();
        this.plugin = plugin;

        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel label = new JLabel("Gielinor Travels");
        label.setForeground(Color.LIGHT_GRAY);
        label.setHorizontalAlignment(SwingConstants.CENTER);

        BufferedImage locationImg = ImageUtil.loadImageResource(GielinorTravelsPlugin.class, "/join.png");
        JPanel picPanel = new JPanel();
        picPanel.setBorder(new EmptyBorder(0,0,0,0));
        picPanel.setLayout(new GridLayout(1, 1, 5, 5));

        // Sidebar width is ~200px depending on RuneLite scaling,
        // so we resize to fit the panel width while keeping aspect ratio.
        setScaledImage(locationImg);

        picPanel.add(picLabel,BorderLayout.NORTH);

        buttonPanel.setBorder(new EmptyBorder(0,0,0,0));
        buttonPanel.setLayout(new GridLayout(2,1,5,5));


        showButton.addActionListener(this::onShowButtonClicked);
        startButton.addActionListener(this::onStartButtonClicked);

        buttonPanel.add(showButton);
        buttonPanel.add(startButton);

        add(label, BorderLayout.NORTH);
        add(picPanel,BorderLayout.CENTER);
        add(buttonPanel,BorderLayout.SOUTH);
    }

    private void onShowButtonClicked(ActionEvent e){
        plugin.showLocation();
    }

    private void onStartButtonClicked(ActionEvent e){
        setScaledImage(ImageUtil.loadImageResource(GielinorTravelsPlugin.class,"/waiting.png"));
        location = new LocationLoader(plugin, this);
        inQueue = true;

        buttonPanel.removeAll();
        buttonPanel.add(showButton);
        JButton stopButton = new JButton("Leave Lobby");
        stopButton.addActionListener(this::onStopButtonClicked);

        buttonPanel.add(stopButton);
        buttonPanel.revalidate();
        buttonPanel.repaint();
    }

    private void onStopButtonClicked(ActionEvent e){
        leaveQueue();
        setScaledImage(ImageUtil.loadImageResource(GielinorTravelsPlugin.class,"/join.png"));
        buttonPanel.removeAll();
        buttonPanel.add(showButton);
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

    public void onTenMinute(){
        location.loadFromServer();
        setScaledImage(location.getLocationImg());
        plugin.setDestination(location.getDestination());
        plugin.changeOverlayImage(location.getLocationImg());
        plugin.showOverlay();
    }

    public void panelSendCompleted(String userid,long timerTicks, String playerName) {
        try {
            location.imageClient.SendCompleted(
                    userid,
                    timerTicks,
                    playerName
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        setScaledImage(ImageUtil.loadImageResource(GielinorTravelsPlugin.class,"/waiting.png"));
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