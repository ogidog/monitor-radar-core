package org.myapp.satellite.radar.squeesar;

import org.esa.s1tbx.commons.Sentinel1Utils;
import org.esa.s1tbx.sentinel1.gpf.TOPSARSplitOp;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorSpi;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;

public class TOPSARSplitOpEnv {

    HashMap subsetParameters, topSarSplitParameters, dataSetParameters;

    int firstBurstIndex, lastBurstIndex;
    String subSwathName;

    public Boolean initSplitParameters(String file, HashMap stageParameters) {

        try {

            subsetParameters = (HashMap) stageParameters.get("Subset");
            topSarSplitParameters = (HashMap) stageParameters.get("TOPSARSplit");
            dataSetParameters = (HashMap) stageParameters.get("DataSet");

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
                        numOfBurst = (int)subSwathInfo.get(subSwathName);

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

            firstBurstIndex = -1;
            lastBurstIndex = -1;

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
                return true;

            } else {
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getSubSwath() {
        return subSwathName;
    }

    public String getFirstBurstIndex() {
        return String.valueOf(firstBurstIndex);
    }

    public String getLastBurstIndex() {
        return String.valueOf(lastBurstIndex);
    }

}