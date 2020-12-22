import java.awt.image.Raster;
import java.util.Map;

/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */
public class Rasterer {
    /** The max image depth level. */
    public static final int MAX_DEPTH = 7;

    /**
     * Takes a user query and finds the grid of images that best matches the query. These images
     * will be combined into one big image (rastered) by the front end. The grid of images must obey
     * the following properties, where image in the grid is referred to as a "tile".
     * <ul>
     *     <li>The tiles collected must cover the most longitudinal distance per pixel (LonDPP)
     *     possible, while still covering less than or equal to the amount of longitudinal distance
     *     per pixel in the query box for the user viewport size.</li>
     *     <li>Contains all tiles that intersect the query bounding box that fulfill the above
     *     condition.</li>
     *     <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     * </ul>
     * @param params The RasterRequestParams containing coordinates of the query box and the browser
     *               viewport width and height.
     * @return A valid RasterResultParams containing the computed results.
     */
    public RasterResultParams getMapRaster(RasterRequestParams params) {
        System.out.println(
                "Since you haven't implemented getMapRaster, nothing is displayed in the browser.");

        /* TODO: Make sure you can explain every part of the task before you begin.
         * Hint: Define additional classes to make it easier to pass around multiple values, and
         * define additional methods to make it easier to test and reason about code. */

        //Query to the left or right of Root Latitudes
        if (params.ullat > MapServer.ROOT_ULLAT && params.lrlat > MapServer.ROOT_ULLAT
                || params.lrlat < MapServer.ROOT_LRLAT && params.ullat < MapServer.ROOT_LRLAT) {
            return RasterResultParams.queryFailed();
        }
        //Query above or below Root longitudes
        if (params.ullon < MapServer.ROOT_ULLON && params.lrlon < MapServer.ROOT_ULLON
                || params.lrlon > MapServer.ROOT_LRLON && params.ullon > MapServer.ROOT_LRLON) {
            return RasterResultParams.queryFailed();
        }
        //Query bigger than Root
        if (params.ullat > MapServer.ROOT_ULLAT && params.lrlat < MapServer.ROOT_LRLAT
                && params.ullon < MapServer.ROOT_ULLON && params.lrlon > MapServer.ROOT_LRLON) {
            return RasterResultParams.queryFailed();
        }
        //Last case
        if (params.ullat < MapServer.ROOT_LRLAT || params.lrlat > MapServer.ROOT_ULLAT) {
            return RasterResultParams.queryFailed();
        }
        if (params.ullon > MapServer.ROOT_LRLON || params.lrlon < MapServer.ROOT_ULLON) {
            return RasterResultParams.queryFailed();
        }

        double desiredlongDPP = lonDPP(params.lrlon, params.ullon, params.w);
        //figure out dimension
        int d = 7; //changed to 7 to make it the default
        for (int i = 0; i < 8; i++) {
            //double lonDPP = ((params.lrlon - params.ullon) / Math.pow(2, i) / 256);
            double numOfBoxes = Math.pow(2, i);
            double lonPerBox = MapServer.ROOT_LON_DELTA / numOfBoxes;
            double boxLrLon = MapServer.ROOT_ULLON + lonPerBox;
            double boxUlLon = MapServer.ROOT_ULLON;
            double boxLonDPP = lonDPP(boxLrLon, boxUlLon, 256);
            if (boxLonDPP <= desiredlongDPP) {
                d = i;
                break;
            }
        }

        //figure out which images we want
        int topY = topY(params.ullat, params.lrlat, d);
        int bottomY = bottomY(params.ullat, params.lrlat, d);
        int leftX = leftX(params.ullon, params.lrlon, d);
        int rightX = rightX(params.ullon, params.lrlon, d);

        //create renderGrid array with .png file names
        String[][] renderGrid = new String[bottomY - topY + 1][rightX - leftX + 1];
        int outerIndex = 0;
        for (int j = topY; j <= bottomY; j++) {
            int index = 0;
            for (int k = leftX; k <= rightX; k++) {
                renderGrid[outerIndex][index] = "d" + d + "_x" + k + "_y" + j + ".png";
                index += 1;
            }
            outerIndex += 1;
            if (outerIndex == renderGrid.length) {
                break;
            }
        }

        //Get Args for Builder
        double numOfBoxes = Math.pow(2, d);
        double latPerBox = MapServer.ROOT_LAT_DELTA / numOfBoxes;
        double lonPerBox = MapServer.ROOT_LON_DELTA / numOfBoxes;
        double rasterULlon = MapServer.ROOT_ULLON + (leftX * lonPerBox);
        double rasterLRlon = MapServer.ROOT_ULLON + ((rightX + 1) * lonPerBox);
        double rasterULlat = MapServer.ROOT_ULLAT - (topY * latPerBox);
        double rasterLRlat = MapServer.ROOT_ULLAT - ((bottomY + 1) * latPerBox);

        //create object to return
        RasterResultParams.Builder builder = new RasterResultParams.Builder();
        builder.setRenderGrid(renderGrid);
        builder.setDepth(d);
        builder.setRasterLrLat(rasterLRlat);
        builder.setQuerySuccess(true);
        builder.setRasterLrLon(rasterLRlon);
        builder.setRasterUlLat(rasterULlat);
        builder.setRasterUlLon(rasterULlon);
        return builder.create();
    }

    public int topY (double ullat, double lrlat, int d) {
        double numOfBoxes = Math.pow(2, d);
        double latTotal = MapServer.ROOT_LAT_DELTA;
        double latPerBox = latTotal / numOfBoxes;
        double ULLat = MapServer.ROOT_ULLAT;
        double LRLat = (MapServer.ROOT_ULLAT) - latPerBox;

        for (int i = 0; i < numOfBoxes; i++) {
            if (ullat < ULLat && ullat > LRLat) {
                return i;
            }
            double temp = LRLat;
            ULLat = LRLat;
            LRLat = temp - latPerBox;
        }
        return 0;
    }

    public int bottomY (double ullat, double lrlat, int d) {
        double numOfBoxes = Math.pow(2, d);
        double latTotal = MapServer.ROOT_LAT_DELTA;
        double latPerBox = latTotal / numOfBoxes;
        double ULLat = MapServer.ROOT_LRLAT + latPerBox;
        double LRLat = MapServer.ROOT_LRLAT;

        for (int i = (int) (numOfBoxes - 1); i >= 0; i--) {
            if (lrlat < ULLat && lrlat > LRLat ) {
                return i;
            }
            double temp = ULLat;
            LRLat = ULLat;
            ULLat = temp + latPerBox;
        }
        return (int) (numOfBoxes - 1);
    }

    public int leftX (double ullon, double lrlon, int d) {
        double numOfBoxes = Math.pow(2, d);
        double lonTotal = MapServer.ROOT_LON_DELTA;
        double lonPerBox = lonTotal / numOfBoxes;
        double ULLon = MapServer.ROOT_ULLON;
        double LRLon = (MapServer.ROOT_ULLON) + lonPerBox;

        for (int i = 0; i < numOfBoxes; i++) {
            if (ullon > ULLon && ullon < LRLon) {
                return i;
            }
            double temp = LRLon;
            ULLon = LRLon;
            LRLon = temp + lonPerBox;
        }
        return 0;
    }

    public int rightX (double ullon, double lrlon, int d) {
        double numOfBoxes = Math.pow(2, d);
        double lonTotal = MapServer.ROOT_LON_DELTA;
        double lonPerBox = lonTotal / numOfBoxes;
        double ULLon = MapServer.ROOT_LRLON - lonPerBox;
        double LRLon = MapServer.ROOT_LRLON;

        for (int i = (int) (numOfBoxes - 1); i >= 0; i--) {
            if (lrlon > ULLon && lrlon < LRLon) {
                return i;
            }
            double temp = ULLon;
            LRLon = ULLon;
            ULLon = temp - lonPerBox;
        }
        return (int) numOfBoxes - 1;
    }



    /**
     * Calculates the lonDPP of an image or query box
     * @param lrlon Lower right longitudinal value of the image or query box
     * @param ullon Upper left longitudinal value of the image or query box
     * @param width Width of the query box or image
     * @return lonDPP
     */
    private double lonDPP(double lrlon, double ullon, double width) {
        return (lrlon - ullon) / width;
    }
}