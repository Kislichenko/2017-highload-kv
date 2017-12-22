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
    private final File dirDeletedSet = new File("./deletedSet");
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

        final int allowMemory = getSizeFromFreeMemory(file);
        //если размер файла меньше допустимой памяти
        if (file.length() < allowMemory) return Files.readAllBytes(Paths.get(dir.toString(), key));
            //если размер файла больше допустимой памяти, то берется первая часть
        else return new StreamReading(new FileInputStream(file), allowMemory).getByteArray();
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
        if (!dirDeletedSet.isDirectory()) dirDeletedSet.mkdir();
        final File file = new File(dirDeletedSet, key);
        file.createNewFile();
        if (!file.exists()) {
            throw new NoSuchElementException("File with key= " + key + " doesn't exist!");
        }

        HashSet<String> mySet = new HashSet<>();
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            String strLine;

            while ((strLine = in.readLine()) != null) mySet.add(strLine);

            if (!remove) mySet.add(key);
            else if (mySet.contains(key)) mySet.remove(key);

            deletedSet = mySet;
            in.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        BufferedWriter out = new BufferedWriter(new FileWriter(file));
        Iterator it = mySet.iterator();

        while (it.hasNext()) {
            out.write((String) it.next());
            out.newLine();
        }
        out.close();
    }

    public boolean checkId(@NotNull final String key) throws IllegalArgumentException {
        File file = new File(dir, key);
        if (file.exists()) return true;
        return false;
    }

    private long getFreeMemory() {
        return Runtime.getRuntime().maxMemory() -
                (
                        Runtime.getRuntime().totalMemory() -
                                Runtime.getRuntime().freeMemory()
                );
    }

    //сколько можно взять свободной памяти, чтобы не навредить работе программы (70%)
    private int getSizeFromFreeMemory(File file) {
        if (file.length() < getFreeMemory() * 0.7) {
            if (file.length() > Integer.MAX_VALUE) return Integer.MAX_VALUE;
            else if (file.length() < Integer.MAX_VALUE) return (int) file.length();
        }
        return (int) (getFreeMemory() * 0.7);
    }

}