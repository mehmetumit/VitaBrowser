package com.dymos.vitabrowser;

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

//android:theme="@style/Theme.VitaBrowser"
public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private ImageView mousePointer;
    private  CountDownTimer pointerVisibilityTimer;
    Timer pointerMoveTimer;
    private  int velocityX = 0,velocityY = 0,keyCode;
    private  int screenWidth,screenHeight;
    private final int RECORD_REQUEST_CODE= 101,pointerAcceleration = 1;
    private Browser browser;
    private WebClient webClient;
    private int x = 0, y = 0,row,column;
    private SearchManager searchManager;
    private EditText searchBar ;
    private RelativeLayout frame,dialogBack;
    private boolean firstDown = true;
    private ImageButton voiceButton,forwardButton,backButton,refreshButton;
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    private View[][] panelViews ;
    private View focusTemp;
    private String homePage = "https://www.google.com";
    //private static final String desktop_mode = "Mozilla/5.0 (X11; Linux x86_64)AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.136 Safari/537.36 Puffin/9.0.0.50278AV";
    //private static final String mobile_mode = "Mozilla/5.0 (Linux; U; Android 4.4; en-us; Nexus 4 Build/JOP24G) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 MobileSafari/534.30";
    private final int UP = 0,DOWN = 1,LEFT = 2,RIGHT = 3;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();

        dialogBack = findViewById(R.id.dialog_back);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault());
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float rmsdB) {

            }

            @Override
            public void onBufferReceived(byte[] buffer) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int error) {
               if(error == 7){
                   searchBar.setHint(webView.getUrl());
                   voiceButton.setBackground(ContextCompat.getDrawable(MainActivity.this,R.drawable.voice_button_background_default));

               }
            }

            @Override
            public void onResults(Bundle bundle) {
                ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                voiceButton.setBackground(ContextCompat.getDrawable(MainActivity.this,R.drawable.voice_button_background_default));
                if (matches != null) {
                    doSearch(matches.get(0));
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {

            }

            @Override
            public void onEvent(int eventType, Bundle params) {

            }
        });
        frame = findViewById(R.id.frame);

        voiceButton = findViewById(R.id.voice_button);
        voiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speechRecognizer.startListening(speechRecognizerIntent);
                searchBar.setText("");
                searchBar.setHint("Listening...");
                voiceButton.setBackground(ContextCompat.getDrawable(MainActivity.this,R.drawable.voice_button_background_touched));

            }
        });
        backButton = findViewById(R.id.back_button);
        forwardButton = findViewById(R.id.forward_button);
        refreshButton = findViewById(R.id.refresh_button);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!webView.canGoBack()){
                    finish();
                }else {
                    webView.goBack();
                    hideView(dialogBack);
                }
            }
        });
        forwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.goForward();
                hideView(dialogBack);
            }
        });
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.reload();
                hideView(dialogBack);
            }
        });

        mousePointer = findViewById(R.id.mouse_pointer);
        searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchBar = findViewById(R.id.search_bar);
        //searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        //searchView.setIconifiedByDefault(false);
        searchBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }
        });
        searchBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_SEARCH){
                    doSearch(v.getText().toString());
                    //searchView.onActionViewExpanded();
                    searchBar.clearFocus();
                    InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    in.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    searchBar.setText("");
                }
                return false;
            }
        });
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
            /*Override
            public boolean onQueryTextSubmit(String query) {
                doSearch(query);
                searchView.onActionViewExpanded();
                searchView.clearFocus();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                return false;
            }*/
        });
        webView = findViewById(R.id.web_view);
        webView.setWebViewClient(browser = new Browser(searchBar,webView));
        webView.setWebChromeClient(webClient = new WebClient(this));
        webView.loadUrl(homePage);
        webView.getSettings().setJavaScriptEnabled(true);

        //webView.getSettings().setUserAgentString("Mozilla/5.0 (X11; Linux x86_64)AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.136 Safari/537.36 Puffin/9.0.0.50278AV");

        webView.getSettings().setSupportMultipleWindows(false);
        panelViews = new View[][]{{searchBar, voiceButton}, {backButton,forwardButton,refreshButton}};
        row = 0;
        column = 1;
        panelViews[row][column].setFocusable(true);
        panelViews[row][column].requestFocus();
        panelViews[row][column].setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.voice_button_focus_background));

    }
    private  void hideView(View v){
        v.setVisibility(View.GONE);
    }
    private void checkPermission() {

        if (ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO},RECORD_REQUEST_CODE);
        }
    }
    public void doSearch(String query){
        if(query != null) {
            webView.loadUrl(homePage + "/search?q=" + query);
            hideView(dialogBack);
        }
    }

    /*@Override
    protected void onNewIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            doSearch(query);
        }
        super.onNewIntent(intent);
    }*/
    @Override
    public void onWindowFocusChanged(boolean hasFocus){
        screenWidth = webView.getWidth();
        screenHeight = webView.getHeight();
    }
    public void nextView(int num){
        int rowTemp,columnTemp;
        rowTemp = row;
        columnTemp = column;
        switch (num){
            case UP:
            case DOWN:
                if(row == 0){
                    row = 1;
                    column = 0;
                }else if(row == 1){
                    row = 0;
                    column = 1;
                }
                break;
            case LEFT:
                if(row == 0){
                    if(column == 0){
                        column = 1;
                    }else{
                        column--;
                    }
                }else if(row == 1){
                    if(column == 0){
                        column = 2;
                    }else{
                        column --;
                    }
                }
                break;
            case RIGHT:
                if(row == 0){
                    if(column == 1){
                        column = 0;
                    }else{
                        column ++;
                    }
                }else if(row == 1){
                    if(column == 2){
                        column = 0;
                    }else{
                        column ++;
                    }
                }
                break;
            default:
                break;
        }
        panelViews[rowTemp][columnTemp].clearFocus();
        panelViews[row][column].setFocusable(true);
        panelViews[row][column].requestFocus();
        if(rowTemp == 0 ){
            if(columnTemp == 0) {
                panelViews[rowTemp][columnTemp].setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.search_bar_background));
            }else{
                panelViews[rowTemp][columnTemp].setBackground(ContextCompat.getDrawable(MainActivity.this,R.drawable.voice_button_background_default));
            }
        }else {
            panelViews[rowTemp][columnTemp].setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.button_default_background));
        }
        if(row == 0 ){
            if(column == 0) {
                panelViews[row][column].setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.search_bar_focus_background));
            }else{
                panelViews[row][column].setBackground(ContextCompat.getDrawable(MainActivity.this,R.drawable.voice_button_focus_background));
            }
        }else {
            panelViews[row][column].setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.button_focus_background));
        }
        /*if(row == 0 && column == 0){
            searchView.performClick();
        }*/
}
    public void dialogEvent(int keyCode){
        Log.e("dialogE","dialogEvent");
            //mousePointer.setVisibility(View.GONE);
        switch (keyCode){
            case KeyEvent.KEYCODE_BACK:
                Log.e("current focus",""+getCurrentFocus());
                focusTemp = getCurrentFocus();
                if(focusTemp != null){
                    focusTemp.requestFocus();
                }

                dialogBack.setVisibility(View.GONE);
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                nextView(UP);
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                nextView(DOWN);
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                nextView(LEFT);
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                nextView(RIGHT);
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
                Log.e("current focus",""+getCurrentFocus());
                getCurrentFocus().performClick();
                break;
            default:
                break;
        }
    }
    public void movePointer(){

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
                velocityX = 0;
                velocityY += pointerAcceleration;
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                velocityX = 0;
                velocityY -= pointerAcceleration;
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                velocityX -= pointerAcceleration;
                velocityY = 0;
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                velocityX += pointerAcceleration;
                velocityY = 0;
                break;

            default:
                break;
        }
        x += velocityX;
        y += velocityY;
        if(x + mousePointer.getWidth() / 2 > screenWidth){
            x = screenWidth - mousePointer.getWidth() / 2;
        }else if(x < 0){
            x = 0;
        }
        if(y + mousePointer.getHeight() > screenHeight){
            y = screenHeight - mousePointer.getHeight();
            webView.scrollBy(0, velocityY);
        }else if(y < 0){
            webView.scrollBy(0,velocityY);
            y = 0;
        }
        mousePointer.setX(x);
        mousePointer.setY(y);
    }
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        keyCode = event.getKeyCode();
        if(dialogBack.getVisibility() == View.VISIBLE && event.getAction() != KeyEvent.ACTION_UP){
                dialogEvent(keyCode);
        }else{
            if (event.getAction() == KeyEvent.ACTION_UP){
                if(pointerMoveTimer != null) {
                    pointerMoveTimer.cancel();
                }
                firstDown = true;
                velocityX = 0;
                velocityY = 0;
                pointerVisibilityTimer =  new CountDownTimer(3 * 1000, 1000){
                    @Override
                    public final void onTick(final long millisUntilFinished) {
                    }
                    @Override
                    public final void onFinish() {
                        mousePointer.setVisibility(View.GONE);
                    }
                }.start();
                return true;
            }else if(event.getAction() == KeyEvent.ACTION_DOWN){
                if(pointerVisibilityTimer != null) {
                    pointerVisibilityTimer.cancel();
                }
                if(mousePointer.getVisibility() == View.GONE) {
                    mousePointer.setVisibility(View.VISIBLE);
                }
            }
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    /*if(webClient.isFullScreen()){
                        //super.onBackPressed();
                        if(paused){
                            webView.onResume();
                            paused = false;
                        }else{
                            paused = true;
                            webView.onPause();
                        }
                    }*/
                    final long uMillis = SystemClock.uptimeMillis();
                    frame.dispatchTouchEvent(MotionEvent.obtain(uMillis, uMillis,
                            MotionEvent.ACTION_DOWN, x, y, 0));
                    frame.dispatchTouchEvent(MotionEvent.obtain(uMillis, uMillis,
                            MotionEvent.ACTION_UP, x, y, 0));
                    break;
                case KeyEvent.KEYCODE_BACK:
                    if(webClient.isFullScreen()){
                        webClient.onHideCustomView();
                    }else{
                        dialogBack.setVisibility(View.VISIBLE);
                        if(focusTemp != null) {
                            focusTemp.requestFocus();
                        }
                    }
                    break;
            }
            if(firstDown){
                firstDown = false;
                pointerMoveTimer = new Timer();
                pointerMoveTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        movePointer();
                    }
                },0,1000/60);

            }
        }
        return true;
    }

}