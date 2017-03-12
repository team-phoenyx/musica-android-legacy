package io.phoenyx.musica;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class VoteActivity extends AppCompatActivity {

    MusicaAPI API = new MusicaAPI();
    String[] voteChoices;
    String joinCode;

    Button songChoice1Button,
            songChoice2Button,
            songChoice3Button,
            songChoice4Button;

    ImageButton songImage1Button,
            songImage2Button,
            songImage3Button,
            songImage4Button;

    TextView voteStatusTextView;

    LinearLayout choicesLayout, imageButtonsLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vote);

        Bundle extras = getIntent().getExtras();
        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/HKSuperRound-Bold.otf");
        joinCode = extras.getString("join_code");

        voteStatusTextView = (TextView) findViewById(R.id.voteStatusLabel);

        songChoice1Button = (Button) findViewById(R.id.songChoice1Button);
        songChoice2Button = (Button) findViewById(R.id.songChoice2Button);
        songChoice3Button = (Button) findViewById(R.id.songChoice3Button);
        songChoice4Button = (Button) findViewById(R.id.songChoice4Button);
        songImage1Button = (ImageButton) findViewById(R.id.songImage1Button);
        songImage2Button = (ImageButton) findViewById(R.id.songImage2Button);
        songImage3Button = (ImageButton) findViewById(R.id.songImage3Button);
        songImage4Button = (ImageButton) findViewById(R.id.songImage4Button);

        songChoice1Button.setTypeface(typeface);
        songChoice2Button.setTypeface(typeface);
        songChoice3Button.setTypeface(typeface);
        songChoice4Button.setTypeface(typeface);

        choicesLayout = (LinearLayout) findViewById(R.id.voteChoicesLayout);
        imageButtonsLayout = (LinearLayout) findViewById(R.id.voteButtonsLayout);



        //TODO make song title scroll across button if too long

        TextView nowPlayingTextView = (TextView) findViewById(R.id.nowPlayingTextView);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    voteChoices = API.getChoices(joinCode);

                    if (voteChoices[0].equals("1")) {
                        imageButtonsLayout.setVisibility(View.GONE);
                        choicesLayout.setVisibility(View.GONE);
                        voteStatusTextView.setVisibility(View.VISIBLE);
                        resetColors();
                    } else {
                        final Bitmap[] bitmaps = new Bitmap[4];

                        for (int i = 8; i < 12; i++) {
                            bitmaps[i - 8] = getBitmap(voteChoices[i]);
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                songChoice1Button.setText(voteChoices[0]);
                                songChoice2Button.setText(voteChoices[1]);
                                songChoice3Button.setText(voteChoices[2]);
                                songChoice4Button.setText(voteChoices[3]);

                                songImage1Button.setImageBitmap(bitmaps[0]);
                                songImage2Button.setImageBitmap(bitmaps[1]);
                                songImage3Button.setImageBitmap(bitmaps[2]);
                                songImage4Button.setImageBitmap(bitmaps[3]);

                                enableAll();

                                voteStatusTextView.setVisibility(View.GONE);
                                choicesLayout.setVisibility(View.VISIBLE);
                                imageButtonsLayout.setVisibility(View.VISIBLE);
                            }
                        });

                    }
                    try {
                        wait(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }


            }
        }).start();
    }

    private Bitmap getBitmap(String imageString) {
        byte[] byteArray = Base64.decode(imageString, Base64.DEFAULT);
        Bitmap image = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        return image;
    }

    private void setButtonClickListener(final Button button, final int choice) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        API.vote(joinCode, choice + 1);
                    }
                }).start();

                disableAll();

                button.setTextColor(Color.WHITE);

                switch (choice) {
                    case 0:
                        songImage1Button.setBackgroundColor(getResources().getColor(R.color.colorTextHint));
                        break;
                    case 1:
                        songImage2Button.setBackgroundColor(getResources().getColor(R.color.colorTextHint));
                        break;
                    case 2:
                        songImage3Button.setBackgroundColor(getResources().getColor(R.color.colorTextHint));
                        break;
                    case 3:
                        songImage4Button.setBackgroundColor(getResources().getColor(R.color.colorTextHint));
                        break;
                }

            }
        });
    }

    private void setImageClickListener(final ImageButton button, final int choice) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        API.vote(joinCode, choice + 1);
                    }
                }).start();

                disableAll();
                button.setBackgroundColor(getResources().getColor(R.color.colorTextHint));

                switch (choice) {
                    case 0:
                        songChoice1Button.setTextColor(Color.WHITE);
                        break;
                    case 1:
                        songChoice2Button.setTextColor(Color.WHITE);
                        break;
                    case 2:
                        songChoice3Button.setTextColor(Color.WHITE);
                        break;
                    case 3:
                        songChoice4Button.setTextColor(Color.WHITE);
                        break;
                }

            }
        });
    }

    private void resetColors() {
        songImage1Button.setBackgroundColor(getResources().getColor(R.color.colorTextHint));
        songImage2Button.setBackgroundColor(getResources().getColor(R.color.colorTextHint));
        songImage3Button.setBackgroundColor(getResources().getColor(R.color.colorTextHint));
        songImage4Button.setBackgroundColor(getResources().getColor(R.color.colorTextHint));
        songChoice1Button.setTextColor(Color.WHITE);
        songChoice2Button.setTextColor(Color.WHITE);
        songChoice3Button.setTextColor(Color.WHITE);
        songChoice4Button.setTextColor(Color.WHITE);
    }

    private void enableAll() {
        songChoice1Button.setEnabled(true);
        songChoice2Button.setEnabled(true);
        songChoice3Button.setEnabled(true);
        songChoice4Button.setEnabled(true);

        songImage1Button.setEnabled(true);
        songImage2Button.setEnabled(true);
        songImage3Button.setEnabled(true);
        songImage4Button.setEnabled(true);
    }

    private void disableAll() {
        songChoice1Button.setEnabled(false);
        songChoice2Button.setEnabled(false);
        songChoice3Button.setEnabled(false);
        songChoice4Button.setEnabled(false);

        songImage1Button.setEnabled(false);
        songImage2Button.setEnabled(false);
        songImage3Button.setEnabled(false);
        songImage4Button.setEnabled(false);
    }
}
