package org.myapp.satellite.radar.manager;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.stream.Collectors;

public class DBManager {

    static private String dbms = "10.101.80.252";

    public static void main(String[] args) {

        if (args[0].equals("addNewImageToDB")) {
            String imageNames = args[1];
            String imageRealDir = args[2];
            String imageDirInDBRecord = args[3];
            String fileNameFilter = args[4]; // задается через запятую список строк (или без запятой если толькоодин фильтр), которые должны входит в имя файла, например _2020,_2021 или _2021
            addNewImageToDB(imageNames, imageRealDir, imageDirInDBRecord, fileNameFilter.split(","));
        }
    }

    private static void addNewImageToDB(String imageNames, String imageRealDir, String imageDirInDBRecord, String[] fileNameFilter) {

        if (imageNames.length() == 0) {
            try {
                imageNames = Files.find(Paths.get(imageRealDir), 99,
                        (path, attr) -> {
                            if (path.toString().endsWith(".zip")) {
                                return true;
                            } else {
                                return false;
                            }
                        }).map(path -> path.getFileName().toString())
                        .collect(Collectors.joining(","));

            } catch (Exception e) {
                System.out.println(e);
            }
        }

        Connection connection = null;
        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager
                    .getConnection(
                            "jdbc:postgresql://" + dbms + ":5432/satimgdb",
                            "satimg_adm", "rfnfkjucybvrjd");
            connection.setAutoCommit(false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (connection != null) {

            boolean filterResult = false;
            for (String imageName : imageNames.split(",")) {

                filterResult = false;

                for (String filter : fileNameFilter) {
                    if (imageName.contains(filter)) {
                        filterResult = true;
                        break;
                    }
                }

                if (!filterResult) continue;

                HashMap productMetadata = getMetadata(imageRealDir + imageName);
                if (productMetadata != null) {
                    String sql = "INSERT INTO remote_sensing_data (name, mission, product_type, geom, file_path, polarizations, orbit_cycle, rel_orbit, abs_orbit, proc_time) " +
                            "VALUES (" +
                            "'" + productMetadata.get("name") + "'," +
                            "'" + productMetadata.get("mission") + "'," +
                            "'" + productMetadata.get("product_type") + "'," +
                            productMetadata.get("geom") + ',' +
                            "'" + imageDirInDBRecord + imageName + "'," +
                            "'" + productMetadata.get("polarization") + "'," +
                            productMetadata.get("orbit_cycle") + "," +
                            productMetadata.get("rel_orbit") + "," +
                            productMetadata.get("abs_orbit") + "," +
                            "'" + productMetadata.get("proc_time") + "'" +
                            ")";

                    try {
                        Statement statement = connection.createStatement();
                        statement.executeUpdate(sql);
                        connection.commit();
                    } catch (SQLException e) {
                        e.printStackTrace();
                        try {
                            connection.rollback();
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            }
        }

        try {
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static HashMap getMetadata(String imageFilePath) {
        try {
            HashMap metadata = new HashMap<>();
            Product product = ProductIO.readProduct(imageFilePath);
            MetadataElement rootMetadataElement = product.getMetadataRoot();

            metadata.put("name", rootMetadataElement.getElement("Abstracted_Metadata").getAttributeString("PRODUCT"));
            metadata.put("mission", rootMetadataElement.getElement("Abstracted_Metadata").getAttributeString("MISSION"));
            metadata.put("product_type", rootMetadataElement.getElement("Abstracted_Metadata").getAttributeString("PRODUCT_TYPE"));

            String polarization = Arrays.stream(rootMetadataElement.getElement("Abstracted_Metadata").getAttributes())
                    .filter(attr -> attr.getName().contains("_tx_rx_polar"))
                    .map(attr -> attr.getData().getElemString())
                    .filter(value -> !value.contains("-"))
                    .collect(Collectors.joining(","));
            metadata.put("polarization", polarization);

            metadata.put("orbit_cycle", rootMetadataElement.getElement("Abstracted_Metadata").getAttributeString("orbit_cycle"));
            metadata.put("rel_orbit", rootMetadataElement.getElement("Abstracted_Metadata").getAttributeString("REL_ORBIT"));
            metadata.put("abs_orbit", rootMetadataElement.getElement("Abstracted_Metadata").getAttributeString("ABS_ORBIT"));

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);
            Date date = simpleDateFormat.parse(rootMetadataElement.getElement("Abstracted_Metadata").getAttributeString("PROC_TIME").split(" ")[0]);
            simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            metadata.put("proc_time", simpleDateFormat.format(date));

            Double firstNearLat = rootMetadataElement.getElement("Abstracted_Metadata").getAttribute("first_near_lat").getData().getElemDouble();
            Double firstNearLong = rootMetadataElement.getElement("Abstracted_Metadata").getAttribute("first_near_long").getData().getElemDouble();
            Double firstFarLat = rootMetadataElement.getElement("Abstracted_Metadata").getAttribute("first_far_lat").getData().getElemDouble();
            Double firstFarLong = rootMetadataElement.getElement("Abstracted_Metadata").getAttribute("first_far_long").getData().getElemDouble();
            Double lastNearLat = rootMetadataElement.getElement("Abstracted_Metadata").getAttribute("last_near_lat").getData().getElemDouble();
            Double lastNearLong = rootMetadataElement.getElement("Abstracted_Metadata").getAttribute("last_near_long").getData().getElemDouble();
            Double lastFarLat = rootMetadataElement.getElement("Abstracted_Metadata").getAttribute("last_far_lat").getData().getElemDouble();
            Double lastFarLong = rootMetadataElement.getElement("Abstracted_Metadata").getAttribute("last_far_long").getData().getElemDouble();
            String geom = "ST_GeomFromText('POLYGON(("
                    + firstNearLong + " " + firstNearLat
                    + ","
                    + firstFarLong + " " + firstFarLat
                    + ","
                    + lastFarLong + " " + lastFarLat
                    + ","
                    + lastNearLong + " " + lastNearLat
                    + ","
                    + firstNearLong + " " + firstNearLat
                    + "))', 4326)";
            metadata.put("geom", geom);
            product.closeIO();

            return metadata;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
