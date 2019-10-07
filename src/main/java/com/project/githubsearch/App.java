package com.project.githubsearch;

import com.github.kevinsawicki.http.HttpRequest;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.stream.Stream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.util.stream.Collectors;

/**
 * Github Search Engine
 *
 */
 public class App {
    public static void main(String[] args) {
        String path = "src/main/java/com/project/githubsearch/data/response1000items.txt";
        String basePath = "src/main/java/com/project/githubsearch/files/"; 
        try (Stream<String> lines = Files.lines(Paths.get(path))) {

            // UNIX \n, WIndows \r\n
            String content = lines.collect(Collectors.joining(System.lineSeparator()));
            // System.out.println(content);
            JSONObject data = new JSONObject(content);
            System.out.println(data.getInt("total_count"));
            JSONArray items = data.getJSONArray("items");
            for (int it = 0; it < items.length(); it++) {
                if (it == 1){
                    JSONObject item = new JSONObject(items.get(it).toString());
                    // System.out.println(items.get(it)); 
                    String html_url = item.getString("html_url");
                    System.out.println("html_url");
                    System.out.println(html_url);
                    String download_url = convertHTMLUrlToDownloadUrl(html_url);
                    System.out.println(download_url);

                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

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
        // System.out.println("download_url");
        // System.out.println(download_url);
        return download_url;

    }

    public void getData() {
        String OAUTH_TOKEN = "39bcdebc4dc9353757acf13dac3708b5460a5e80";
        int BAD_CREDENTIAL = 401;
        int RESPONSE_OK = 200;

        String endpoint = "https://api.github.com/search/code";
        String query = "java.lang.String";
        String[] paths = { "app/src/", "src/", "lib/" };

        int per_page_limit = 100;
        int page = 0;

        String url = endpoint + "?q=" + query + "+in:file+language:java+extension:java+path:src/&page=" + page
                + "&per_page=" + per_page_limit;

        HttpRequest request = HttpRequest.get(url).authorization("token " + OAUTH_TOKEN)
                .accept("application/vnd.github.v3.text-match+json");

        int responseCode = request.code();
        String responseBody = request.body();
        JSONObject queryResult = new JSONObject(responseBody);

        for (int i = 0; i < 10; i++) {
            page = i;
            for (int j = 0; j < paths.length; j++) {
                String path = paths[j];
                url = endpoint + "?q=" + query + "+in:file+language:java+extension:java+path:" + path + "&page=" + page
                        + "&per_page=" + per_page_limit;

                request = HttpRequest.get(url).authorization("token " + OAUTH_TOKEN)
                        .accept("application/vnd.github.v3.text-match+json");

                responseCode = request.code();
                responseBody = request.body();
                JSONObject response = new JSONObject(responseBody);

                if (responseCode == RESPONSE_OK) {
                    JSONArray item = response.getJSONArray("items");
                    JSONArray items = queryResult.getJSONArray("items");
                    for (int it = 0; it < item.length(); it++) {
                        items.put(item.get(it));
                    }
                } else if (responseCode == BAD_CREDENTIAL) {
                    System.out.println(response.getString("message"));
                    System.out.println("Authorization problem");
                    System.out.println("Please read the readme file!");
                    System.exit(-1);
                }
            }
        }

        System.out.println(queryResult.toString());
    }

}
