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

    private final JButton showButton = new JButton("Show Location");
    private final JButton startButton = new JButton("Start");
    private final JLabel picLabel = new JLabel();


    public GielinorTravelsPanel(GielinorTravelsPlugin plugin, GielinorTravelsConfig config){
        super();
        this.plugin = plugin;

        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel label = new JLabel("Developer Tools");
        label.setForeground(Color.LIGHT_GRAY);
        label.setHorizontalAlignment(SwingConstants.CENTER);

        BufferedImage locationImg = ImageUtil.loadImageResource(GielinorTravelsPlugin.class, "/locations/1/1.png");
        JPanel picPanel = new JPanel();
        picPanel.setBorder(new EmptyBorder(0,0,0,0));
        picPanel.setLayout(new GridLayout(1, 1, 5, 5));

        // Sidebar width is ~200px depending on RuneLite scaling,
        // so we resize to fit the panel width while keeping aspect ratio.
        setScaledImage(locationImg);

        picPanel.add(picLabel,BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel();
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
        LocationLoader location = new LocationLoader();
        setScaledImage(location.getLocationImg());
        plugin.setDestination(location.getDestination());
    }

    private void setScaledImage(BufferedImage locationImg){
        // Sidebar width is ~200px depending on RuneLite scaling,
        // so we resize to fit the panel width while keeping aspect ratio.
        int panelWidth = Math.max(220, getWidth());
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

}