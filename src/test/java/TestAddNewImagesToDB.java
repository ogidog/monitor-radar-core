import org.myapp.satellite.radar.manager.DBManager;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class TestAddNewImagesToDB {

    public static void main(String[] args) {
        String imagePath = "Y:\\Sentinel-1A\\Kuzbass\\165\\";
        String filter = "_1SDV_2018";

        try {
            String images = Files.find(Paths.get(imagePath), 99,
                    (path, attr) -> {
                        if (path.toString().endsWith(".zip") && path.toString().contains(filter)) {
                            return true;
                        } else {
                            return false;
                        }
                    }).map(path -> path.getFileName().toString())
                    .collect(Collectors.joining(","));

            DBManager.main(new String[]{"addNewImageToDB", images, imagePath, "/mnt/satimg/Satellites/Sentinel-1A/" });

        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
