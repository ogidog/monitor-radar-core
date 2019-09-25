package org.myapp.satellite.radar.mintpy;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import org.esa.s1tbx.commons.Sentinel1Utils;
import org.esa.s1tbx.sentinel1.gpf.TOPSARSplitOp;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorSpi;

import java.io.File;
import java.util.HashMap;

public class TOPSARSplitOpEnv {

    String targetProductName;
    Product sourceProduct, targetProduct;
    OperatorSpi spi;
    TOPSARSplitOp op;
    Sentinel1Utils s1u;
    Sentinel1Utils.SubSwathInfo[] subSwathInfos;

    public Product getTargetProduct(String file, HashMap stageParameters) {

        try {

            HashMap subsetParameters = (HashMap) stageParameters.get("Subset");
            HashMap topSarSplitParameters = (HashMap) stageParameters.get("TOPSARSplit");

            sourceProduct = ProductIO.readProduct(new File(file));
            s1u = new Sentinel1Utils(sourceProduct);

            GeoPos topLeftGeoPos = new GeoPos();
            topLeftGeoPos.setLocation((double) subsetParameters.get("topLeftLat"), (double) subsetParameters.get("topLeftLon"));
            PixelPos topLeftPixelPos = new PixelPos();
            sourceProduct.getSceneGeoCoding().getPixelPos(topLeftGeoPos, topLeftPixelPos);

            subSwathInfos = s1u.getSubSwath();
            int numOfBurst = 0;
            for (int i = 0; i < subSwathInfos.length; i++) {
                if ((i + 1) * subSwathInfos[i].numOfSamples < topLeftPixelPos.x) {
                    continue;
                }
                topSarSplitParameters.put("subswath", subSwathInfos[i].subSwathName);
                numOfBurst = subSwathInfos[i].numOfBursts;
                break;
            }

            GeometryFactory gf = new GeometryFactory();
            Polygon subsetPolygon = gf.createPolygon(gf.createLinearRing(new Coordinate[]{
                    new Coordinate((double) subsetParameters.get("topLeftLat"), (double) subsetParameters.get("topLeftLon")),
                    new Coordinate((double) subsetParameters.get("topRightLat"), (double) subsetParameters.get("topRightLon")),
                    new Coordinate((double) subsetParameters.get("bottomLeftLat"), (double) subsetParameters.get("bottomLeftLon")),
                    new Coordinate((double) subsetParameters.get("bottomRightLat"), (double) subsetParameters.get("bottomRightLon")),
                    new Coordinate((double) subsetParameters.get("topLeftLat1"), (double) subsetParameters.get("topLeftLon1"))
            }), null);

            int firstBurstIndex = -1, lastBurstIndex = -1;

            Polygon splitPolygon;
            for (int burst = 1; burst < numOfBurst + 1; burst++) {

                spi = new TOPSARSplitOp.Spi();
                op = (TOPSARSplitOp) spi.createOperator();
                op.setSourceProduct(sourceProduct);

                op.setParameter("selectedPolarisations", topSarSplitParameters.get("selectedPolarisations"));
                op.setParameter("subswath", topSarSplitParameters.get("subswath"));
                op.setParameter("firstBurstIndex", burst);
                op.setParameter("lastBurstIndex", burst);

                targetProduct = op.getTargetProduct();

                splitPolygon = gf.createPolygon(gf.createLinearRing(new Coordinate[]{
                        new Coordinate(targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeDouble("first_near_lat"),
                                targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeDouble("first_near_long")),
                        new Coordinate(targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeDouble("first_far_lat"),
                                targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeDouble("first_far_long")),
                        new Coordinate(targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeDouble("last_near_lat"),
                                targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeDouble("last_near_long")
                        ),
                        new Coordinate(targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeDouble("last_far_lat"),
                                targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeDouble("last_far_long")
                        ),
                        new Coordinate(targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeDouble("first_near_lat"),
                                targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeDouble("first_near_long")
                        )
                }), null);

                if (splitPolygon.intersects(subsetPolygon) && firstBurstIndex == -1) {
                    firstBurstIndex = burst;
                    lastBurstIndex = burst;
                    targetProduct.closeIO();
                    op.dispose();
                    continue;
                }

                if (splitPolygon.intersects(subsetPolygon) && firstBurstIndex != -1) {
                    lastBurstIndex = burst;
                    targetProduct.closeIO();
                    op.dispose();
                    continue;
                }

                op.dispose();
            }

            if (firstBurstIndex != -1 && lastBurstIndex != -1) {

                spi = new TOPSARSplitOp.Spi();
                op = (TOPSARSplitOp) spi.createOperator();
                op.setSourceProduct(sourceProduct);

                op.setParameter("selectedPolarisations", topSarSplitParameters.get("selectedPolarisations"));
                op.setParameter("subswath", topSarSplitParameters.get("subswath"));
                op.setParameter("firstBurstIndex", firstBurstIndex);
                op.setParameter("lastBurstIndex", lastBurstIndex);

                targetProduct = op.getTargetProduct();
                return targetProduct;

            } else {
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public int[] getBurstsRange(String file, HashMap parameters) {

        int[] burstRange = null;

        try {

            sourceProduct = ProductIO.readProduct(new File(file));
            s1u = new Sentinel1Utils(sourceProduct);

            GeoPos topLeftGeoPos = new GeoPos();
            topLeftGeoPos.setLocation((double) parameters.get("topLeftLat"), (double) parameters.get("topLeftLon"));
            PixelPos topLeftPixelPos = new PixelPos();
            sourceProduct.getSceneGeoCoding().getPixelPos(topLeftGeoPos, topLeftPixelPos);

            subSwathInfos = s1u.getSubSwath();
            int numOfBurst = 0;
            for (int i = 0; i < subSwathInfos.length; i++) {
                if ((i + 1) * subSwathInfos[i].numOfSamples < topLeftPixelPos.x) {
                    continue;
                }
                parameters.put("subswath", subSwathInfos[i].subSwathName);
                numOfBurst = subSwathInfos[i].numOfBursts;
                break;
            }

            GeometryFactory gf = new GeometryFactory();
            Polygon subsetPolygon = gf.createPolygon(gf.createLinearRing(new Coordinate[]{
                    new Coordinate((double) parameters.get("topLeftLat"), (double) parameters.get("topLeftLon")),
                    new Coordinate((double) parameters.get("topRightLat"), (double) parameters.get("topRightLon")),
                    new Coordinate((double) parameters.get("bottomLeftLat"), (double) parameters.get("bottomLeftLon")),
                    new Coordinate((double) parameters.get("bottomRightLat"), (double) parameters.get("bottomRightLon")),
                    new Coordinate((double) parameters.get("topLeftLat1"), (double) parameters.get("topLeftLon1"))
            }), null);

            int firstBurstIndex = -1, lastBurstIndex = -1;

            Polygon splitPolygon;
            for (int burst = 1; burst < numOfBurst + 1; burst++) {

                spi = new TOPSARSplitOp.Spi();
                op = (TOPSARSplitOp) spi.createOperator();
                op.setSourceProduct(sourceProduct);

                op.setParameter("selectedPolarisations", parameters.get("selectedPolarisations"));
                op.setParameter("subswath", parameters.get("subswath"));
                op.setParameter("firstBurstIndex", burst);
                op.setParameter("lastBurstIndex", burst);

                targetProduct = op.getTargetProduct();
                targetProductName = targetProduct.getName();

                splitPolygon = gf.createPolygon(gf.createLinearRing(new Coordinate[]{
                        new Coordinate(targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeDouble("first_near_lat"),
                                targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeDouble("first_near_long")),
                        new Coordinate(targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeDouble("first_far_lat"),
                                targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeDouble("first_far_long")),
                        new Coordinate(targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeDouble("last_near_lat"),
                                targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeDouble("last_near_long")
                        ),
                        new Coordinate(targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeDouble("last_far_lat"),
                                targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeDouble("last_far_long")
                        ),
                        new Coordinate(targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeDouble("first_near_lat"),
                                targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeDouble("first_near_long")
                        )
                }), null);

                if (splitPolygon.intersects(subsetPolygon) && firstBurstIndex == -1) {
                    firstBurstIndex = burst;
                    lastBurstIndex = burst;
                    targetProduct.closeIO();
                    op.dispose();
                    continue;
                }

                if (splitPolygon.intersects(subsetPolygon) && firstBurstIndex != -1) {
                    lastBurstIndex = burst;
                    targetProduct.closeIO();
                    op.dispose();
                    continue;
                }

                op.dispose();
            }

            if (firstBurstIndex != -1 && lastBurstIndex != -1) {
                burstRange = new int[]{
                        firstBurstIndex, lastBurstIndex
                };
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return burstRange;
    }

    public String getProductName() {
        return targetProductName;
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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
