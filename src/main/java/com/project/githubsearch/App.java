package com.project.githubsearch;

import com.github.kevinsawicki.http.HttpRequest;
import org.json.JSONObject;
import org.omg.CORBA.Request;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.stream.Stream;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Github Search Engine
 *
 */
public class App {

    private static final String AUTH_TOKEN = System.getenv("GITHUB_AUTH_TOKEN");
    // parameter for the request
    private static String PARAM_QUERY = "q"; //$NON-NLS-1$
    private static String PARAM_PAGE = "page"; //$NON-NLS-1$
    private static String PARAM_PER_PAGE = "per_page"; //$NON-NLS-1$

    private static String ACCEPT_TEXT_MATCH = "application/vnd.github.v3.text-match+json";

    // links from the response header
    private static String META_REL = "rel"; //$NON-NLS-1$
    private static String META_NEXT = "next"; //$NON-NLS-1$
    private static final String DELIM_LINKS = ","; //$NON-NLS-1$
    private static final String DELIM_LINK_PARAM = ";"; //$NON-NLS-1$

    private static final int BAD_CREDENTIAL = 401;
    private static final int RESPONSE_OK = 200;
    private static final int ABUSE_RATE_LIMITS = 403;

    public static void main(String[] args) throws InterruptedException, IOException {
        Instant start = Instant.now();
        
        // String basePath = "src/main/java/com/project/githubsearch/files/";

        // String path =
        // "src/main/java/com/project/githubsearch/data/response3000items.json";

        // try (Stream<String> lines = Files.lines(Paths.get(path))) {
        // // UNIX \n, WIndows \r\n
        // String content = lines.collect(Collectors.joining(System.lineSeparator()));

        // // parse json array
        // JSONArray items = new JSONArray(content);
        // System.out.println("items.length()");
        // System.out.println(items.length());

        // for (int it = 0; it < items.length(); it++) {
        // if (it <= 100) {
        // JSONObject item = new JSONObject(items.get(it).toString());
        // String html_url = item.getString("html_url");
        // String download_url = convertHTMLUrlToDownloadUrl(html_url);
        // String fileName = item.getString("name").replace(".java", ".txt");
        // System.out.println("");
        // System.out.println(it);
        // System.out.println(html_url);

        // // try {
        // // // download file from url
        // // URL url;
        // // url = new URL(download_url);
        // // ReadableByteChannel readableByteChannel =
        // Channels.newChannel(url.openStream());
        // // String pathFile = new String(basePath + fileName);
        // // FileOutputStream fileOutputStream = new FileOutputStream(pathFile);
        // // FileChannel fileChannel = fileOutputStream.getChannel();
        // // fileOutputStream.getChannel().transferFrom(readableByteChannel, 0,
        // Long.MAX_VALUE);
        // // } catch (MalformedURLException e) {
        // // // TODO Auto-generated catch block
        // // e.printStackTrace();
        // // }

        // }
        // }

        // } catch (IOException e) {
        // e.printStackTrace();
        // }

        getData();

        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();

        long minutes = (timeElapsed / 1000) / 60;
        long seconds = (timeElapsed / 1000) % 60;

        System.out.println("Elapsed time: " + minutes + " minutes " + seconds + " seconds");
    }

    /**
     * Convert github html url to download url input:
     * https://github.com/shuchen007/ThinkInJavaMaven/blob/85b402a81fc0b3f2f039b34c23be416322ef14b4/src/main/java/sikaiClient/IScyxKtService.java
     * output:
     * https://raw.githubusercontent.com/shuchen007/ThinkInJavaMaven/85b402a81fc0b3f2f039b34c23be416322ef14b4/src/main/java/sikaiClient/IScyxKtService.java
     */
    private static String convertHTMLUrlToDownloadUrl(String html_url) {
        String[] parts = html_url.split("/");
        String download_url = "https://raw.githubusercontent.com/";
        int l = parts.length;
        for (int i = 0; i < l; i++) {
            if (i >= 3) {
                if (i != 5) {
                    if (i != l - 1) {
                        download_url = download_url.concat(parts[i] + '/');
                    } else {
                        download_url = download_url.concat(parts[i]);
                    }
                }
            }
        }

        return download_url;
    }

    private static void getData() throws InterruptedException, IOException {
        String endpoint = "https://api.github.com/search/code";
        // String query = "java.lang.String.replaceAll";
        String query = "createConfigFile";
        // String query = "stringUrl";

        ArrayList<String> sizes = new ArrayList<String>();
        // int MAX_SIZE = 10000;
        int MAX_SIZE = 384000; // the max searchable size from github api
        final int INITIAL_TOTAL_COUNT = 1001;
        final int TOTAL_COUNT_LIMIT = 1000;
        int INITIAL_INTERVAL = 2048;
        String size = "";
        int lower_bound = 0;
        int total_count = 0;
        int dynamic_interval, upper_bound;

        JSONArray result = new JSONArray();
        int total_items = 0;

        int per_page_limit = 100;
        int page = 1;

        System.out.println("Getting data from Github");
        System.out.println("with per page limit: " + per_page_limit);
        showTotalItemsInAQuery(endpoint, query, page, per_page_limit);

        dynamic_interval = INITIAL_INTERVAL;
        while (lower_bound < MAX_SIZE) {
            upper_bound = lower_bound + dynamic_interval;
            total_count = getTotalCount(endpoint, query, lower_bound, upper_bound, page, per_page_limit);
    
            if (total_count < TOTAL_COUNT_LIMIT) { // create the dynamic range higher 
                while (total_count < 200 && upper_bound < MAX_SIZE) {
                    dynamic_interval = dynamic_interval * 2;
                    upper_bound = lower_bound + dynamic_interval;
                    int prev_total_count = total_count;
                    total_count = getTotalCount(endpoint, query, lower_bound, upper_bound, page, per_page_limit);
                    if (total_count > TOTAL_COUNT_LIMIT) {
                        total_count = prev_total_count;
                        dynamic_interval = (int) dynamic_interval / 2;
                        upper_bound = lower_bound + dynamic_interval;
                    }
                }
            } else { // create the dynamic interval smaller
                do {
                    dynamic_interval = (int) dynamic_interval / 2;
                    upper_bound = lower_bound + dynamic_interval;
                    total_count = getTotalCount(endpoint, query, lower_bound, upper_bound, page, per_page_limit);
                } while (total_count > TOTAL_COUNT_LIMIT);
            }

            System.out.println("");
            System.out.println("Lower bound: " + lower_bound);
            System.out.println("Upper bound: " + upper_bound);
            System.out.println("Number items in this request: " + total_count);
            
            lower_bound = upper_bound;
            total_items += total_count;
        }

        System.out.println("");
        // System.out.println("Total items for all requests: " + result.length());
        System.out.println("Total items for all requests: " + total_items);

        // // Get the file reference
        // Path path = Paths.get("src/main/java/com/project/githubsearch/data/response.json");

        // // Use try-with-resource to get auto-closeable writer instance
        // try (BufferedWriter writer = Files.newBufferedWriter(path)) {
        //     writer.write(result.toString());
        // }
    }

    private static int getTotalCount(String endpoint, String query, int lower_bound, int upper_bound, int page, int per_page_limit) {
        int total_count = -1;
        upper_bound++;
        lower_bound--;
        String size = lower_bound + ".." + upper_bound;
        do {
            String url = endpoint + "?" + PARAM_QUERY + "=" + query + "+in:file+language:java+extension:java+size:"
                    + size + "&" + PARAM_PAGE + "=" + page + "&" + PARAM_PER_PAGE + "=" + per_page_limit;
            

            HttpRequest request = HttpRequest.get(url, false).authorization("token " + AUTH_TOKEN);

            // handle response
            int responseCode = request.code();
            if (responseCode == RESPONSE_OK) {
                // System.out.println("Response Headers: " + request.headers());
                String responseBody = request.body();
                JSONObject response = new JSONObject(responseBody);
                total_count = response.getInt("total_count");
            } else if (responseCode == BAD_CREDENTIAL) {
                System.out.println("Authorization problem");
                System.out.println("Please read the readme file!");
                System.out.println("Github message: " + (new JSONObject(request.body())).getString("message"));
                System.exit(-1);
            } else if (responseCode == ABUSE_RATE_LIMITS) {
                System.out.println("Abuse Rate Limits");
                // retry current progress after wait for some seconds
                String retryAfter = request.header("Retry-After");
                try {
                    int sleepTime = 0; // wait for a while
                    if (retryAfter.isEmpty()) {
                        sleepTime = 1;
                    } else {
                        sleepTime = new Integer(retryAfter).intValue();
                    }
                    System.out.println("Retry-After: " + sleepTime);
                    TimeUnit.SECONDS.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Response Code: " + responseCode);
                System.out.println("Response Body: " + request.body());
                System.out.println("Response Headers: " + request.headers());
                System.exit(-1);
            }

        } while (total_count == -1);

        return total_count;
    }

    private static void showTotalItemsInAQuery(String endpoint, String query, int page, int per_page_limit) {
        int total_count = -1;
        String requestText = "";
        do {
            String url = endpoint + "?" + PARAM_QUERY + "=" + query + "+in:file+language:java+extension:java"
                     + "&" + PARAM_PAGE + "=" + page + "&" + PARAM_PER_PAGE + "=" + per_page_limit;

            HttpRequest request = HttpRequest.get(url, false).authorization("token " + AUTH_TOKEN);
            requestText = request.toString();

            // handle response
            int responseCode = request.code();
            if (responseCode == RESPONSE_OK) {
                // System.out.println("Response Headers: " + request.headers());
                String responseBody = request.body();
                JSONObject response = new JSONObject(responseBody);
                total_count = response.getInt("total_count");
            } else if (responseCode == BAD_CREDENTIAL) {
                System.out.println("Authorization problem");
                System.out.println("Please read the readme file!");
                System.out.println("Github message: " + (new JSONObject(request.body())).getString("message"));
                System.exit(-1);
            } else if (responseCode == ABUSE_RATE_LIMITS) {
                System.out.println("Abuse Rate Limits");
                // retry current progress after wait for some seconds
                String retryAfter = request.header("Retry-After");
                try {
                    int sleepTime = 0; // wait for a while
                    if (retryAfter.isEmpty()) {
                        sleepTime = 1;
                    } else {
                        sleepTime = new Integer(retryAfter).intValue();
                    }
                    System.out.println("Retry-After: " + sleepTime);
                    TimeUnit.SECONDS.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Response Code: " + responseCode);
                System.out.println("Response Body: " + request.body());
                System.out.println("Response Headers: " + request.headers());
                System.exit(-1);
            }

        } while (total_count == -1);

        System.out.println("");
        System.out.println("MAIN QUERY");
        System.out.println("Request: " + requestText);
        System.out.println("Total items from github: " + total_count);
    }

    private static String getNextLinkFromResponse(String linkHeader) {

        String next = null;

        if (linkHeader != null) {
            String[] links = linkHeader.split(DELIM_LINKS);
            for (String link : links) {
                String[] segments = link.split(DELIM_LINK_PARAM);
                if (segments.length < 2)
                    continue;

                String linkPart = segments[0].trim();
                if (!linkPart.startsWith("<") || !linkPart.endsWith(">")) //$NON-NLS-1$ //$NON-NLS-2$
                    continue;
                linkPart = linkPart.substring(1, linkPart.length() - 1);

                for (int i = 1; i < segments.length; i++) {
                    String[] rel = segments[i].trim().split("="); //$NON-NLS-1$
                    if (rel.length < 2 || !META_REL.equals(rel[0]))
                        continue;

                    String relValue = rel[1];
                    if (relValue.startsWith("\"") && relValue.endsWith("\"")) //$NON-NLS-1$ //$NON-NLS-2$
                        relValue = relValue.substring(1, relValue.length() - 1);

                    if (META_NEXT.equals(relValue))
                        next = linkPart;
                }
            }
        }
        return next;
    }

    private static int getRandomNumberInRange(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }

}
