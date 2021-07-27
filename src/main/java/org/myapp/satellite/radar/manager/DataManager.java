package org.myapp.satellite.radar.manager;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Year;
import java.util.Base64;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.myapp.security.Credentials;
import org.myapp.utils.ConsoleArgsReader;

public class DataManager {

    static String OAHubURL = "https://" + Credentials.COPERNICUS_USER + ":" + Credentials.COPERNICUS_PASSWORD + "@scihub.copernicus.eu/dhus/";

    /*
    relativeorbitnumber:165
    sensoroperationalmode:IW
    producttype:SLC
    footprint:"Intersects(POLYGON((84.52998795213192 54.65138129891938, 85.4203990394665 53.7511761784223, 86.65906665087286 52.62795102669355, 87.97510662516017 52.09048592333664, 88.82106812011401 52.14372209108452, 88.86635841126693 52.1837190890267, 89.15165814256267 52.37972109021305, 89.25516760367282 52.84500423605002, 89.29772616290136 53.45901508111474, 89.34729633078236 54.21676506189529, 88.82675347772887 54.61907058531327, 89.54098233075624 55.77741355333997, 88.60798919443489 56.90274705069148, 84.16563424557138 56.46039206304616, 84.52998795213192 54.65138129891938)))"
    */

    public static void main(String[] args) {

        HashMap parameters = ConsoleArgsReader.readConsoleArgs(args);
        createDownloaderScript(parameters);
    }

    private static void createDownloaderScript(HashMap parameters) {
        try {

            int currentYear = Year.now().getValue();
            String httpQuery = OAHubURL + "search?rows=100&q="
                    + URLEncoder.encode("footprint:\"Intersects(POLYGON((", "UTF-8") + URLEncoder.encode(parameters.get("roi").toString(), "UTF-8") + URLEncoder.encode(")))\"", "UTF-8")
                    + (parameters.get("producttype").equals("''") ? "" : URLEncoder.encode(" AND producttype:", "UTF-8") + parameters.get("producttype"))
                    + URLEncoder.encode(" AND ingestiondate:[", "UTF-8") + currentYear
                    + URLEncoder.encode("-01-01T00:00:00.000Z TO ", "UTF-8") + currentYear
                    + URLEncoder.encode("-12-31T00:00:00.000Z]", "UTF-8")
                    + (parameters.get("relativeorbitnumber").equals("''") ? "" : URLEncoder.encode(" AND relativeorbitnumber:", "UTF-8") + parameters.get("relativeorbitnumber"))
                    + (parameters.get("sensoroperationalmode").equals("''") ? "" : URLEncoder.encode(" AND sensoroperationalmode:", "UTF-8") + parameters.get("sensoroperationalmode"))
                    + "&format=json";

            String httpQuery1 = OAHubURL + "search?rows=100&q="
                    + "footprint:\"Intersects(POLYGON((" + parameters.get("roi").toString() + ")))\""
                    + (parameters.get("producttype").equals("''") ? "" : " AND producttype:" + parameters.get("producttype"))
                    + " AND ingestiondate:[" + currentYear
                    + "-01-01T00:00:00.000Z TO " + currentYear
                    + "-12-31T00:00:00.000Z]"
                    + (parameters.get("relativeorbitnumber").equals("''") ? "" : " AND relativeorbitnumber:" + parameters.get("relativeorbitnumber"))
                    + (parameters.get("sensoroperationalmode").equals("''") ? "" : " AND sensoroperationalmode:" + parameters.get("sensoroperationalmode"))
                    + "&format=json";


            URL url = new URL(httpQuery);

            URLConnection urlConnection = url.openConnection();
            urlConnection.setConnectTimeout(20000);
            urlConnection.setReadTimeout(100000);

            if (url.getUserInfo() != null) {
                String basicAuth = "Basic " + new String(Base64.getEncoder().encode(url.getUserInfo().getBytes()));
                urlConnection.setRequestProperty("Authorization", basicAuth);
            }

            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(
                    new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));

            HashMap imageTitles = new HashMap();
            for (Object obj : (JSONArray) ((HashMap) jsonObject.get("feed")).get("entry")) {
                imageTitles.put(((HashMap) obj).get("title").toString(), ((HashMap) obj).get("id").toString());
            }

            Class.forName("org.postgresql.Driver");
            Connection connection = DriverManager
                    .getConnection(
                            "jdbc:postgresql://" + Credentials.DB_HOST + ":5432/" + Credentials.DB,
                            Credentials.DB_USER, Credentials.DB_PASSWORD);

            connection.setAutoCommit(false);

            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT name FROM remote_sensing_data");
            while (resultSet.next()) {
                String name = resultSet.getString("name");
                if (imageTitles.containsKey(name)) {
                    imageTitles.remove(name);
                }
            }

            connection.close();
            String downloaderScriptContent = imageTitles.values().stream().map(value ->
                    "wget --no-http-keep-alive --no-verbose --output-file=/mnt/satimg/Satellites/Sentinel-1A/downloading.log --continue --content-disposition --user=" + Credentials.COPERNICUS_USER + " --password=" + Credentials.COPERNICUS_PASSWORD + " --directory-prefix=/mnt/satimg/Satellites/Sentinel-1A/ \"https://scihub.copernicus.eu/dhus/odata/v1/Products('" + value + "')/\\$value\""
            ).collect(Collectors.joining("\n")).toString();
            downloaderScriptContent = downloaderScriptContent + "\nwait\n";

            String imageFilePaths = imageTitles.keySet().stream().map(key -> "/mnt/satimg/Satellites/Sentinel-1A/" + key + ".zip").collect(Collectors.joining(",")).toString();
            downloaderScriptContent = downloaderScriptContent + "java -Djava.library.path=$JAVA_LIBRARY_PATH -cp $CLASSPATH org.myapp.satellite.radar.manager.DBManager addNewImageToDB " + imageFilePaths + " > /dev/null\n";

            File file = new File("downloader_script");
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(downloaderScriptContent);
            bw.close();

            return;

        } catch (Exception e) {
            e.printStackTrace();

        }
    }
}
