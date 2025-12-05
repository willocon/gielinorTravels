package org.GielinorTravels;

import lombok.SneakyThrows;
import net.runelite.api.coords.WorldPoint;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class LocationLoader {

    private BufferedImage locationImg;

    private WorldPoint destination;

    private final SSEImageClient imageClient = new SSEImageClient();


    @SneakyThrows
    public LocationLoader(GielinorTravelsPlugin plugin){
        //join the queue and wait for image and csv
        long userID = plugin.client.getAccountHash();
        imageClient.joinQueue(userID+"");
        imageClient.listenForImageEvents(userID+"");
    }

    public BufferedImage getLocationImg() { return locationImg; }
    public WorldPoint getDestination() { return destination; }


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
        this.locationImg = imageClient.getDownloadedImage();
        String csvLine = imageClient.getDownloadedCsv();
        int[] coords = readWorldPointCSV(csvLine);
        this.destination = new WorldPoint(coords[0],coords[1],coords[2]);
    }
}
