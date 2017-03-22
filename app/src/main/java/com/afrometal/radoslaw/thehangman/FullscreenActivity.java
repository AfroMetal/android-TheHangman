package com.afrometal.radoslaw.thehangman;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 2000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;

    /**
     * Saved State data names
     */
    private static final String POINTS_TOTAL = "pt";
    private static final String LIVES_LEFT = "ll";
    private static final String INFO = "inf";
    private static final String WORD = "wrd";
    private static final String RIDDLE = "rdl";
    private static final String BUTTONS_STATE = "bs";

    /**
     * The Hangmen
     */
    private static int[] HANGMAN_RESOURCE = new int[9];
    //static block to initialize the chalk board image array for guesses.
    static{
        HANGMAN_RESOURCE[0] = R.drawable.hangman_0;
        HANGMAN_RESOURCE[1] = R.drawable.hangman_1;
        HANGMAN_RESOURCE[2] = R.drawable.hangman_2;
        HANGMAN_RESOURCE[3] = R.drawable.hangman_3;
        HANGMAN_RESOURCE[4] = R.drawable.hangman_4;
        HANGMAN_RESOURCE[5] = R.drawable.hangman_5;
        HANGMAN_RESOURCE[6] = R.drawable.hangman_6;
        HANGMAN_RESOURCE[7] = R.drawable.hangman_7;
        HANGMAN_RESOURCE[8] = R.drawable.hangman_8;
    }

    /**
     * Letters for every line to make keyboard creating easier.
     */
    private static final char[] line1Letters = "QWERTYUIOP".toCharArray();
    private static final char[] line2Letters = "ASDFGHJKL".toCharArray();
    private static final char[] line3Letters = "ZXCVBNM".toCharArray();

    // List of words
    private static String[] words;

    private char[] word;
    private char[] riddle;

    private int pointsTotal = 0;
    private int livesLeft = 9;

    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private ImageView mHangman;
    private TextView mRiddle;
    private TextView mPoints;
    private TextView mLivesLeft;
    private TextView mInfo;
    private LinearLayout mLine1;
    private LinearLayout mLine2;
    private LinearLayout mLine3;

    private LinkedList<Button> buttons;
    private int[] buttonsState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);

        mHangman = (ImageView) findViewById(R.id.hangman);
        mRiddle = (TextView) findViewById(R.id.riddle);
        mPoints = (TextView) findViewById(R.id.points);
        mLivesLeft = (TextView) findViewById(R.id.guesses_left);
        mInfo = (TextView) findViewById(R.id.info);
        mLine1 = (LinearLayout) findViewById(R.id.line_1);
        mLine2 = (LinearLayout) findViewById(R.id.line_2);
        mLine3 = (LinearLayout) findViewById(R.id.line_3);

        words = getResources().getStringArray(R.array.words);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.nextGame).setOnTouchListener(mDelayHideTouchListener);
        findViewById(R.id.nextPuzzle).setOnTouchListener(mDelayHideTouchListener);

        prepareKeyboard();

        if (savedInstanceState != null) {
            // Set content back to saved state
            pointsTotal = savedInstanceState.getInt(POINTS_TOTAL);
            mPoints.setText(Integer.toString(pointsTotal));

            livesLeft = savedInstanceState.getInt(LIVES_LEFT);
            mLivesLeft.setText(Integer.toString(livesLeft));

            if (livesLeft == 0) {
                for (Button b : buttons) {
                    b.setEnabled(false);
                }
            }
            if (livesLeft != 9) {
                mHangman.setImageResource(HANGMAN_RESOURCE[livesLeft]);
            } else {
                mHangman.setImageResource(0);
            }

            mInfo.setText(savedInstanceState.getString(INFO));

            word = savedInstanceState.getString(WORD).toCharArray();

            riddle = savedInstanceState.getString(RIDDLE).toCharArray();
            mRiddle.setText(new String(riddle));

            buttonsState = savedInstanceState.getIntArray(BUTTONS_STATE);
            for (int i=0; i<buttons.size(); i++) {
                Button b = buttons.get(i);
                if (buttonsState[i] != 0) {
                    if (buttonsState[i] == 1) {
                        b.setTextColor(getResources().getColor(R.color.correctButton, null));
                    } else if (buttonsState[i] == -1) {
                        b.setTextColor(getResources().getColor(R.color.wrongButton, null));
                    }
                    b.setEnabled(false);
                    b.setBackgroundColor(Color.TRANSPARENT);
                }
            }
            hide();
        } else {
            // Initialize new game
            mPoints.setText(Integer.toString(pointsTotal));
            nextPuzzle();
            delayedHide(500);
        }
    }

    private void prepareKeyboard() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            width = height;
        }

        Map<LinearLayout, char[]> lineLetters = new HashMap<>();
        lineLetters.put(mLine1, line1Letters);
        lineLetters.put(mLine2, line2Letters);
        lineLetters.put(mLine3, line3Letters);

        buttons = new LinkedList<>();
        buttonsState = new int[line1Letters.length + line2Letters.length + line3Letters.length];

        for (LinearLayout ll : lineLetters.keySet()) {
            ll.removeAllViewsInLayout();
            int buttonWidth = width / 11;
            for (char c : lineLetters.get(ll)) {
                Button b = new Button(this);

                b.setOnClickListener(generateOnClickListener());

                b.setText(Character.toString(c));
                b.setTag(c);
                b.setGravity(Gravity.CENTER);
                b.setPadding(0,0,0,0);
//                b.setBackgroundColor(Color.TRANSPARENT);
                b.setMinimumWidth(buttonWidth-5);
                b.setWidth(buttonWidth);
                ll.addView(b);
                buttons.add(b);
                buttonsState[buttons.indexOf(b)] = 0;
            }
        }
    }

    private View.OnClickListener generateOnClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                char c = (char) b.getTag();

                int index = new String(word).indexOf(c);
                Boolean correct = index != -1;

                if (correct) {
                    b.setTextColor(getResources().getColor(R.color.correctButton, null));
                    buttonsState[buttons.indexOf(b)] = 1;
                    correctGuess(c);
                } else {
                    b.setTextColor(getResources().getColor(R.color.wrongButton, null));
                    buttonsState[buttons.indexOf(b)] = -1;
                    wrongGuess();
                }
                b.setEnabled(false);
                b.setBackgroundColor(Color.TRANSPARENT);
            }
        };
    }

    private void correctGuess(char c) {
        Log.d("correct char: ", Character.toString(c));
        Log.d("correct char: ", new String(word));

        for (int i=0; i<word.length; i++) {
            if (word[i] == c) {
                riddle[i] = c;
                changePoints(1);
            }
        }
        mRiddle.setText(new String(riddle));

        if (new String(word).equals(new String(riddle))) {
            endRound(true);
        }
    }

    private void wrongGuess() {
        changePoints(-1);

        mLivesLeft.setText(Integer.toString(--livesLeft));
        mHangman.setImageResource(HANGMAN_RESOURCE[livesLeft]);

        if (livesLeft <= 0) {
            endRound(false);
        }
    }

    private void endRound(boolean win) {
        for (Button btn : buttons) {
            btn.setEnabled(false);
        }
        mInfo.setText(win ? "YOU'VE WON!" : "YOU'VE LOST! The word was " + new String(word));


        int points = 2 * word.length;
        changePoints(win ? points : -points);


        Toast.makeText(this, win ? "Congratulations!" : "Try again.", Toast.LENGTH_SHORT).show();

        show();
    }

    private void changePoints(int points) {
        pointsTotal += points;
        mPoints.setText(Integer.toString(pointsTotal));
//        Toast.makeText(this, Integer.toString(points), Toast.LENGTH_SHORT).show();
    }

    public void nextPuzzle(View view) {
        nextPuzzle();
    }


    public void newGame(View view) {
        pointsTotal = 0;
        mPoints.setText(Integer.toString(0));
        nextPuzzle();
        Toast.makeText(this, "New game started", Toast.LENGTH_SHORT).show();
    }

    private void nextPuzzle() {
        livesLeft = 9;
        mLivesLeft.setText(Integer.toString(livesLeft));

        mInfo.setText(R.string.spaceFiller);

        mHangman.setImageResource(0);

        Random r = new Random();
        word = words[r.nextInt(words.length)].toUpperCase().toCharArray();

        Log.d("word: ", new String(word));

        riddle = new char[word.length];
        for (int i=0; i<riddle.length; i++) {
            riddle[i] = '_';
        }
        mRiddle.setText(new String(riddle));

        prepareKeyboard();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(POINTS_TOTAL, pointsTotal);
        outState.putInt(LIVES_LEFT, livesLeft);
        outState.putString(INFO, new String(mInfo.getText().toString()));
        outState.putString(WORD, new String(word));
        outState.putString(RIDDLE, new String(riddle));
        outState.putIntArray(BUTTONS_STATE, buttonsState);
    }

    // FULLSCREEN METHODS -------------------------------------------------------------------------

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}
