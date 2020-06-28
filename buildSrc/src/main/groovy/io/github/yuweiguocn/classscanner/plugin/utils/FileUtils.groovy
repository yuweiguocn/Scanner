package io.github.yuweiguocn.classscanner.plugin.utils

class FileUtils{
    static void closeQuietly(Closeable obj) {
        if (obj != null) {
            try {
                obj.close()
            } catch (Exception e) {
                e.printStackTrace()
            }
        }
    }
}
