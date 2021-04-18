
package org.myapp.satellite.radar.squeesar;

import org.esa.s1tbx.commons.Sentinel1Utils;
import org.esa.s1tbx.sentinel1.gpf.TOPSARSplitOp;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.satellite.radar.squeesar.TOPSARSplitOpEnv;
import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;


public class Stage1 {

    public static String[] getTopSarSplitParameters(String file, HashMap stageParameters) {

        try {

            HashMap subsetParameters = (HashMap) stageParameters.get("Subset");
            HashMap topSarSplitParameters = (HashMap) stageParameters.get("TOPSARSplit");
            HashMap dataSetParameters = (HashMap) stageParameters.get("DataSet");

            String dbms = dataSetParameters.get("databaseIp").toString(); // "172.16.1.4"; // "10.101.80.252";

            Class.forName("org.postgresql.Driver");
            Connection connection = DriverManager
                    .getConnection(
                            "jdbc:postgresql://" + dbms + ":5432/" + dataSetParameters.get("databaseName").toString(),
                            dataSetParameters.get("databaseLogin").toString(), dataSetParameters.get("databasePasswd").toString());
            connection.setAutoCommit(false);

            Product sourceProduct1 = ProductIO.readProduct(new File(file));
            Sentinel1Utils s1u = new Sentinel1Utils(sourceProduct1);
            Sentinel1Utils.SubSwathInfo[] subSwathInfos = s1u.getSubSwath();
            sourceProduct1.closeIO();
            sourceProduct1.dispose();

            HashMap subSwathInfo = new HashMap<String, Integer>();
            for (int i = 0; i < subSwathInfos.length; i++) {
                subSwathInfo.put(subSwathInfos[i].subSwathName, subSwathInfos[i].numOfBursts);
            }

            String subSwathName = "";
            int numOfBurst = 0;
            String splittedSwathGeoRegionWKT = "", splittedBurstOfSwathGeoRegionWKT = "";
            String subsetGeoRegionWKT = "POLYGON((" + subsetParameters.get("geoRegion").toString() + "))";
            String sql = "";

            for (int i = 0; i < subSwathInfo.keySet().size(); i++) {

                Product sourceProduct = ProductIO.readProduct(new File(file));

                OperatorSpi spi = new TOPSARSplitOp.Spi();
                TOPSARSplitOp op = (TOPSARSplitOp) spi.createOperator();
                op.setSourceProduct(sourceProduct);

                op.setParameter("selectedPolarisations", topSarSplitParameters.get("selectedPolarisations"));
                op.setParameter("subswath", subSwathInfos[i].subSwathName);
                op.setParameter("firstBurstIndex", 1);
                op.setParameter("lastBurstIndex", subSwathInfos[i].numOfSamples);

                Product targetProduct = op.getTargetProduct();
                splittedSwathGeoRegionWKT = "POLYGON((" + targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("first_near_long") + ' '
                        + targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("first_near_lat") + ','
                        + targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("first_far_long") + ' '
                        + targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("first_far_lat") + ','
                        + targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("last_far_long") + ' '
                        + targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("last_far_lat") + ','
                        + targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("last_near_long") + ' '
                        + targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("last_near_lat") + ','
                        + targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("first_near_long") + ' '
                        + targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("first_near_lat") + "))";

                sql = "SELECT ST_AsText(ST_Intersection(ST_GeomFromText('" + splittedSwathGeoRegionWKT + "'),ST_GeomFromText('" + subsetGeoRegionWKT + "')))";
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(sql);
                if (rs.next()) {
                    String resultColumn = rs.getString(1);
                    if (!resultColumn.contains("EMPTY")) {

                        resultColumn = resultColumn.replace("POLYGON((", "").replace("))", "");
                        String intersectionGeoRegion = resultColumn + resultColumn.substring(resultColumn.lastIndexOf(","));

                        subSwathName = subSwathInfo.keySet().toArray()[i].toString();
                        numOfBurst = (int) subSwathInfo.get(subSwathName);

                        statement.close();
                        rs.close();

                        break;
                    } else {
                        statement.close();
                        rs.close();
                    }
                }

                sourceProduct.closeIO();
                sourceProduct.dispose();
                targetProduct.closeIO();
                targetProduct.dispose();

                op.dispose();
            }

            int firstBurstIndex = -1;
            int lastBurstIndex = -1;

            for (int burst = 1; burst < numOfBurst + 1; burst++) {

                Product sourceProduct = ProductIO.readProduct(new File(file));

                OperatorSpi spi = new TOPSARSplitOp.Spi();
                TOPSARSplitOp op = (TOPSARSplitOp) spi.createOperator();
                op.setSourceProduct(sourceProduct);

                op.setParameter("selectedPolarisations", topSarSplitParameters.get("selectedPolarisations"));
                op.setParameter("subswath", subSwathName);
                op.setParameter("firstBurstIndex", burst);
                op.setParameter("lastBurstIndex", burst);

                Product targetProduct = op.getTargetProduct();

                splittedBurstOfSwathGeoRegionWKT = "POLYGON((" + targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("first_near_long") + ' '
                        + targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("first_near_lat") + ','
                        + targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("first_far_long") + ' '
                        + targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("first_far_lat") + ','
                        + targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("last_far_long") + ' '
                        + targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("last_far_lat") + ','
                        + targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("last_near_long") + ' '
                        + targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("last_near_lat") + ','
                        + targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("first_near_long") + ' '
                        + targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("first_near_lat") + "))";

                sql = "SELECT ST_AsText(ST_Intersection(ST_GeomFromText('" + splittedBurstOfSwathGeoRegionWKT + "'),ST_GeomFromText('" + subsetGeoRegionWKT + "')))";
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(sql);

                if (rs.next()) {
                    String resultColumn = rs.getString(1);

                    if (!resultColumn.contains("EMPTY")) {
                        if (firstBurstIndex == -1) {
                            firstBurstIndex = burst;
                            lastBurstIndex = burst;
                            continue;
                        } else {
                            lastBurstIndex = burst;
                            continue;
                        }
                    }
                }

                sourceProduct.closeIO();
                sourceProduct.dispose();
                targetProduct.closeIO();
                targetProduct.dispose();

                op.dispose();

                statement.close();
                rs.close();
            }
            connection.close();

            if (firstBurstIndex != -1 && lastBurstIndex != -1) {
                return new String[]{subSwathName, String.valueOf(firstBurstIndex), String.valueOf(lastBurstIndex)};

            } else {
                return new String[]{};
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String configDir = consoleParameters.get("configDir").toString();
            String graphDir = consoleParameters.get("graphDir").toString();
            String filesList = consoleParameters.get("filesList").toString();

            HashMap parameters = getParameters(configDir);
            if (parameters == null) {
                System.out.println("Fail to read parameters.");
                return;
            }

            String[] files;
            if (!filesList.contains(",")) {
                files = Files.walk(Paths.get(filesList)).skip(1)
                        .map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);
            } else {
                files = filesList.split(",");
            }

            if (Files.exists(Paths.get(outputDir))) {
                Files.walk(Paths.get(outputDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }

            String stage1Dir = outputDir + "" + File.separator + "stage1";

            new File(outputDir).mkdirs();
            new File(outputDir + File.separator + "applyorbitfile").mkdirs();
            new File(stage1Dir).mkdirs();

            String graphFile = "applyorbitfile.xml";
            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            PrintWriter cmdWriter = new PrintWriter(stage1Dir + File.separator + "stage1.cmd", "UTF-8");

            for (int i = 0; i < files.length; i++) {

                //TOPSARSplitOpEnv topsarSplitOpEnv = new TOPSARSplitOpEnv();
                //topsarSplitOpEnv.initSplitParameters(files[i], parameters);

                String[] topSarSplitParameters = getTopSarSplitParameters(files[i], parameters);

                graph.getNode("Read").getConfiguration().getChild("file").setValue(files[i]);
                graph.getNode("Write").getConfiguration().getChild("file")
                        .setValue(outputDir + File.separator + "applyorbitfile" + File.separator
                                + Paths.get(files[i]).getFileName().toString().replace(".zip", "") + "_Orb.dim");
                graph.getNode("TOPSAR-Split").getConfiguration().getChild("subswath").setValue(topSarSplitParameters[0]);
                graph.getNode("TOPSAR-Split").getConfiguration().getChild("firstBurstIndex").setValue(topSarSplitParameters[1]);
                graph.getNode("TOPSAR-Split").getConfiguration().getChild("lastBurstIndex").setValue(topSarSplitParameters[2]);

                FileWriter fileWriter = new FileWriter(stage1Dir + File.separator
                        + Paths.get(files[i]).getFileName().toString().replace(".zip", "") + ".xml");
                GraphIO.write(graph, fileWriter);
                fileWriter.flush();
                fileWriter.close();

                cmdWriter.println("gpt " + stage1Dir + File.separator
                        + Paths.get(files[i]).getFileName().toString().replace(".zip", "") + ".xml");

            }

            cmdWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    static HashMap getParameters(String configDir) {

        HashMap<String, HashMap> stageParameters = null;

        try {
            stageParameters = new HashMap<>();

            // DataSet
            JSONParser parser = new JSONParser();
            FileReader fileReader = new FileReader(configDir + File.separator + "dataset.json");
            JSONObject jsonObject = (JSONObject) parser.parse(fileReader);
            HashMap<String, HashMap> jsonParameters1 = (HashMap) jsonObject.get("parameters");

            stageParameters.put("DataSet",
                    (HashMap) jsonParameters1.entrySet().stream
                            ().collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue().get("value")))
            );

            // TOPSARSplit
            fileReader = new FileReader(configDir + File.separator + "s1_tops_split.json");
            jsonObject = (JSONObject) parser.parse(fileReader);
            HashMap jsonParameters = (HashMap) jsonObject.get("parameters");
            jsonParameters1 = (HashMap) jsonObject.get("parameters");

            stageParameters.put("TOPSARSplit",
                    (HashMap) jsonParameters1.entrySet().stream
                            ().collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue().get("value")))
            );

            fileReader.close();

            // ApplyOrbitFile
            fileReader = new FileReader(configDir + File.separator + "apply_orbit_file.json");
            jsonObject = (JSONObject) parser.parse(fileReader);
            jsonParameters = (HashMap) jsonObject.get("parameters");

            HashMap parameters = new HashMap();
            parameters.put("polyDegree", Integer.valueOf(((HashMap) jsonParameters.get("polyDegree")).get("value").toString()));
            parameters.put("continueOnFail", Boolean.valueOf(((HashMap) jsonParameters.get("continueOnFail")).get("value").toString()));
            parameters.put("orbitType", ((HashMap) jsonParameters.get("orbitType")).get("value"));
            stageParameters.put("ApplyOrbitFile", parameters);

            fileReader.close();

            // Subset
            fileReader = new FileReader(configDir + File.separator + "subset.json");
            jsonObject = (JSONObject) parser.parse(fileReader);
            jsonParameters = (HashMap) jsonObject.get("parameters");

            String geoRegionCoordinates = ((HashMap) jsonParameters.get("geoRegion")).get("value").toString();
            parameters = new HashMap();
            parameters.put("geoRegion", geoRegionCoordinates);
            stageParameters.put("Subset", parameters);

            fileReader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return stageParameters;
    }

}
