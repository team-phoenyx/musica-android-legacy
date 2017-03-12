package io.phoenyx.musica;

import android.net.Uri;
import android.os.AsyncTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

/**
 * Created by terra on 1/14/2017.
 */

public class MusicaAPI extends AsyncTask<String, Void, String> {

    /**
     * Create Room
     *
     * @param room_name Name of room
     * @return join_code(0), room_id(1)
     */

    public String[] createRoom(String room_name, String location) {
        String rawCall = "create_room/" + room_name + "/" + location;
        String json = doInBackground(rawCall);
        JSONObject jsonObject = parseDoubleLayerJSON(json)[0];
        String joinCode = "";
        String roomID = "";
        try {
            joinCode = jsonObject.getString("join_code");
            roomID = jsonObject.getString("room_id");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String[] returnArray = new String[]{joinCode, roomID};
        return returnArray;
    }

    /**
     * Destroy Room
     *
     * @param join_code
     * @param room_id
     * @return void
     */

    public void destroyRoom(String join_code, String room_id) {
        String rawCall = "destroy_room/" + join_code + "/" + room_id;
        doInBackground(rawCall);
    }


    /**
     * Join Room
     *
     * @param join_code
     * @param lng
     * @param lat
     * @return boolean
     */


    public int joinRoom(String join_code, String lng, String lat) {
        if (checkLocationProximity(getRoomLocation(join_code), lng, lat)) {
            String rawCall = "join_room/" + join_code;
            String json = doInBackground(rawCall);
            JSONObject result = parseDoubleLayerJSON(json)[0];
            int resultCode = 0;
            try {
                resultCode = Integer.parseInt(result.getString("result"));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (resultCode == 0) {
                return 0;
            } else {
                return 2;
            }
        } else {
            return 1;
        }


    }

    private boolean checkLocationProximity(String roomLocation, String lng, String lat) {
        double roomLng = Double.parseDouble(roomLocation.split(":")[0]);
        double roomLat = Double.parseDouble(roomLocation.split(":")[1]);

        double userLng = Double.parseDouble(lng);
        double userLat = Double.parseDouble(lat);

        double dLng = Math.abs(roomLng - userLng);
        double dLat = Math.abs(roomLat - userLat);

        double distance = Math.sqrt(dLat * dLat + dLng * dLng);

        //TODO find out the units of the coords and set appropriate distance cutoff

        if (distance < 1) {
            return true;
        }
        return false;
    }

    private void setNewMetadata() {
        //TODO create API endpoint
    }


    /**
     * Get Location
     *
     * @param join_code Join code
     * @return coordinates
     */

    public String getRoomLocation(String join_code) {
        String rawCall = "get_location/" + join_code;
        String json = doInBackground(rawCall);
        JSONObject jsonObject = parseDoubleLayerJSON(json)[0];
        String locationCoords = "";
        try {
            locationCoords = jsonObject.getString("location");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return locationCoords;
    }


    /**
     * Get Choices
     *
     * @param join_code
     * @return [vote_choices] (indexes 1-4 are song titles, 5-8 are picture strings), 0 if voting hasn't started, or the choice that won
     */

    public String[] getChoices(String join_code) {
        String rawCall = "get_choices/" + join_code;
        String json = doInBackground(rawCall);
        JSONObject results = parseDoubleLayerJSON(json)[0];
        String status = "0";
        try {
            status = results.getString("status");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (status.equals("0")) {
            ArrayList<String> choicesInfoAL = new ArrayList<>();
            try {
                choicesInfoAL.add(results.getString("title_1"));
                choicesInfoAL.add(results.getString("title_2"));
                choicesInfoAL.add(results.getString("title_3"));
                choicesInfoAL.add(results.getString("title_4"));
                choicesInfoAL.add(results.getString("image_1"));
                choicesInfoAL.add(results.getString("image_2"));
                choicesInfoAL.add(results.getString("image_3"));
                choicesInfoAL.add(results.getString("image_4"));
                choicesInfoAL.add(results.getString("artist_1"));
                choicesInfoAL.add(results.getString("artist_2"));
                choicesInfoAL.add(results.getString("artist_3"));
                choicesInfoAL.add(results.getString("artist_4"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String[] returnArray = new String[12];
            choicesInfoAL.toArray(returnArray);
            return returnArray;
        } else if (status.equals("1")) {
            return new String[]{"1"};
        } else {
            String[] returnArray = new String[]{status};
            return returnArray;
        }
    }


    /**
     * Get Vote Status
     *
     * @param join_code
     * @param room_id
     * @return [numbers] of votes for every choice
     */

    public int[] getVoteStatus(String join_code, String room_id) {
        String rawCall = "get_vote_status" + join_code + "/" + room_id;
        String json = doInBackground(rawCall);
        JSONObject jsonObject = parseDoubleLayerJSON(json)[0];
        int votes1 = 0;
        int votes2 = 0;
        int votes3 = 0;
        int votes4 = 0;

        try {
            votes1 = Integer.parseInt(jsonObject.getString("1"));
            votes2 = Integer.parseInt(jsonObject.getString("2"));
            votes3 = Integer.parseInt(jsonObject.getString("3"));
            votes4 = Integer.parseInt(jsonObject.getString("4"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return new int[]{votes1, votes2, votes3, votes4};
    }


    /**
     * Vote
     *
     * @param join_code
     * @param choice_number
     * @return 0 for successful, 1 for unsuccessful
     */

    public String vote(String join_code, int choice_number) {
        String rawCall = "vote/" + join_code + "/" + choice_number;
        String json = doInBackground(rawCall);
        JSONObject jsonObject = parseDoubleLayerJSON(json)[0];
        String voteStatus = "";
        try {
            voteStatus = jsonObject.getString("vote_status");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return voteStatus;
    }


    /**
     * Close voting round
     *
     * @param join_code
     * @param room_id
     * @return void
     */

    public void closeVotingRound(String join_code, String room_id) {
        String rawCall = "close_current_round/" + join_code + "/" + room_id;
        doInBackground(rawCall);
    }

    /**
     * Start Voting Round
     *
     * @param join_code
     * @param room_id
     * @param song_choices
     * @param images
     * @param artists
     */
    public void startVotingRound(String join_code, String room_id,
                                 String[] song_choices,
                                 String[] images,
                                 String[] artists) {
        StringBuilder rawCallStringBuilder = new StringBuilder();
        rawCallStringBuilder.append("start_next_round/" + join_code + "/" + room_id + "/");
        for (int i = 0; i < song_choices.length; i++) {
            rawCallStringBuilder.append(song_choices[i] + "/");
        }
        for (int i = 0; i < images.length; i++) {
            rawCallStringBuilder.append(images[i] + "/");
        }
        for (int i = 0; i < artists.length; i++) {
            rawCallStringBuilder.append(artists[i] + "/");
        }
        rawCallStringBuilder.deleteCharAt(rawCallStringBuilder.length() - 1);
        doInBackground(rawCallStringBuilder.toString());
    }


    public int responseCode;
    boolean isConnected;

    public boolean checkConnection() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("http://musica.cloudapp.net/Service1.svc/r/");
                    URLConnection connection = url.openConnection();
                    connection.setConnectTimeout(5000);
                    connection.connect();
                    isConnected = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    isConnected = false;
                }
            }
        });

        thread.start();

        try {
            thread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return isConnected;
    }

    @Override
    protected String doInBackground(final String... params) {
        final StringBuilder returnedString = new StringBuilder();
        Thread network = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String urlStr;
                    String encodedQuery = Uri.encode(params[0]);
                    urlStr = "http://musica.cloudapp.net/Service1.svc/r/" + encodedQuery;
                    HttpClient httpClient = new DefaultHttpClient();
                    HttpGet httpGet = new HttpGet(urlStr);
                    HttpResponse response = httpClient.execute(httpGet);

                    responseCode = response.getStatusLine().getStatusCode();
                    if (responseCode == 200) {
                        HttpEntity entity = response.getEntity();
                        if (entity != null) {
                            InputStream inputStream = entity.getContent();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                            String line;
                            while ((line = reader.readLine()) != null) {
                                returnedString.append(line + "\n");
                            }
                            inputStream.close();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        network.start();

        try {
            network.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return returnedString.toString();
    }

    public JSONObject[] parseDoubleLayerJSON(String jsonString) {
        ArrayList<JSONObject> resultList = new ArrayList<>();

        try {
            //Parse json
            JSONObject jsonToParse = new JSONObject(jsonString);
            for (int i = 0; i < jsonToParse.length(); i++) {
                JSONObject innerJSON = new JSONObject(jsonToParse.getString(Integer.toString(i)));
                resultList.add(i, innerJSON);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject[] returnArray = new JSONObject[resultList.size()];
        resultList.toArray(returnArray);
        return returnArray;
    }
}
