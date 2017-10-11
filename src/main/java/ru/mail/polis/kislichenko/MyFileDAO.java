package ru.mail.polis.kislichenko;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.NoSuchElementException;

public class MyFileDAO implements MyDAO{

    @NotNull
    private final File dir;

    public MyFileDAO(@NotNull final File dir){
        this.dir = dir;
    }

    @NotNull
    @Override
    public byte[] get(@NotNull final String key) throws NoSuchElementException, IllegalArgumentException, IOException {

            final File file = getFile(key);

            /*проверяется существование файла, т.е. если были спользованы недопустимые для имени файла символы,
            ОС не даст создать их, и выйдет исключение. Следовательно, таким образом учитываютя недопустимые ключи.
             */
            if(!file.exists()){
                throw new NoSuchElementException("File with key= " + key + " doesn't exist!");
            }

            InputStream in = new FileInputStream(file);
            FileReading readFile = new FileReading(in, file.length());

            return readFile.getFindByteArray();
    }

    @NotNull
    private File getFile(@NotNull final String key) throws IOException{
        return new File(dir, key);
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

}