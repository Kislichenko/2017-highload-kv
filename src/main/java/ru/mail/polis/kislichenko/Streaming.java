package ru.mail.polis.kislichenko;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class Streaming {
    private final long length;//количество байтов, которое нужно прочитать из потока
    private FileChannel fc;

    public Streaming(long newLength) {
        this.length = newLength;
    }

    public byte[] getByteArray(File file) throws IOException {
        //организуем прямой доступ к файлу и получаем канал файла
        fc = new RandomAccessFile(file, "rw").getChannel();
        //MappedByteBuffer - буфер отображения в память области файла
        //fc.map отображает область файла канала в память
        //MapMode.READ_WRITE - все изменения, которые будут произведены в буфере, будут
        //проиведены в файле
        MappedByteBuffer mem = fc.map(FileChannel.MapMode.READ_ONLY, 0, length);

        byte tempBuffer[] = new byte[(int) length];
        mem.get(tempBuffer);

        return tempBuffer;
    }

}