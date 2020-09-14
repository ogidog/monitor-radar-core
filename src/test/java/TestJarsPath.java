import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class TestJarsPath {

    public static void main(String[] args) {

        long startTime = System.nanoTime();

        // Создаем рандомный массив
        int N = 100000000;
        int[] in = new int[N];
        Random r = new Random();
        for (int i = 0; i < N; i++) {
            in[i] = r.nextInt(N);
        }

        int dupCounter[] = new int[N];
        long endTime = System.nanoTime();
        long execTime = (endTime - startTime) / 1000000000;
        System.out.println("Время создания рандомного массива: " + Long.toString(execTime) + " c.");

        startTime = System.nanoTime();
        for (int i = 0; i < N; i++) {
            dupCounter[in[i]] += 1;
        }

        endTime = System.nanoTime();
        execTime = (endTime - startTime) / 1000000000;
        System.out.println("Время посчета дубликатов: " + Long.toString(execTime) + " c.");


        startTime = System.nanoTime();
        int out[][] = new int[N][];
        for (int i = 0; i < N; i++) {
            if (dupCounter[i] > 1) {
                out[i] = new int[dupCounter[i]];
            }
        }
        endTime = System.nanoTime();
        execTime = (endTime - startTime) / 1000000000;
        System.out.println("Время создание выходного массива: " + Long.toString(execTime) + " c.");

        startTime = System.nanoTime();
        int value;
        for (int i = 0; i < N; i++) {
            value = in[i];
            if (out[value] != null) {
                out[value][dupCounter[value] - 1] = i;
                dupCounter[value] -= 1;
            }
        }
        endTime = System.nanoTime();
        execTime = (endTime - startTime) / 1000000000;
        System.out.println("Время поиска повторяющихся элементов и их индексов: " + Long.toString(execTime) + " c.");

        return;


        /*System.getProperty("os.name");
        try {
            HashSet<String> classPath = new HashSet<>();
            HashSet<String> libNames = new HashSet<>();

            String images = Files.find(Paths.get("Y:\\Satellites\\Sentinel-1A\\1"), 99,
                    (path, attr) -> {
                        if (path.toString().endsWith(".zip")) {
                            return true;
                        } else {
                            return false;
                        }
                    }).map(path -> path.toString())
                    .map(path->path.replace("Y:","/mnt/satimg").replace("\\","/"))
                    .collect(Collectors.joining(","));


            Files.find(Paths.get("C:\\Program Files\\snap\\snap\\modules\\ext"), 99,
                    (path, attr) -> {
                        if (path.toString().endsWith(".jar")) {
                            return true;
                        } else {
                            return false;
                        }
                    }).map(path -> path.getParent().toString())
                    .forEach(path -> {
                        if (!libNames.contains(path.substring(path.lastIndexOf("\\")))) {
                            classPath.add(path);
                            libNames.add(path.substring(path.lastIndexOf("\\")));
                        }
                    });

            Files.find(Paths.get("C:\\Program Files\\snap\\s1tbx\\modules\\ext"), 99,
                    (path, attr) -> {
                        if (path.toString().endsWith(".jar")) {
                            return true;
                        } else {
                            return false;
                        }
                    }).map(path -> path.getParent().toString())
                    .forEach(path -> {
                        if (!libNames.contains(path.substring(path.lastIndexOf("\\")))) {
                            classPath.add(path);
                            libNames.add(path.substring(path.lastIndexOf("\\")));
                        }
                    });

            String tmp1 = classPath.stream().map(path -> {
                return path.replace("C:\\Program Files\\snap", "/usr/local/snap").replace("\\", "/")
                        + "/*";
            }).collect(Collectors.joining(":"))+"/usr/local/snap/snap/modules*:/usr/local/snap/s1tbx/modules/*";

            return;
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }
}
