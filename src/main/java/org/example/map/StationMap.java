package org.example.map;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class StationMap {
    private final Map<Character, StationData> dataMap = new TreeMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private static final StationMap instance = new StationMap();

    private StationMap() {
    }

    public static StationMap getInstance() {
        return instance;
    }

    public void update(char letter, double temperature) {
        lock.writeLock().lock();
        try {
            StationData current = dataMap.getOrDefault(letter, new StationData());
            current.incrementCount();
            current.addToSum(temperature);
            dataMap.put(letter, current);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Map<Character, StationData> getSnapshot() {
        lock.readLock().lock();
        try {
            return new TreeMap<>(dataMap);
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return dataMap.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }
}
