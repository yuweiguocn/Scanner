package io.github.yuweiguocn.classscanner.plugin.utils


class L {
    public static boolean DEBUG = true

    static void d(String message) {
        if (DEBUG)
            println("scanner: " + message)
    }
}