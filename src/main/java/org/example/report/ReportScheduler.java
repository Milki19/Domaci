package org.example.report;


import org.example.map.StationData;
import org.example.map.StationMap;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class ReportScheduler implements Runnable {
    private final Path logPath = Path.of("station_map_log.csv");
    private final Object logLock;

    public ReportScheduler(Object logLock) {
        this.logLock = logLock;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(60_000); // 1 minut

                synchronized (logLock) {
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(logPath.toFile(), false))) {
                        writer.write("Letter,Station count,Sum\n");
                        Map<Character, StationData> snapshot = StationMap.getInstance().getSnapshot();
                        for (Map.Entry<Character, StationData> entry : snapshot.entrySet()) {
                            writer.write(entry.getKey() + "," + entry.getValue().getCount() + "," + entry.getValue().getSum() + "\n");
                        }
                        System.out.println("[SCHEDULER] Periodični log ažuriran: " + logPath.toAbsolutePath());
                    } catch (IOException e) {
                        System.err.println("[GREŠKA] Neuspešan upis periodičnog loga: " + e.getMessage());
                    }
                }

            } catch (InterruptedException e) {
                System.out.println("[SCHEDULER] Izveštaj prekinut.");
                break;
            }
        }
    }
}
