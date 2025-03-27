package org.example.cli;

import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class CliListener implements Runnable {
    private final BlockingQueue<String> commandQueue = new LinkedBlockingQueue<>();

    public BlockingQueue<String> getCommandQueue() {
        return commandQueue;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("[CLI] Unesite komandu:");

        while (true) {
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("STOP") || input.equalsIgnoreCase("START")) {
                // Te komande ne idu u queue, već se eventualno obrađuju direktno
                System.out.println("[INFO] Komanda '" + input + "' primljena (obrada direktno, bez reda).");
            } else {
                try {
                    commandQueue.put(input);
                } catch (InterruptedException e) {
                    System.err.println("[GREŠKA] CLI prekid: " + e.getMessage());
                }
            }
        }
    }
}
