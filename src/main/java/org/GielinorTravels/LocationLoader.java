package org.GielinorTravels;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.Random;

public class LocationLoader {

    private final BufferedImage locationImg;

    private final WorldPoint destination;

    public LocationLoader(){
        Random rand = new Random();
        int maxNum = countFiles();
        int locationNum = rand.nextInt(maxNum)+1;
        String path = ("/locations/"+locationNum);

        this.locationImg = ImageUtil.loadImageResource(GielinorTravelsPlugin.class, path+"/1.png");

        int[] coords = readWorldPointCSV("src/main/resources"+path);
        this.destination = new WorldPoint(coords[0],coords[1],coords[2]);
    }

    public BufferedImage getLocationImg() { return locationImg; }
    public WorldPoint getDestination() { return destination; }

    private int countFiles(){
        Path dir = Paths.get("src/main/resources/locations");

        try (Stream<Path> files = Files.list(dir)) {
            return Math.toIntExact(files.filter(Files::isDirectory).count());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private int[] readWorldPointCSV(String path){
        List<Integer> numbers = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path+"/1.csv"))) {
            String line;

            while ((line = br.readLine()) != null) {
                // Split line by commas
                String[] values = line.split(",");

                // Parse each value as int and add to the list
                for (String value : values) {
                    numbers.add(Integer.parseInt(value.trim()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Convert List<Integer> to int[]
        return numbers.stream().mapToInt(Integer::intValue).toArray();
    }
}
