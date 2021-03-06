package org.myapp.satellite.radar.shared;

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

    Connection connection = null;

    Product sourceProduct, targetProduct;
    OperatorSpi spi;
    TOPSARSplitOp op;
    Sentinel1Utils s1u;
    Sentinel1Utils.SubSwathInfo[] subSwathInfos;
    HashMap subsetParameters, topSarSplitParameters, dataSetParameters;

    String intersectionGeoRegion = "";
    int firstBurstIndex, lastBurstIndex;
    String subSwathName;

    public Boolean getSplitParameters(String file, HashMap stageParameters) {

        try {

            subsetParameters = (HashMap) stageParameters.get("Subset");
            topSarSplitParameters = (HashMap) stageParameters.get("TOPSARSplit");
            dataSetParameters = (HashMap) stageParameters.get("DataSet");

            String dbms = dataSetParameters.get("databaseIp").toString(); // "172.16.1.4"; // "10.101.80.252";

            Class.forName("org.postgresql.Driver");
            connection = DriverManager
                    .getConnection(
                            "jdbc:postgresql://" + dbms + ":5432/" + dataSetParameters.get("databaseName").toString(),
                            dataSetParameters.get("databaseLogin").toString(), dataSetParameters.get("databasePasswd").toString());
            connection.setAutoCommit(false);

            sourceProduct = ProductIO.readProduct(new File(file));
            s1u = new Sentinel1Utils(sourceProduct);

            subSwathInfos = s1u.getSubSwath();

            int numOfBurst = 0;
            String splittedSwathGeoRegionWKT = "", splittedBurstOfSwathGeoRegionWKT = "";
            String subsetGeoRegionWKT = "POLYGON((" + subsetParameters.get("geoRegion").toString() + "))";
            String sql = "";

            for (int i = 0; i < subSwathInfos.length; i++) {

                spi = new TOPSARSplitOp.Spi();
                op = (TOPSARSplitOp) spi.createOperator();
                op.setSourceProduct(sourceProduct);

                op.setParameter("selectedPolarisations", topSarSplitParameters.get("selectedPolarisations"));
                op.setParameter("subswath", subSwathInfos[i].subSwathName);
                op.setParameter("firstBurstIndex", 1);
                op.setParameter("lastBurstIndex", subSwathInfos[i].numOfBursts);

                targetProduct = op.getTargetProduct();
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

                sql = "SELECT ST_AsText(ST_Intersection(ST_GeomFromText('" + splittedSwathGeoRegionWKT + "'),ST_GeomFromText('" + subsetGeoRegionWKT + "'))), " +
                        "ST_Area(ST_Intersection(ST_GeomFromText('" + splittedSwathGeoRegionWKT + "'),ST_GeomFromText('" + subsetGeoRegionWKT + "'))::geography)/1000000 as intersect_area, " +
                        "ST_Area(ST_GeomFromText('" + subsetGeoRegionWKT + "')::geography)/1000000 as subset_area";
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(sql);
                if (rs.next()) {
                    String resultColumn = rs.getString(1);
                    float intersectArea = rs.getFloat(2);
                    float subsetArea = rs.getFloat(3);
                    if ((intersectArea / subsetArea) * 100 < 80) {
                        continue;
                    }
                    if (!resultColumn.contains("EMPTY")) {
                        resultColumn = resultColumn.replace("POLYGON((", "").replace("))", "");
                        intersectionGeoRegion = resultColumn + resultColumn.substring(resultColumn.lastIndexOf(","));

                        subSwathName = subSwathInfos[i].subSwathName;
                        numOfBurst = subSwathInfos[i].numOfBursts;

                        statement.close();
                        rs.close();

                        break;
                    } else {
                        statement.close();
                        rs.close();
                    }
                }
            }

            firstBurstIndex = -1;
            lastBurstIndex = -1;

            for (int burst = 1; burst < numOfBurst + 1; burst++) {

                spi = new TOPSARSplitOp.Spi();
                op = (TOPSARSplitOp) spi.createOperator();
                op.setSourceProduct(sourceProduct);

                op.setParameter("selectedPolarisations", topSarSplitParameters.get("selectedPolarisations"));
                op.setParameter("subswath", subSwathName);
                op.setParameter("firstBurstIndex", burst);
                op.setParameter("lastBurstIndex", burst);

                targetProduct = op.getTargetProduct();

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
                            targetProduct.closeIO();
                            op.dispose();
                            continue;
                        } else {
                            lastBurstIndex = burst;
                            targetProduct.closeIO();
                            op.dispose();
                            continue;
                        }
                    }
                }

                statement.close();
                rs.close();

                op.dispose();
            }

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

    public void Dispose() {
        try {

            this.targetProduct.closeIO();
            targetProduct = null;

            this.sourceProduct.closeIO();
            sourceProduct = null;

            op.dispose();
            op = null;
            spi = null;

            s1u = null;

            subSwathInfos = null;

            connection.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}