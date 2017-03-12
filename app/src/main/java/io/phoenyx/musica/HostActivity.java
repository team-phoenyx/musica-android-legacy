package io.phoenyx.musica;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.Playlist;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.Track;

public class HostActivity extends AppCompatActivity implements SpotifyPlayer.NotificationCallback, ConnectionStateCallback {

    ListView playlistListView;
    Button startVoteButton;

    private String roomID;
    private String joinCode;
    private boolean isChangingTrack;
    private int selectedPlaylist;
    private int currentVotePlaylist;
    private int[] songChoices;
    private boolean isInitialization;

    private static final String EXTRA_TOKEN = "EXTRA_TOKEN";
    private static final String CLIENT_ID = "4d67539ec17a4f7eb35169023e4c9290";
    private static final String KEY_CURRENT_QUERY = "CURRENT_QUERY";
    private static final String REDIRECT_URI = "phoenyx-musica://callback";
    private static final int REQUEST_CODE = 1337;

    MusicaAPI API = new MusicaAPI();

    SpotifyApi spotifyApi;
    SpotifyService spotifyService;
    Player player;

    Playlist currentPlaylist;
    Pager<PlaylistSimple> playlists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host);
        //searchView = (Search.View) findViewById(R.id.search_view);
        //Intent intent = getIntent();
        //String token = intent.getStringExtra(EXTRA_TOKEN);

        Bundle extras = getIntent().getExtras();
        String token = extras.getString(EXTRA_TOKEN);

        startVoteButton = (Button) findViewById(R.id.startVoteButton);
        playlistListView = (ListView) findViewById(R.id.playlistListView);

        isInitialization = true;
        playlists = new Pager<>();
        spotifyApi = new SpotifyApi();
        spotifyApi.setAccessToken(token);
        spotifyService = spotifyApi.getService();

        Config playerConfig = new Config(this, token, CLIENT_ID);
        Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
            @Override
            public void onInitialized(SpotifyPlayer spotifyPlayer) {
                player = spotifyPlayer;
                player.addConnectionStateCallback(HostActivity.this);
                player.addNotificationCallback(HostActivity.this);
            }

            @Override
            public void onError(Throwable throwable) {
                Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
            }
        });

        //*********************************CREATES ROOM********************************
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        final String roomName = sharedPreferences.getString("room_name", "ROOM_NAME");
        final String location = sharedPreferences.getString("location", "LOCATION");

        new Thread(new Runnable() {
            @Override
            public void run() {
                String[] roomInfo = API.createRoom(roomName, location);
                joinCode = roomInfo[0];
                roomID = roomInfo[1];
            }
        }).start();

        //*****************************SETS ONCLICKLISTENERS******************************
        startVoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startVotingRound(selectedPlaylist);
            }
        });

        playlistListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                selectedPlaylist = i;
                if (isInitialization) {
                    startVotingRound(i);
                }
            }
        });

        getSupportActionBar().setTitle("Room Code: " + joinCode);
    }

    @Override
    protected void onStart() {
        super.onStart();

        try {
            Thread getPlaylistThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    playlists = spotifyService.getMyPlaylists();
                }
            });

            getPlaylistThread.start();

            getPlaylistThread.join();

        } catch (Exception e) {
            e.printStackTrace();
        }
        List<PlaylistSimple> playlistLists = playlists.items;

        ArrayList<String> playlistTitles = new ArrayList<>();

        for (int i = 0; i < playlistLists.size(); i++) {
            playlistTitles.add(i, playlistLists.get(i).name);
        }

        ArrayAdapter<String> playlistsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, playlistTitles);

        playlistListView.setAdapter(playlistsAdapter);

    }

    private void startVotingRound(int selectedPlaylist) {
        if (isInitialization) {
            playSong(selectedPlaylist, 0);
        }
        currentVotePlaylist = selectedPlaylist;
        final String playlistID = playlists.items.get(currentVotePlaylist).id;
        final String userID = playlists.items.get(currentVotePlaylist).owner.id;

        Thread getPlayListThread = new Thread(new Runnable() {
            @Override
            public void run() {
                currentPlaylist = spotifyService.getPlaylist(userID, playlistID);
            }
        });

        getPlayListThread.start();

        try {
            getPlayListThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Pager<PlaylistTrack> pagerPLT = currentPlaylist.tracks;
        int playlistSize = pagerPLT.items.size();
        int[] songAssignments;
        if (playlistSize > 4) {
            songAssignments = getRandomSet(playlistSize);
            String[] songTitles = new String[4];
            String[] artists = new String[4];
            String[] images = new String[]{"", "", "", ""};

            songChoices = songAssignments;
            for (int i = 0; i < songAssignments.length; i++) {
                songTitles[i] = pagerPLT.items.get(i).track.name;
                artists[i] = pagerPLT.items.get(i).track.album.name;
            }

            API.startVotingRound(joinCode, roomID, songTitles, images, artists);
        } else {
            Snackbar.make(findViewById(android.R.id.content), "Playlist too small!", Snackbar.LENGTH_LONG).show();
        }
    }

    private int[] getRandomSet(int playlistSize) {
        Random random = new Random();
        int[] randomInts = new int[4];

        for (int i = 0; i < 4; i++) {
            int testInt = random.nextInt(playlistSize);
            if (i == 0) {
                randomInts[i] = testInt;
            } else if (!arrayContains(randomInts, testInt)) {
                randomInts[i] = testInt;
            } else {
                i--;
            }
        }
        return randomInts;
    }

    private boolean arrayContains(int[] array, int index) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == index) {
                return true;
            }
        }
        return false;
    }

    private void playSong(int playlistNumber, int songNumber) {
        String playlistID = playlists.items.get(playlistNumber).id;
        String userID = playlists.items.get(playlistNumber).owner.id;
        Playlist currentPlaylist = spotifyService.getPlaylist(userID, playlistID);
        Pager<PlaylistTrack> pagerPLT = currentPlaylist.tracks;
        PlaylistTrack currentPlaylistTrack = pagerPLT.items.get(songNumber);
        Track currentTrack = currentPlaylistTrack.track;

        isChangingTrack = true;
        player.playUri(null, currentTrack.uri, 0, 0);
        isChangingTrack = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }

    @Override
    public void onLoggedIn() {

    }

    @Override
    public void onLoggedOut() {

    }

    @Override
    public void onLoginFailed(int i) {

    }

    @Override
    public void onTemporaryError() {

    }

    @Override
    public void onConnectionMessage(String s) {

    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        if (playerEvent.equals(PlayerEvent.kSpPlaybackNotifyTrackChanged) && isChangingTrack == false) {
            isChangingTrack = true;
            player.pause(null);
            int[] voteCounts = API.getVoteStatus(joinCode, roomID);
            List<Integer> winningSongs = getWinningSongs(voteCounts);
            if (winningSongs.size() == 1) {
                playSong(currentVotePlaylist, songChoices[winningSongs.get(0)]);
            } else if (winningSongs.size() > 1) {
                playSong(currentVotePlaylist, songChoices[getRandom(winningSongs)]);
            }
            isChangingTrack = false;
            startVotingRound(selectedPlaylist);
        }
    }

    private int getRandom(List<Integer> availableChoices) {
        Random random = new Random();
        return availableChoices.get(random.nextInt(availableChoices.size()));
    }

    private List<Integer> getWinningSongs(int[] voteCounts) {
        List<Integer> winningSongs = new ArrayList<>();
        int winningVotes = 0;

        for (int i = 0; i < 4; i++) {
            if (voteCounts[i] > winningVotes) {
                winningSongs.clear();
                winningSongs.add(i);
                winningVotes = voteCounts[i];
            } else if (voteCounts[i] == winningVotes) {
                winningSongs.add(i);
            }
        }

        return winningSongs;
    }

    @Override
    public void onPlaybackError(Error error) {

    }
}