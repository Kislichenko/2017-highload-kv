package ru.mail.polis.kislichenko;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class MyFileDAO implements MyDAO {

    @NotNull
    private final File dir;
    @NotNull
    private HashSet<String> deletedSet;

    public MyFileDAO(@NotNull final File dir) {

        this.dir = dir;
        this.deletedSet = new HashSet<>();
    }

    public boolean containsDeletedKey(@NotNull final String key) {
        if (deletedSet.contains(key)) return true;
        return false;
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
            removeOrAdd(key, true);
        }
    }

    @Override
    public void delete(@NotNull final String key) throws IllegalArgumentException, IOException {
        getFile(key).delete();
        removeOrAdd(key, false);
    }

    private void removeOrAdd(@NotNull final String key, boolean remove) throws IllegalArgumentException, IOException {
        final File file = getFile("deleted");
        file.createNewFile();
        if (!file.exists()) {
            throw new NoSuchElementException("File with key= " + key + " doesn't exist!");
        }

        FileInputStream fstream = new FileInputStream(file);
        BufferedReader in = new BufferedReader(new InputStreamReader(fstream));
        String strLine;
        HashSet<String> mySet = new HashSet<>();

        while ((strLine = in.readLine()) != null) {
            mySet.add(strLine);
        }

        if (!remove) mySet.add(key);
        else if (mySet.contains(key)) mySet.remove(new String(key));
        deletedSet = mySet;
        in.close();

        BufferedWriter out = new BufferedWriter(new FileWriter(file));
        Iterator it = mySet.iterator();

        while (it.hasNext()) {
            out.write((String) it.next());
            out.newLine();
        }
        out.close();
    }

    public boolean checkId(@NotNull final String key) throws IllegalArgumentException, IOException {
        File file = new File(dir, key);
        if (file.exists()) return true;
        return false;
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