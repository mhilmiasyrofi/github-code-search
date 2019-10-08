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
    public static void main(String[] args) throws InterruptedException, IOException {
        // String basePath = "src/main/java/com/project/githubsearch/files/";
        
        // String path = "src/main/java/com/project/githubsearch/data/response1000items.txt";
        
        // try (Stream<String> lines = Files.lines(Paths.get(path))) {
        //     // UNIX \n, WIndows \r\n
        //     String content = lines.collect(Collectors.joining(System.lineSeparator()));
        //     // System.out.println(content);
        //     JSONObject data = new JSONObject(content);
        //     System.out.println(data.getInt("total_count"));
        //     JSONArray items = data.getJSONArray("items");
        //     System.out.println("items.length()");
        //     System.out.println(items.length());
        //     for (int it = 0; it < items.length(); it++) {
        //         if (it == 0) {
        //             JSONObject item = new JSONObject(items.get(it).toString());
        //             String html_url = item.getString("html_url");
        //             String download_url = convertHTMLUrlToDownloadUrl(html_url);
        //             String fileName = item.getString("name").replace(".java", ".txt");
                    
        //             try {
        //                 // download file from url
        //                 URL url;
        //                 url = new URL(download_url);
        //                 ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
        //                 String pathFile = new String(basePath + fileName);
        //                 FileOutputStream fileOutputStream = new FileOutputStream(pathFile);
        //                 FileChannel fileChannel = fileOutputStream.getChannel();
        //                 fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        //             } catch (MalformedURLException e) {
        //                 // TODO Auto-generated catch block
        //                 e.printStackTrace();
        //             }

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
        String OAUTH_TOKEN = "39bcdebc4dc9353757acf13dac3708b5460a5e80";
        int BAD_CREDENTIAL = 401;
        int RESPONSE_OK = 200;
        int ABUSE_RATE_LIMITS = 403;

        String endpoint = "https://api.github.com/search/code";
        String query = "java.lang.String#ReplaceAll";
        // String[] paths = { "app/src/", "src/", "lib/" };
        String[] sizes = { "10000:50000", "<10000", ">50000" };
        

        JSONArray result = new JSONArray();

        int per_page_limit = 100;
        int page = 0;
        int last_page = 10;

        System.out.println("Getting data from Github");
        System.out.println("with per page limit: " + per_page_limit);
        int id = 1;

        for (int i = 0; i < sizes.length; i++) {
            String size = sizes[i];
            for (int j = 0; j < last_page; j++) {
                System.out.println("");
                System.out.println("Iteration number: " + id++);
                page = j;
                String url = endpoint + "?"
                                        + "&page=" + page
                                        + "&per_page=" + per_page_limit
                                        + "&q=" + query + "+in:file+language:java+extension:java+size:" + size;
                                        
                System.out.println("Url Query: " + url);
                System.out.println("Size: " + size);
                System.out.println("Page: " + page);
                
                HttpRequest request = HttpRequest.get(url).authorization("token " + OAUTH_TOKEN)
                                .accept("application/vnd.github.v3.text-match+json");
                
                // handle response
                int responseCode = request.code();
                if (responseCode == RESPONSE_OK) {
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
                    System.out.println("Response Headers: " + request.headers());
                }
            }
        }

        System.out.println("Total items for all requests: " + result.length());

        // Get the file reference
        Path path = Paths.get("src/main/java/com/project/githubsearch/data/response.txt");
        
        // Use try-with-resource to get auto-closeable writer instance
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(result.toString());
        }
    }

    private static int getRandomNumberInRange(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }

}
