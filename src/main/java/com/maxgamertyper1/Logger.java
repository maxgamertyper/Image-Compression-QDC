package com.maxgamertyper1;

import java.util.function.Supplier;

public final class Logger {
    public static final int INFO_LEVEL = 0;
    public static final int VERBOSE_LEVEL = 1;
    public static final int DUMP_LEVEL = 2;

    private static int level = INFO_LEVEL; // 0 - normal, 1 - verbose, 2 - dump
    private static long previousTime = System.nanoTime();
    private static long startTime = System.nanoTime();


    public static void setLevel(int newLevel) {
        if (level != INFO_LEVEL) {
            System.err.println("can't reset logging level");
            return;
        }
        level = newLevel;
    }

    public static int getLevel() {
        return level;
    }

    static void info(String msg)    { System.out.println(msg); }
    static void verbose(String msg) { if (level >= VERBOSE_LEVEL) System.out.println("  " + msg); }
    static void dump(String head, Supplier<String> msg) {
        if (level >= DUMP_LEVEL) System.out.println(head + msg.get());
    }

    static void logTime() {
        long durationNano = System.nanoTime() - previousTime;
        previousTime = System.nanoTime();
        System.out.println("Time taken: " + durationNano + " ns {" + durationNano/1_000_000.0 + " ms}");
    }

    static void wholeTime() {
        long durationNano = System.nanoTime() - startTime;
        System.out.println("Entire Time taken: " + durationNano + " ns {" + durationNano/1_000_000.0 + " ms}");
    }
}
