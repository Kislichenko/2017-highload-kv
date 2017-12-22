package ru.mail.polis.kislichenko;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class StreamReading {

    final int bufferSize = 8 * 1024;//размер буфера 8Kb
    private final InputStream in;
    private final long length;//количество байтов, которое нужно прочитать из потока

    public StreamReading(InputStream newIn, long newLength) {
        this.in = newIn;
        this.length = newLength;
    }

    //Алгоритм чтения файлов до конца.
    public byte[] getByteArray() throws IOException {

        long temp = 0;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            byte tempBuffer[] = new byte[bufferSize];
            while (true) {
                int bytesLength = in.read(tempBuffer);
                temp += bytesLength;
                if (bytesLength == -1 || temp > length) break;
                if (bytesLength > 0) out.write(tempBuffer, 0, bytesLength);
            }

            out.flush();
            in.close();

            return out.toByteArray();
        }
    }
}