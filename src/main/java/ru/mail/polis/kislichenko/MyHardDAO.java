package ru.mail.polis.kislichenko;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.NoSuchElementException;

/*
В этом классе используется немного более сложный алгоритм чтения файла, чем в классе FileReading
 */
public class MyHardDAO implements MyDAO {

    @NotNull
    private final File dir;

    public MyHardDAO(@NotNull final File dir) {
        this.dir = dir;
    }

    @NotNull
    @Override
    public byte[] get(@NotNull String key) throws NoSuchElementException, IllegalArgumentException, IOException {

            final File file = getFile(key);

             if(!file.exists()){
                   throw new NoSuchElementException("File with key= "+key+" doesn't exist!");
             }

            InputStream in = new FileInputStream(file);
            int bufferSize;
            int fileSize;

            fileSize = getSizeFromFreeMemory(file);
            byte smallArray[] = new byte[fileSize];

            bufferSize = 8 * 1024 * 1024;//8Mb
            byte tempBuffer[] = new byte[bufferSize];

            int tmp = 0;

        while (in.available() > 0) {
            if (in.available() < bufferSize) bufferSize = in.available();

            if (tmp * bufferSize < file.length()) {
                in.read(smallArray, tmp * bufferSize, (int) file.length());
                tmp++;
            } else if (file.length() - tmp * bufferSize > 0) {
                in.read(smallArray, (tmp - 1) * bufferSize, (int) (file.length() - tmp * bufferSize));
                tmp++;
            } else in.read(tempBuffer, 0, bufferSize);
        }

        in.close();

        return smallArray;
        }


    @Override
    public void upsert(@NotNull final String key,
                       @NotNull final byte[] value) throws IllegalArgumentException, IOException {

        try(OutputStream os = new FileOutputStream(getFile(key))){
            os.write(value);
        }
    }

    @Override
    public void delete(@NotNull final String key) throws IllegalArgumentException, IOException {
        getFile(key).delete();
    }

    //сколько можно есть свободной памяти
    private long getFreeMemory() {
        return Runtime.getRuntime().maxMemory()
                - (Runtime.getRuntime().totalMemory()
                - Runtime.getRuntime().freeMemory());
    }

    //сколько можно взять свободной памяти, чтобы не навредить работе программы
    private int getSizeFromFreeMemory(File file){
        if (file.length() > Integer.MAX_VALUE) {
            if (file.length() < getFreeMemory() * 0.7) return Integer.MAX_VALUE;
            else return (int) (getFreeMemory() * 0.7);
        } else {
            if (file.length() < getFreeMemory() * 0.7) return (int) file.length();
            else return (int) (getFreeMemory() * 0.7);
        }
    }

    @NotNull
    private File getFile(@NotNull final String key) {
        return new File(dir, key);
    }
}


