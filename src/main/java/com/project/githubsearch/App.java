package com.project.githubsearch;

import com.github.kevinsawicki.http.HttpRequest;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Github Search Engine
 *
 */
public class App {
    public static void main(String[] args) {

        String OAUTH_TOKEN = "39bcdebc4dc9353757acf13dac3708b5460a5e80";
        int BAD_CREDENTIAL = 401;
        int RESPONSE_OK = 200;

        String endpoint = "https://api.github.com/search/code";
        String query = "java.lang.String";

        int per_page_limit = 100;
        int page = 0;
        
        String url = endpoint + "?q=" + query + "+in:file+language:java+extension:java+path:src/&page=" + page + "&per_page=" + per_page_limit;
        
        HttpRequest request = HttpRequest.get(url).authorization("token " + OAUTH_TOKEN)
        .accept("application/vnd.github.v3.text-match+json");
        
        int responseCode = request.code();
        String responseBody = request.body();
        JSONObject queryResult = new JSONObject(responseBody);
        
        int i = 0;
        for (i = 0; i < 10; i++) {
            page = i;
            url = endpoint + "?q=" + query + "+in:file+language:java+extension:java+path:src/&page=" + page + "&per_page=" + per_page_limit;
    
            request = HttpRequest.get(url)
                    .authorization("token " + OAUTH_TOKEN)
                    .accept("application/vnd.github.v3.text-match+json");
                    
            responseCode = request.code();
            responseBody = request.body();
            JSONObject response = new JSONObject(responseBody);
            
            if (responseCode == RESPONSE_OK) {
                JSONArray item = response.getJSONArray("items");
                JSONArray items = queryResult.getJSONArray("items");
                for (int j = 0; j < item.length(); j++) {
                    items.put(item.get(j));
                }
            } else if (responseCode == BAD_CREDENTIAL) {
                System.out.println(response.getString("message"));
                System.out.println("Authorization problem");
                System.out.println("Please read the readme file!");
            }
        }

        System.out.println(queryResult.toString());

    }

}
