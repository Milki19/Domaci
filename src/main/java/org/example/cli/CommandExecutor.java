package org.example.cli;

import org.example.map.StationData;
import org.example.map.StationMap;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandExecutor implements Runnable {
    private final BlockingQueue<String> commandQueue;
    private final Map<String, String> jobStatus = new HashMap<>();
    private volatile boolean running = true;


    public CommandExecutor(BlockingQueue<String> commandQueue) {
        this.commandQueue = commandQueue;
    }

    @Override
    public void run() {
        while (running) {
            try {
                String command = commandQueue.take();
                executeCommand(command);
            } catch (InterruptedException e) {
                System.err.println("[GREŠKA] Executor prekid: " + e.getMessage());
            }
        }
    }

    private void executeCommand(String input) {
        String normalized = input.trim();

        if (normalized.toUpperCase().startsWith("START")) {
            System.out.println("[EXECUTOR] Izvršavam START komandu: " + input);
            Map<String, String> args = parseArguments(normalized);
            String loadOption = args.getOrDefault("--load-jobs", args.get("-l"));
            if (loadOption != null) {
                Path loadPath = Paths.get("load_config.txt");
                if (Files.exists(loadPath)) {
                    try (BufferedReader reader = Files.newBufferedReader(loadPath)) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] parts = line.split(",");
                            if (parts.length == 2) {
                                String job = parts[0];
                                jobStatus.put(job, "pending");
                                System.out.println("[START] Učitana definicija posla: " + job + " (status: pending)");
                            }
                        }
                        System.out.println("[START] Poslovi učitani iz load_config.txt");
                    } catch (IOException e) {
                        System.err.println("[GREŠKA] Nije moguće učitati poslove: " + e.getMessage());
                    }
                } else {
                    System.out.println("[START] Fajl sačuvanih poslova ne postoji.");
                }
            } else {
                System.out.println("[START] Nema poslova za učitavanje.");
            }

        } else if (normalized.toUpperCase().startsWith("SCAN")) {
            System.out.println("[EXECUTOR] Izvršavam SCAN komandu: " + input);
            Map<String, String> args = parseArguments(normalized);
            String min = args.getOrDefault("--min", args.get("-m"));
            String max = args.getOrDefault("--max", args.get("-M"));
            String letter = args.getOrDefault("--letter", args.get("-l"));
            String output = args.getOrDefault("--output", args.get("-o"));
            String job = args.getOrDefault("--job", args.get("-j"));

            if (min == null || max == null || letter == null || output == null || job == null) {
                System.out.println("[GREŠKA] SCAN komanda nije validna. Potrebni su svi argumenti.");
                return;
            }

            jobStatus.put(job, "running");
            new Thread(() -> {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
                    Files.list(Paths.get("data"))
                            .filter(p -> p.toString().endsWith(".txt") || p.toString().endsWith(".csv"))
                            .forEach(path -> {
                                try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
                                    boolean skipHeader = path.toString().endsWith(".csv");
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        if (skipHeader) {
                                            skipHeader = false;
                                            continue;
                                        }
                                        String[] parts = line.split(";");
                                        if (parts.length != 2) continue;
                                        String station = parts[0].trim();
                                        double temp = Double.parseDouble(parts[1].trim());
                                        if (station.toUpperCase().startsWith(letter.toUpperCase()) && temp >= Double.parseDouble(min) && temp <= Double.parseDouble(max)) {
                                            writer.write(station + ";" + temp + "\n");
                                        }
                                    }
                                } catch (IOException e) {
                                    System.err.println("[UPOZORENJE] Greška u fajlu " + path.getFileName());
                                }
                            });
                    System.out.println("[SCAN] Završena pretraga. Rezultati su u fajlu: " + output);
                    jobStatus.put(job, "completed");
                } catch (IOException e) {
                    System.err.println("[GREŠKA] Ne mogu da upišem u fajl: " + output);
                    jobStatus.put(job, "error");
                }
            }).start();

        } else if (normalized.toUpperCase().startsWith("STATUS")) {
            System.out.println("[EXECUTOR] Izvršavam STATUS komandu: " + input);
            Map<String, String> args = parseArguments(normalized);
            String job = args.getOrDefault("--job", args.get("-j"));
            if (job == null) {
                System.out.println("[GREŠKA] STATUS komanda mora da sadrži --job argument.");
                return;
            }
            String status = jobStatus.getOrDefault(job, "pending");
            System.out.println(job + " is " + status);

        } else if (normalized.toUpperCase().startsWith("MAP")) {
            System.out.println("[EXECUTOR] Izvršavam MAP komandu:");
            if (StationMap.getInstance().isEmpty()) {
                System.out.println("[MAP] Mapa još uvek nije dostupna.");
            } else {
                Map<Character, StationData> snapshot = StationMap.getInstance().getSnapshot();
                int count = 0;
                StringBuilder line = new StringBuilder();
                for (Map.Entry<Character, StationData> entry : snapshot.entrySet()) {
                    line.append(entry.getKey()).append(": ")
                            .append(entry.getValue().getCount()).append(" - ")
                            .append(entry.getValue().getSum()).append("    ");
                    count++;
                    if (count % 2 == 0) {
                        System.out.println(line);
                        line = new StringBuilder();
                    }
                }
                if (line.length() > 0) {
                    System.out.println(line);
                }
            }

        } else if (normalized.toUpperCase().startsWith("EXPORTMAP")) {
            System.out.println("[EXECUTOR] Izvršavam EXPORTMAP komandu:");
            Path logPath = Paths.get("station_map_log.csv");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logPath.toFile()))) {
                writer.write("Letter,Station count,Sum\n");
                Map<Character, StationData> snapshot = StationMap.getInstance().getSnapshot();
                for (Map.Entry<Character, StationData> entry : snapshot.entrySet()) {
                    writer.write(entry.getKey() + "," + entry.getValue().getCount() + "," + entry.getValue().getSum() + "\n");
                }
                System.out.println("[EXPORTMAP] Mapa uspešno eksportovana u " + logPath.toAbsolutePath());
            } catch (IOException e) {
                System.err.println("[GREŠKA] Neuspešno pisanje EXPORTMAP fajla: " + e.getMessage());
            }

        } else if (normalized.toUpperCase().startsWith("SHUTDOWN")) {
            System.out.println("[EXECUTOR] Izvršavam SHUTDOWN komandu: " + input);
            Map<String, String> args = parseArguments(normalized);
            String saveOption = args.getOrDefault("--save-jobs", args.get("-s"));
            if (saveOption != null) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter("load_config.txt"))) {
                    for (Map.Entry<String, String> entry : jobStatus.entrySet()) {
                        if (!"completed".equals(entry.getValue())) {
                            writer.write(entry.getKey() + "," + entry.getValue() + "\n");
                        }
                    }
                    System.out.println("[SHUTDOWN] Sačuvani neizvršeni poslovi u load_config.txt");
                } catch (IOException e) {
                    System.err.println("[GREŠKA] Nije moguće sačuvati poslove: " + e.getMessage());
                }
            }
            running = false;
            System.exit(0);
        } else {
            System.out.println("[UPOZORENJE] Nepoznata komanda: " + input);
        }
    }

    private Map<String, String> parseArguments(String input) {
        Map<String, String> args = new HashMap<>();
        Matcher matcher = Pattern.compile("(--\\w+|-\\w)\\s+([^\\s]+)").matcher(input);
        while (matcher.find()) {
            args.put(matcher.group(1), matcher.group(2));
        }
        return args;
    }
}
