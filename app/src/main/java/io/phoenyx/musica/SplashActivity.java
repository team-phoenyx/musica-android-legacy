package io.phoenyx.musica;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

/**
 * Created by terra on 1/14/2017.
 */

public class SplashActivity extends AppCompatActivity {
    MusicaAPI API = new MusicaAPI();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (API.checkConnection()) {
            Intent joinRoomIntent = new Intent(getApplicationContext(), JoinActivity.class);
            startActivity(joinRoomIntent);
            finish();
        } else {
            Snackbar.make(findViewById(android.R.id.content), "No Connection", Snackbar.LENGTH_LONG).setAction("Retry", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent restartIntent = getIntent();
                    finish();
                    startActivity(restartIntent);
                }
            }).show();
        }
    }
}
