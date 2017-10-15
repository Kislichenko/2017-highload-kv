package ru.mail.polis.kislichenko;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class StreamReading {

    private final InputStream in;
    private final long length;//количество байтов, которое нужно прочитать из потока

    public StreamReading(InputStream newIn, long newLength) {
        this.in = newIn;
        this.length = newLength;
    }

    //Алгоритм чтения файлов до конца.
    public byte[] getByteArray() throws IOException {

        final FileOutputStream out = new FileOutputStream(new File("buffer.bf"));
        int bufferSize = 8 * 1024 * 1024;//размер буфера 8Mb
        byte tempBuffer[] = new byte[bufferSize];
        int countBlocks = 0;

        //сначала читается файл пока не заполнится основной массив, потом остатки (если есть)
        while (in.available() > 0) {

            //кол-во доступных байт меньше буффера
            if (in.available() < bufferSize) {
                bufferSize = in.available();
                in.read(tempBuffer, 0, bufferSize);
                out.write(tempBuffer, 0, bufferSize);
            }
            //проверка на то, чтобы не записалось больше доступной памяти
            else if ((countBlocks + 1) * bufferSize < length) {
                in.read(tempBuffer, 0, bufferSize);
                out.write(tempBuffer, 0, bufferSize);
                countBlocks++;
            }
            //заполнение остатка свободной памяти
            else if (length - countBlocks * bufferSize > 0) {
                in.read(tempBuffer, 0, (int) (length - countBlocks * bufferSize));
                out.write(tempBuffer, 0, (int) (length - countBlocks * bufferSize));
                countBlocks++;
            }
            //безцельное считывание до конца файла
            else in.read(tempBuffer, 0, bufferSize);

        }

        in.close();
        out.close();
        return Files.readAllBytes(Paths.get("", "buffer.bf"));
    }


}