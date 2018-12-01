import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

public class TestJarsPath {

    public static void main(String[] args) {
        System.getProperty("os.name");
        try {
            HashSet<String> classPath = new HashSet<>();
            HashSet<String> libNames = new HashSet<>();

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
        }
    }
}
