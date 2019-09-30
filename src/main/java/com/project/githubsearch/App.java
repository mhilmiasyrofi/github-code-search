package com.project.githubsearch;

import com.github.kevinsawicki.http.HttpRequest;
import org.json.JSONObject;
import org.json.JSONArray;


/**
 * Github Search Engine
 *
 */
public class App {
    public static void main( String[] args ) {
        
        String OAUTH_TOKEN = "6cdddfecd5ab4929a844b37741e28951d6be61c7";

        String url = "https://api.github.com/search/code";
        String query = "String.replace";
        String response = HttpRequest.get(url, true, 'q', query, "in", "file", "language", "js" )
                                    .authorization("token " + OAUTH_TOKEN)
                                    .body();

        JSONObject obj = new JSONObject(response);

        JSONArray items = obj.getJSONArray("items");

        if (items.length() > 0) {
            System.out.println("Total result: " + obj.getInt("total_count"));
            JSONObject item = items.getJSONObject(0); // get the first item
            System.out.println("Print First Item ");
            System.out.println("Repository: " + item.getJSONObject("repository").getString("full_name"));
            System.out.println("Filename: " + item.getString("name"));
            System.out.println("File url: " + item.getString("html_url"));
        } else {
            System.out.println("There is no result for the query");
        }


    }

}
