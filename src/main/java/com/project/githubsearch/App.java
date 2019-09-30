package com.project.githubsearch;

import com.github.kevinsawicki.http.HttpRequest;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 * Github Search Engine
 *
 */
public class App {
    public static void main(String[] args) {

        String OAUTH_TOKEN = "48b6674f0e050c99bc6418947b6a8950b6893c9b";
        int BAD_CREDENTIAL = 401;
        int RESPONSE_OK = 200;

        String url = "https://api.github.com/search/code";
        String query = "java.lang.String";

        HttpRequest request = HttpRequest.get(url, true, 'q', query, "in", "file", "language", "java")
                .authorization("token " + OAUTH_TOKEN);
        int responseCode = request.code();
        String responseBody = request.body();
        JSONObject response = new JSONObject(responseBody);

        if (responseCode == RESPONSE_OK) {

            JSONArray items = response.getJSONArray("items");
            if (items.length() > 0) {
                System.out.println("Total result: " + response.getInt("total_count"));
                JSONObject item = items.getJSONObject(0); // get the first item
                System.out.println("Print First Item ");
                System.out.println("Repository: " + item.getJSONObject("repository").getString("full_name"));
                System.out.println("Filename: " + item.getString("name"));
                System.out.println("File url: " + item.getString("html_url"));
            } else {
                System.out.println("There is no result for the query");
            }
            
        } else if (responseCode == BAD_CREDENTIAL) {
            System.out.println(response.getString("message"));
            System.out.println("Authorization problem");
            System.out.println("Please read the readme file!");
        }

    }

}
