package org.example.processor;

import org.example.map.StationMap;

import java.io.*;
import java.nio.file.Path;

public class FileProcessor implements Runnable {
    private final Path filePath;
    private final boolean isCsv;

    public FileProcessor(Path filePath) {
        this.filePath = filePath;
        this.isCsv = filePath.toString().endsWith(".csv");
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
            String line;
            boolean skipHeader = isCsv;

            while ((line = reader.readLine()) != null) {
                if (skipHeader) {
                    skipHeader = false;
                    continue;
                }

                String[] parts = line.split(";");
                if (parts.length != 2) continue;

                String station = parts[0].trim();
                double temperature;
                try {
                    temperature = Double.parseDouble(parts[1].trim());
                } catch (NumberFormatException e) {
                    System.err.println("[UPOZORENJE] Nevalidna temperatura: " + parts[1]);
                    continue;
                }

                if (!station.isEmpty()) {
                    char letter = Character.toLowerCase(station.charAt(0));
                    StationMap.getInstance().update(letter, temperature);
                }
            }

            System.out.println("[INFO] Obrada fajla završena: " + filePath.getFileName());

        } catch (IOException e) {
            System.err.println("[GREŠKA] Neuspešno čitanje fajla '" + filePath.getFileName() + "'. Nastavljam rad.");
        }
    }
}
