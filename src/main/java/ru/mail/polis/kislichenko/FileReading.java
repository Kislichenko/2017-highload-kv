package ru.mail.polis.kislichenko;

import java.io.IOException;
import java.io.InputStream;

public class FileReading {

    private InputStream in;
    private long length;//количество байтов, которое нужно прочитать из потока

    public FileReading(InputStream newIn, long newLength){
        this.in = newIn;
        this.length = newLength;
    }

    /*
    Алгоритм чтения файлов до конца. Информация сначала записывается в главный массив smallArray,
    который ВОЗМОЖНО занимает меньше памяти, чем было выдано программе. Приведенный алгоритм в классе
    MyHardDAO  учитывает размер свободной памяти в JVM, поэтому размер массива не превышает 70% от свободной
    памяти. Следовательно, может учитываться то, что нам было выделено 1Gb памяти. В данном алгоритме используется
    обычный массив для буффера, чтобы уменьшить потребление памяти (прочитать большие файлы, не загоняя
    в память эти файлы) и увеличить скорость чтения.
     */

    public byte[] getFindByteArray() throws IOException{

        int bufferSize;//размер буфера
        byte smallArray[] = new byte[(int) length];//массив, в котором хранится считываемая информация

        bufferSize = 8 * 1024 * 1024;//8Mb
        byte tempBuffer[] = new byte[bufferSize];

        int tmp = 0;

       //сначала читается файл пока не заполнится основной массив, потом остатки (если есть)
        while (in.available() > 0) {
            if (in.available() < bufferSize) bufferSize = in.available();

            if (tmp * bufferSize < length) {
                in.read(smallArray, tmp * bufferSize, (int) length);
                tmp++;
            } else if (length - tmp * bufferSize > 0) {
                in.read(smallArray, (tmp - 1) * bufferSize, (int) (length - tmp * bufferSize));
                tmp++;
            } else in.read(tempBuffer, 0, bufferSize);
        }

        in.close();
        return smallArray;
    }

}
