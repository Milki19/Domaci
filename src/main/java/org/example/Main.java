package org.example;

import org.example.cli.CliListener;
import org.example.cli.CommandExecutor;
import org.example.report.ReportScheduler;
import org.example.watcher.DirectoryWatcher;

public class Main {
    private static final Object logLock = new Object();

    public static void main(String[] args) {
        String directoryToWatch = "data";

        // Pokretanje monitora direktorijuma
        DirectoryWatcher watcher = new DirectoryWatcher(directoryToWatch);
        Thread watcherThread = new Thread(watcher);
        watcherThread.start();

        // Pokretanje CLI niti
        CliListener cliListener = new CliListener();
        Thread cliThread = new Thread(cliListener);
        cliThread.start();

        // Pokretanje executor niti za komande
        CommandExecutor commandExecutor = new CommandExecutor(cliListener.getCommandQueue());
        Thread commandExecutorThread = new Thread(commandExecutor);
        commandExecutorThread.start();

        // Pokretanje periodičnog loggera
        ReportScheduler scheduler = new ReportScheduler(logLock);
        Thread schedulerThread = new Thread(scheduler);
        schedulerThread.setDaemon(true);
        schedulerThread.start();

        System.out.println("Sistem je pokrenut. Praćenje direktorijuma: " + directoryToWatch);
    }

    public static Object getLogLock() {
        return logLock;
    }
}
