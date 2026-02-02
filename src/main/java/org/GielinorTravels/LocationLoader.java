package org.GielinorTravels;

import lombok.SneakyThrows;
import net.runelite.api.coords.WorldPoint;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class LocationLoader {

    private BufferedImage locationImg;

    private WorldPoint destination;

    public final SSEImageClient imageClient = new SSEImageClient();


    @SneakyThrows
    public LocationLoader(GielinorTravelsPlugin plugin, GielinorTravelsPanel panel){
        //join the queue and wait for image and csv
        long userID = plugin.client.getAccountHash();
        String username = plugin.client.getLocalPlayer().getName();
        imageClient.joinQueue(userID+"",username);
        imageClient.listenForImageEvents(userID+"",panel);
    }

    public BufferedImage getLocationImg()
    {
        return locationImg;
    }
    public WorldPoint getDestination()
    {
        return destination;
    }


    private int[] readWorldPointCSV(String line) {
        List<Integer> numbers = new ArrayList<>();
        String[] values = line.split(",");

        // Parse each value as int and add to the list
        for (String value : values) {
            numbers.add(Integer.parseInt(value.trim()));
        }
        // Convert List<Integer> to int[]
        return numbers.stream().mapToInt(Integer::intValue).toArray();
    }

    public void loadFromServer(){
        imageClient.handleEvent();
        this.locationImg = imageClient.getDownloadedImage();
        String csvLine = imageClient.getDownloadedCsv();
        int[] coords = readWorldPointCSV(csvLine);
        this.destination = new WorldPoint(coords[0],coords[1],coords[2]);
    }
}
