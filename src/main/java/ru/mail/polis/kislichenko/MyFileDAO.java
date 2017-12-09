package ru.mail.polis.kislichenko;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.NoSuchElementException;

public class MyFileDAO implements MyDAO {

    @NotNull
    private final File dir;

    public MyFileDAO(@NotNull final File dir) {
        this.dir = dir;
    }

    @NotNull
    @Override
    public byte[] get(@NotNull final String key) throws NoSuchElementException, IllegalArgumentException, IOException {

        final File file = getFile(key);

        if (!file.exists()) {
            throw new NoSuchElementException("File with key= " + key + " doesn't exist!");
        }

        //если размер файла меньше допустимой памяти
        if (file.length() < getSizeFromFreeMemory(file)) return Files.readAllBytes(Paths.get(dir.toString(), key));
            //если размер файла больше допустимой памяти, то берется первая часть
        else return new StreamReading(new FileInputStream(file), getSizeFromFreeMemory(file)).getByteArray();
    }

    @NotNull
    private File getFile(@NotNull final String key) throws IOException {
        return new File(dir, key);
    }

    @Override
    public void upsert(@NotNull final String key,
                       @NotNull final byte[] value) throws IllegalArgumentException, IOException {

        try (OutputStream os = new FileOutputStream(getFile(key))) {
            os.write(value);
        }
    }

    @Override
    public void delete(@NotNull final String key) throws IllegalArgumentException, IOException {
        getFile(key).delete();
    }

    private long getFreeMemory() {
        return Runtime.getRuntime().maxMemory()
                - (Runtime.getRuntime().totalMemory()
                - Runtime.getRuntime().freeMemory());
    }

    //сколько можно взять свободной памяти, чтобы не навредить работе программы (70%)
    private int getSizeFromFreeMemory(File file) {
        if (file.length() > Integer.MAX_VALUE) {
            if (file.length() < getFreeMemory() * 0.7) return Integer.MAX_VALUE;
            return (int) (getFreeMemory() * 0.7);
        } else {
            if (file.length() < getFreeMemory() * 0.7) return (int) file.length();
            return (int) (getFreeMemory() * 0.7);
        }
    }

}