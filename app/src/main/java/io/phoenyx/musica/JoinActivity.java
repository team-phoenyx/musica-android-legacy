package io.phoenyx.musica;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import java.util.concurrent.TimeUnit;

public class JoinActivity extends AppCompatActivity {

    AlertDialog.Builder createRoomBuilder;

    MusicaAPI API = new MusicaAPI();

    Location currentLocation;
    LocationManager locationManager;
    LocationListener locationListener;

    TextView musicaLabel;
    EditText joinCodeEditText;
    Button hostButton;

    boolean isLocationAllowed = false;

    private static final String TAG = JoinActivity.class.getSimpleName();

    @SuppressWarnings("SpellCheckingInspection")
    private static final String CLIENT_ID = "4d67539ec17a4f7eb35169023e4c9290";
    @SuppressWarnings("SpellCheckingInspection")
    private static final String REDIRECT_URI = "phoenyx-musica://callback";

    private static final int REQUEST_CODE = 1337;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);

        String token = CredentialsHandler.getToken(this);
        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/HKSuperRound-Bold.otf");

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                currentLocation = location;
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        musicaLabel = (TextView) findViewById(R.id.musicalLabel);
        joinCodeEditText = (EditText) findViewById(R.id.joinCodeEditText);
        hostButton = (Button) findViewById(R.id.hostButton);

        musicaLabel.setTypeface(typeface);
        joinCodeEditText.setTypeface(typeface);
        hostButton.setTypeface(typeface);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(JoinActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    6969);
        } else {
            isLocationAllowed = true;
        }

        joinCodeEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if ((i == EditorInfo.IME_ACTION_DONE) || ((keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (keyEvent.getAction() == KeyEvent.ACTION_DOWN))) {
                    String joinCode = joinCodeEditText.getText().toString();
                    joinCode = joinCode.toUpperCase();

                    joinRoom(joinCode);

                    return true;
                } else {
                    return false;
                }
            }

        });

        final Thread createDialogThread = new Thread(new Runnable() {
            @Override
            public void run() {
                createRoomBuilder = new AlertDialog.Builder(JoinActivity.this);
                LayoutInflater layoutInflater = getLayoutInflater();
                View createRoomDialogView = layoutInflater.inflate(R.layout.create_room_dialog, null);
                createRoomBuilder.setTitle("Create Room").setView(createRoomDialogView).setCustomTitle(layoutInflater.inflate(R.layout.create_room_dialog_title, null));

                final EditText roomNameEditText = (EditText) createRoomDialogView.findViewById(R.id.roomNameEditText);
                final ImageButton spotifyButton = (ImageButton) createRoomDialogView.findViewById(R.id.spotifyButton);
                final ImageButton soundcloudButton = (ImageButton) createRoomDialogView.findViewById(R.id.soundcloudButton);
                final TextView errorTextView = (TextView) createRoomDialogView.findViewById(R.id.errorTextView);


                spotifyButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String roomName = roomNameEditText.getText().toString();

                        if (roomName.equals("") || roomName.isEmpty()) {
                            errorTextView.setVisibility(View.VISIBLE);
                        } else {
                            errorTextView.setVisibility(View.GONE);

                            double lng = 0, lat = 0;

                            if (currentLocation == null) {
                                Snackbar.make(findViewById(android.R.id.content), "Enable location permissions!", Snackbar.LENGTH_LONG).setAction("Configure", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        ActivityCompat.requestPermissions(JoinActivity.this,
                                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                                6969);
                                    }
                                }).show();
                            } else {
                                lng = currentLocation.getLongitude();
                                lat = currentLocation.getLatitude();

                                SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("room_name", roomName);
                                editor.putString("location", Double.toString(lng) + ":" + Double.toString(lat));
                                editor.apply();

                                onLoginButtonClicked();
                            }
                        }
                    }
                });
            }
        });

        createDialogThread.start();

        try {
            createDialogThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        final AlertDialog createRoomDialog = createRoomBuilder.create();

        hostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActivityCompat.checkSelfPermission(JoinActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                        ActivityCompat.checkSelfPermission(JoinActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(JoinActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                            6969);
                } else {
                    currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }

                fetchLocation();
                createRoomDialog.show();
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 6969: {
                // If request is cancelled, the result arrays are empty.
                isLocationAllowed = (grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED);
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void joinRoom(final String joinCode) {
        double lng = 0, lat = 0;

        try {
            lng = currentLocation.getLongitude();
            lat = currentLocation.getLatitude();
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        int joinResult = API.joinRoom(joinCode, Double.toString(lng), Double.toString(lat));

        if (joinResult == 0) {
            Intent voteIntent = new Intent(getApplicationContext(), VoteActivity.class);
            voteIntent.putExtra("join_code", joinCode);
            startActivity(voteIntent);
            finish();
        } else if (joinResult == 1) {
            Snackbar.make(findViewById(android.R.id.content), "Too far from room", Snackbar.LENGTH_LONG).show();
        } else {
            Snackbar.make(findViewById(android.R.id.content), "Error joining room", Snackbar.LENGTH_LONG).setAction("Retry", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    joinRoom(joinCode);
                }
            }).show();
        }
    }

    private void fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(JoinActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    6969);
        } else {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }

    }

    public void onLoginButtonClicked() {
        final AuthenticationRequest request = new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI)
                .setScopes(new String[]{"playlist-read-private", "playlist-read-collaborative", "user-read-private", "streaming"})
                .build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    logMessage("Got token: " + response.getAccessToken());
                    CredentialsHandler.setToken(this, response.getAccessToken(), response.getExpiresIn(), TimeUnit.SECONDS);
                    startMainActivity(response.getAccessToken());
                    break;

                // Auth flow returned an error
                case ERROR:
                    logError("Auth error: " + response.getError());
                    break;

                // Most likely auth flow was cancelled
                default:
                    logError("Auth result: " + response.getType());
            }
        }
    }

    private void startMainActivity(String token) {
        Intent intent = new Intent(getApplicationContext(), HostActivity.class);
        intent.putExtra("EXTRA_TOKEN", token);
        startActivity(intent);
        finish();
    }

    private void logError(String msg) {
        //TODO replace these toasts w/ presentable Snackbars
        Toast.makeText(this, "Error: " + msg, Toast.LENGTH_SHORT).show();
        Log.e(TAG, msg);
    }

    private void logMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        Log.d(TAG, msg);
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            locationManager.removeUpdates(locationListener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
}
