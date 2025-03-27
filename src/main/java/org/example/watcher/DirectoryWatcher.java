package org.example.watcher;

import org.example.processor.FileProcessor;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.*;

public class DirectoryWatcher implements Runnable {
    private final Path pathToWatch;
    private final ConcurrentHashMap<String, Long> lastModifiedMap = new ConcurrentHashMap<>();
    private final ExecutorService executorService;

    public DirectoryWatcher(String directoryPath) {
        this.pathToWatch = Paths.get(directoryPath);
        this.executorService = Executors.newFixedThreadPool(4);
    }

    @Override
    public void run() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            pathToWatch.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY);

            while (true) {
                WatchKey key = watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    Path fileName = (Path) event.context();

                    if (fileName.toString().endsWith(".txt") || fileName.toString().endsWith(".csv")) {
                        Path fullPath = pathToWatch.resolve(fileName);
                        long lastModified = Files.getLastModifiedTime(fullPath).toMillis();

                        Long previous = lastModifiedMap.put(fileName.toString(), lastModified);
                        if (previous == null || previous != lastModified) {
                            System.out.println("[INFO] Detektovana izmena fajla: " + fileName);
                            executorService.submit(new FileProcessor(fullPath));
                        }
                    }
                }
                key.reset();
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("[GREŠKA] Problem sa direktorijumom: " + e.getMessage());
        }
    }
}
