import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class TestAddNewImagesToDB {

    public static void main(String[] args) {
        try {
            String images = Files.find(Paths.get("Y:\\Satellites\\Sentinel-1A\\1"), 99,
                    (path, attr) -> {
                        if (path.toString().endsWith(".zip")) {
                            return true;
                        } else {
                            return false;
                        }
                    }).map(path -> path.toString())
                    .map(path -> path.replace("Y:", "/mnt/satimg").replace("\\", "/"))
                    .collect(Collectors.joining(","));
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
