package com.project.githubsearch;

import com.github.kevinsawicki.http.HttpRequest;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.stream.Stream;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Github Search Engine
 *
 */
public class App {

    private static final String AUTH_TOKEN = "5a2446f46bc95d4b915ff8e14bc0efac1827072e";
    // parameter for the request
    private static String PARAM_QUERY = "q"; //$NON-NLS-1$
    private static String PARAM_PAGE = "page"; //$NON-NLS-1$
    private static String PARAM_PER_PAGE = "per_page"; //$NON-NLS-1$
    
    private static String ACCEPT_TEXT_MATCH = "application/vnd.github.v3.text-match+json";
    
    // some links from the response header
    private static String META_REL = "rel"; //$NON-NLS-1$
    private static String META_LAST = "last"; //$NON-NLS-1$
    private static String META_NEXT = "next"; //$NON-NLS-1$
    private static String META_FIRST = "first"; //$NON-NLS-1$
    private static String META_PREV = "prev"; //$NON-NLS-1$
    
    private static final String DELIM_LINKS = ","; //$NON-NLS-1$
    private static final String DELIM_LINK_PARAM = ";"; //$NON-NLS-1$
        
    private static final int BAD_CREDENTIAL = 401;
    private static final int RESPONSE_OK = 200;
    private static final int ABUSE_RATE_LIMITS = 403;


    public static void main(String[] args) throws InterruptedException, IOException {
        // String basePath = "src/main/java/com/project/githubsearch/files/";
        
        // String path = "src/main/java/com/project/githubsearch/data/response3000items.json";
        
        // try (Stream<String> lines = Files.lines(Paths.get(path))) {
        //     // UNIX \n, WIndows \r\n
        //     String content = lines.collect(Collectors.joining(System.lineSeparator()));
            
        //     // parse json array
        //     JSONArray items = new JSONArray(content);
        //     System.out.println("items.length()");
        //     System.out.println(items.length());

        //     for (int it = 0; it < items.length(); it++) {
        //         if (it <= 100) {
        //             JSONObject item = new JSONObject(items.get(it).toString());
        //             String html_url = item.getString("html_url");
        //             String download_url = convertHTMLUrlToDownloadUrl(html_url);
        //             String fileName = item.getString("name").replace(".java", ".txt");
        //             System.out.println("");
        //             System.out.println(it);
        //             System.out.println(html_url);
                    
        //             // try {
        //             //     // download file from url
        //             //     URL url;
        //             //     url = new URL(download_url);
        //             //     ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
        //             //     String pathFile = new String(basePath + fileName);
        //             //     FileOutputStream fileOutputStream = new FileOutputStream(pathFile);
        //             //     FileChannel fileChannel = fileOutputStream.getChannel();
        //             //     fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        //             // } catch (MalformedURLException e) {
        //             //     // TODO Auto-generated catch block
        //             //     e.printStackTrace();
        //             // }

        //         }
        //     }

        // } catch (IOException e) {
        //     e.printStackTrace();
        // }

        getData();

    }


    /**
     * Convert github html url to download url
     * input: https://github.com/shuchen007/ThinkInJavaMaven/blob/85b402a81fc0b3f2f039b34c23be416322ef14b4/src/main/java/sikaiClient/IScyxKtService.java
     * output: https://raw.githubusercontent.com/shuchen007/ThinkInJavaMaven/85b402a81fc0b3f2f039b34c23be416322ef14b4/src/main/java/sikaiClient/IScyxKtService.java
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
        String query = "java.lang.String.replaceAll";
        // String[] paths = { "app/src/", "src/", "lib/" };
        String[] sizes = { "10000..50000", "<10000", ">50000" };
        

        JSONArray result = new JSONArray();

        int per_page_limit = 100;
        int page = 1;
        int last_page = 2;

        System.out.println("Getting data from Github");
        System.out.println("with per page limit: " + per_page_limit);
        int id = 1;

        for (int i = 0; i < sizes.length; i++) { 
            String size = sizes[i];
            for (int j = 1; j <= last_page; j++) { // the page start at 1
                page = j;
                System.out.println("");
                System.out.println("Iteration number: " + id++);
                System.out.println("Size: " + size);
                System.out.println("Page: " + page);
                
                HttpRequest request = HttpRequest.get(endpoint, false
                                                        , PARAM_QUERY, query + "+in:file+language:java+extension:java+size:" + size
                                                        , PARAM_PAGE, page
                                                        , PARAM_PER_PAGE, per_page_limit)
                                                .authorization("token " + AUTH_TOKEN)
                                                .accept(ACCEPT_TEXT_MATCH);
                System.out.println("Request: " + request);
                
                // handle response
                int responseCode = request.code();
                if (responseCode == RESPONSE_OK) {
                    // System.out.println("Response Headers: " + request.headers());
                    System.out.println("Link: " + request.header("Link"));
                    System.out.println("Next: " + getNextLinkFromResponse(request.header("Link")));
                    String responseBody = request.body();
                    JSONObject response = new JSONObject(responseBody);
                    JSONArray item = response.getJSONArray("items");
                    System.out.println("Total items in current request: " + item.length());
                    for (int it = 0; it < item.length(); it++) {
                        result.put(item.get(it));
                    }
                } else if (responseCode == BAD_CREDENTIAL) {
                    System.out.println("Authorization problem");
                    System.out.println("Please read the readme file!");
                    System.out.println("Github message: " + (new JSONObject(request.body())).getString("message"));
                    System.exit(-1);
                } else if (responseCode == ABUSE_RATE_LIMITS) {
                    System.out.println("Abuse Rate Limits");
                    System.out.println("Retry-After: " + request.header("Retry-After"));
                    try {
                        int sleepTime = 60; // wait for 60 seconds
                        TimeUnit.SECONDS.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    j--; // retry current progress
                } else {
                    System.out.println("Response Code: " + responseCode);
                    System.out.println("Response Body: " + request.body());
                    System.out.println("Response Headers: " + request.headers());
                }
            }
        }

        System.out.println("Total items for all requests: " + result.length());

        // Get the file reference
        Path path = Paths.get("src/main/java/com/project/githubsearch/data/response.json");
        
        // Use try-with-resource to get auto-closeable writer instance
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(result.toString());
        }
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
                        next  = linkPart;
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
