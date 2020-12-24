package com.dogonfire.myhorse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class UpdateChecker {
    private static final String API_NAME_VALUE = "name";
    private static final String API_LINK_VALUE = "downloadUrl";
    private static final String API_RELEASE_TYPE_VALUE = "releaseType";
    private static final String API_FILE_NAME_VALUE = "fileName";
    private static final String API_GAME_VERSION_VALUE = "gameVersion";
    private static final String API_QUERY = "/servermods/files?projectIds=";
    private static final String API_HOST = "https://api.curseforge.com";
    private static String versionName = "NOVERSION";
    private static String versionLink;
    private static String versionType;
    private static String versionFileName;
    private static String versionGameVersion;
    private final int projectID = 60440;
    private final String apiKey = null;

    public String getLatestVersionName() {
        URL url = null;

        String versionName = null;
        try {
            url = new URL("https://api.curseforge.com/servermods/files?projectIds=60440");
        } catch (MalformedURLException e) {
            e.printStackTrace();

            return versionName;
        }
        try {
            URLConnection conn = url.openConnection();
            if (this.apiKey != null) {
                conn.addRequestProperty("X-API-Key", this.apiKey);
            }
            conn.addRequestProperty("User-Agent", "MyHorse (by DoggyOnFire)");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String response = reader.readLine();

            JSONArray array = (JSONArray) JSONValue.parse(response);
            if (array.size() > 0) {
                JSONObject latest = (JSONObject) array.get(array.size() - 1);

                versionName = (String) latest.get("name");

                versionLink = (String) latest.get("downloadUrl");

                versionType = (String) latest.get("releaseType");

                versionFileName = (String) latest.get("fileName");

                versionGameVersion = (String) latest.get("gameVersion");
            } else {
                System.out.println("There are no files for this project");
            }
        } catch (IOException e) {
            return versionName;
        }
        return versionName;
    }

    public String getLatestVersionGameVersion() {
        return versionGameVersion;
    }

    public String getLatestVersionLink() {
        return "http://dev.bukkit.org/bukkit-plugins/myhorse/";
    }
}
