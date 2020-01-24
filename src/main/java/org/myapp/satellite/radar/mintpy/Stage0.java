package org.myapp.satellite.radar.mintpy;

import org.esa.s1tbx.insar.gpf.InSARStackOverview;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Stage0 {

    static Pattern datePattern = Pattern.compile("(\\d\\d\\d\\d\\d\\d\\d\\d)");

    public static void main(String[] args) {

        HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
        String workingDir = consoleParameters.get("workingDir").toString();
        String fileList = consoleParameters.get("filesList").toString();
        String bPerpCrit = consoleParameters.get("bPerpCrit").toString();
        String bTempCrit = consoleParameters.get("bTempCrit").toString();

        try {
            Files.walk(Paths.get(workingDir + File.separator + "network"))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (Exception e) {
            System.out.println(e);
        }

        String pairIDStr = "", pairNameStr = "", pairDateStr = "";
        TreeSet<String> productNames = new TreeSet<>();
        String masterProductName, slaveProductName, masterProductDate, slaveProductDate;

        InSARStackOverview.IfgPair[] masterSlavePairs;
        InSARStackOverview.IfgPair masterSlavePair;
        InSARStackOverview.IfgStack[] stackOverview;

        Product[] products = Arrays.stream(fileList.split(",")).map(file -> {
            try {
                return ProductIO.readProduct(file);
            } catch (Exception e) {
                return null;
            }
        }).toArray(Product[]::new);

        try {

            stackOverview = InSARStackOverview.calculateInSAROverview(products);

            int counter = 0;

            for (int i = 0; i < stackOverview.length; i++) {
                masterSlavePairs = stackOverview[i].getMasterSlave();
                for (int j = i; j < masterSlavePairs.length; j++) {
                    masterSlavePair = masterSlavePairs[j];

                    double bPerp = masterSlavePair.getPerpendicularBaseline();
                    double bTemp = masterSlavePair.getTemporalBaseline();
                    if (masterSlavePair.getMasterMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString().equals(
                            masterSlavePair.getSlaveMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString())) {
                        continue;
                    }

                    if (Math.abs(bTemp) <= Integer.valueOf(bTempCrit) && Math.abs(bPerp) <= Integer.valueOf(bPerpCrit)) {

                        Matcher dateMatcher = datePattern.matcher(masterSlavePair.getMasterMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString());
                        dateMatcher.find();
                        masterProductDate = dateMatcher.group();

                        dateMatcher = datePattern.matcher(masterSlavePair.getSlaveMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString());
                        dateMatcher.find();
                        slaveProductDate = dateMatcher.group();

                        pairDateStr = pairDateStr + masterProductDate + "," + slaveProductDate + "," + bPerp + "," + bTemp + ";";

                        masterProductName = masterSlavePair.getMasterMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString();
                        slaveProductName = masterSlavePair.getSlaveMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString();
                        productNames.add(masterProductName);
                        productNames.add(slaveProductName);

                        pairNameStr = pairNameStr + masterProductName + "," + slaveProductName + ";";

                        counter++;

                    }
                }
            }

            System.out.println("Total pairs: " + counter);

            pairNameStr = pairNameStr.substring(0, pairNameStr.length() - 1);
            pairIDStr = pairNameStr;
            String[] productNamesStr = productNames.stream().toArray(String[]::new);

            for (int i = 0; i < productNamesStr.length; i++) {
                pairIDStr = pairIDStr.replace(productNamesStr[i], String.valueOf(i));
            }

            new File(workingDir + File.separator + "network").mkdirs();

            // Write pairs to file
            PrintWriter out = new PrintWriter(workingDir + File.separator + "network" + File.separator + "pairNames.txt");
            out.println(pairNameStr);
            out.close();

            out = new PrintWriter(workingDir + File.separator + "network" + File.separator + "pairDate.txt");
            out.println(pairDateStr);
            out.close();

            out = new PrintWriter(workingDir + File.separator + "network" + File.separator + "pairID.txt");
            out.println(pairIDStr);
            out.close();

        } catch (Exception e) {
            System.out.println(e);
        }
    }
}

class Graph {
    private int V;   // No. of vertices

    // Array  of lists for Adjacency List Representation
    private LinkedList<Integer> adj[];

    // Constructor
    Graph(int v) {
        V = v;
        adj = new LinkedList[v];
        for (int i = 0; i < v; ++i)
            adj[i] = new LinkedList();
    }

    //Function to add an edge into the graph
    void addEdge(int v, int w) {
        adj[v].add(w);  // Add w to v's list.
    }

    // A function used by DFS
    void DFSUtil(int v, boolean visited[]) {
        // Mark the current node as visited and print it
        visited[v] = true;
        System.out.print(v + " ");

        // Recur for all the vertices adjacent to this vertex
        Iterator<Integer> i = adj[v].listIterator();
        while (i.hasNext()) {
            int n = i.next();
            if (!visited[n])
                DFSUtil(n, visited);
        }
    }

    // The function to do DFS traversal. It uses recursive DFSUtil()
    void DFS(int v) {
        // Mark all the vertices as not visited(set as
        // false by default in java)
        boolean visited[] = new boolean[V];

        // Call the recursive helper function to print DFS traversal
        DFSUtil(v, visited);
    }
}
