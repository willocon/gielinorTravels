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
    //private final JPanel textPanel = new JPanel();


    public GielinorTravelsPanel(GielinorTravelsPlugin plugin, GielinorTravelsConfig config){
        super();
        this.plugin = plugin;

        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel label = new JLabel("Gielinor Travels");
        label.setForeground(Color.LIGHT_GRAY);
        label.setHorizontalAlignment(SwingConstants.CENTER);

//        BufferedImage locationImg = ImageUtil.loadImageResource(GielinorTravelsPlugin.class, "/join.png");
        JPanel picPanel = new JPanel();
        picPanel.setBorder(new EmptyBorder(0,0,0,0));
        picPanel.setLayout(new GridLayout(1, 1, 5, 5));

        // Sidebar width is ~220px depending on RuneLite scaling,
        // so we resize to fit the panel width while keeping aspect ratio.
//        setScaledImage(locationImg);

        picPanel.add(picLabel,BorderLayout.NORTH);

        JLabel infoLabel = new JLabel("<html>Join a lobby to be matched with a random location in Gielinor.<br>" +
                "Every 10 minutes, your destination will update to a new location!</html>");
        infoLabel.setForeground(Color.LIGHT_GRAY);

        buttonPanel.setBorder(new EmptyBorder(0,0,0,0));
        buttonPanel.setLayout(new GridLayout(2,1,5,5));


        //showButton.addActionListener(this::onShowButtonClicked);
        startButton.addActionListener(this::onStartButtonClicked);

        //buttonPanel.add(showButton);
        buttonPanel.add(infoLabel);
        buttonPanel.add(startButton);

        add(label, BorderLayout.NORTH);
        add(picPanel,BorderLayout.CENTER);
        add(buttonPanel,BorderLayout.SOUTH);
    }

    private void onShowButtonClicked(ActionEvent e){
        plugin.showLocation();
    }

    private void onStartButtonClicked(ActionEvent e){
//        setScaledImage(ImageUtil.loadImageResource(GielinorTravelsPlugin.class,"/waiting.png"));
        location = new LocationLoader(plugin, this);
        inQueue = true;


        JLabel waitLabel = new JLabel("<html>Waiting for server to send destination.</html>");
        waitLabel.setForeground(Color.LIGHT_GRAY);

        buttonPanel.removeAll();
        //buttonPanel.add(showButton);
        JButton stopButton = new JButton("Leave Lobby");
        stopButton.addActionListener(this::onStopButtonClicked);

        buttonPanel.add(waitLabel);
        buttonPanel.add(stopButton);
        buttonPanel.revalidate();
        buttonPanel.repaint();
    }

    private void onStopButtonClicked(ActionEvent e){
        leaveQueue();
        //setScaledImage(ImageUtil.loadImageResource(GielinorTravelsPlugin.class,"/join.png"));
        picLabel.removeAll();
        picLabel.setIcon(null);
        picLabel.revalidate();
        picLabel.repaint();
        JLabel infoLabel = new JLabel("<html>Join a lobby to be matched with a random location in Gielinor.<br>" +
                "Every 10 minutes, your destination will update to a new location!</html>");
        infoLabel.setForeground(Color.LIGHT_GRAY);
        buttonPanel.removeAll();
        //buttonPanel.add(showButton);
        buttonPanel.add(infoLabel);
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
        JLabel destLabel = new JLabel("<html>Travel to destination!</html>");
        destLabel.setForeground(Color.LIGHT_GRAY);
        buttonPanel.removeAll();
        buttonPanel.add(destLabel);
        JButton stopButton = new JButton("Leave Lobby");
        stopButton.addActionListener(this::onStopButtonClicked);
        buttonPanel.add(stopButton);
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
//        setScaledImage(ImageUtil.loadImageResource(GielinorTravelsPlugin.class,"/waiting.png"));
        picLabel.removeAll();
        picLabel.setIcon(null);
        picLabel.revalidate();
        picLabel.repaint();
        JLabel waitLabel = new JLabel("<html>Waiting for next destination update in 10 minutes.</html>");
        waitLabel.setForeground(Color.LIGHT_GRAY);
        buttonPanel.removeAll();
        buttonPanel.add(waitLabel);
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