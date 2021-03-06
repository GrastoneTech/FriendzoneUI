package tech.grastone.friendzoneui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.transition.Slide;
import androidx.transition.TransitionManager;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import tech.grastone.friendzoneui.util.JSInterface;
import tech.grastone.friendzoneui.util.MessageBean;
import tech.grastone.friendzoneui.util.RequestBody;

public class HomeActivity extends AppCompatActivity {
    private final int NEXT_VALUE_MIN = 3;
    private final int NEXT_VALUE_MAX = 7;

    String serverHost = "";


    private FrameLayout loadingFrame, videoFrame, ownFaceFrame;
    private WebView videoWV;
    String serverName = "";

    private LottieAnimationView backToStartLAV;

    private OkHttpClient okHttpClient;
    public WebSocket webSocket;
    public String uuid = null;
    private Gson gson;
    private MessageBean currBean = null;
    private SwipeListener swipeListener;
    InterstitialAd mInterstitialAd;
    //    private TimerTextHelper timerTextHelper = null;
    private AdView mAdView;
    private InterstitialAd interstitial;
    private int nextValue = getRandomInRange(NEXT_VALUE_MIN, NEXT_VALUE_MAX);
    private int counter = 0;
    String serverBase = "";
    String serverPort = "";
    String serverProtocol = "";
    String wsserverProtocol = "";
    private int status = 0;
    private TextView loadingMsgTW, onlineUserTw;

    public static int getRandomInRange(int start, int end) {
        //return start + new Random().nextInt(end - start + 1);
        return ThreadLocalRandom.current().nextInt(start, end);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        createView();
        super.onCreate(savedInstanceState);
    }

    private void createView() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        gson = new Gson();
        SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE);
        uuid = sharedPreferences.getString("UUID", "");
        serverHost = sharedPreferences.getString("serverHost", "");
        serverName = sharedPreferences.getString("serverName", "");
        serverBase = sharedPreferences.getString("serverBase", "");
        serverPort = sharedPreferences.getString("serverPort", "");
        serverProtocol = sharedPreferences.getString("serverProtocol", "");
        wsserverProtocol = sharedPreferences.getString("wsserverProtocol", "");

        setContentView(R.layout.activity_home);
        loadingFrame = findViewById(R.id.loadingFrame);
        videoFrame = findViewById(R.id.videoFrame);
        ownFaceFrame = findViewById(R.id.ownFaceFrame);
        backToStartLAV = findViewById(R.id.backToStartLAV);
        videoWV = findViewById(R.id.videoWV);
        loadingMsgTW = findViewById(R.id.loadingMsgTW);
        onlineUserTw = findViewById(R.id.onlineUserTW);
        loadingFrame.setVisibility(View.VISIBLE);
        videoFrame.setVisibility(View.GONE);
        ownFaceFrame.setVisibility(View.GONE);
        webSocket = null;

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        mAdView = findViewById(R.id.homeBannerAd);
        //  mAdView.setAdUnitId("ca-app-pub-8438566450366927/8505669966");
        AdRequest adRequest2 = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest2);
        // loadInterstitialAd();


        runOnUiThread(() -> {
            new Handler().postDelayed(() -> init(), 5000);
        });

        backToStart();
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, "ca-app-pub-8438566450366927/5632094610", adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                mInterstitialAd = interstitialAd;
                Log.i("TAG", "onAdLoaded");

                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        // Called when fullscreen content is dismissed.
                        Log.d("TAG", "The ad was dismissed.");

                        new Handler().postDelayed(() -> {
                            startMatching();
                        }, 5000);
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                        // Called when fullscreen content failed to show.
                        Log.d("TAG", "The ad failed to show.");
                        new Handler().postDelayed(() -> {
                            startMatching();
                        }, 5000);
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        // Called when fullscreen content is shown.
                        // Make sure to set your reference to null so you don't
                        // show it a second time.
                        mInterstitialAd = null;
                        Log.d("TAG", "The ad was shown.");
//                        new Handler().postDelayed(() -> {
//                            startMatching();
//                        }, 5000);
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                // Handle the error
                Log.i("TAG", loadAdError.getMessage());
                mInterstitialAd = null;
                new Handler().postDelayed(() -> {
                    startMatching();
                }, 5000);
            }
        });
    }

    private void showInterstitialAd() {
        if (mInterstitialAd != null) {
            mInterstitialAd.show(HomeActivity.this);
            nextValue = getRandomInRange(NEXT_VALUE_MIN, NEXT_VALUE_MAX);
            //counter = 0;
            // loadInterstitialAd();
        } else {
            counter++;
            Log.d("TAG", "The interstitial ad wasn't ready yet.");
            Log.d("TAG", "The counter is  " + counter);
            Log.d("TAG", "The next is  " + nextValue);
            if (counter >= nextValue) {
                Log.d("TAG", "The add is gonna load");

                loadInterstitialAd();
                nextValue = getRandomInRange(NEXT_VALUE_MIN, NEXT_VALUE_MAX);
                counter = 0;
            }

            new Handler().postDelayed(() -> {
                startMatching();
            }, 5000);
        }
    }

    private void backToStart() {
        backToStartLAV.setOnClickListener(v -> {
            try {
                backToStartImpl();
            } catch (Exception e) {
                onException();
            }
        });
    }

    private void backToStartImpl() {
        closingWebview();
        Intent startActivityIntent = new Intent(HomeActivity.this, StartActivity.class);
        startActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startActivityIntent);
        //startActivity(new Intent(HomeActivity.this, StartActivity.class));
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }

    private void init() {
        try {
            initializeWS();
            startMatching();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closingWebview() {
        if (webSocket != null) {
            webSocket.close(1000, "GOOD BYE");
            webSocket = null;
        }
        //
        if (videoWV != null) {
            videoWV.clearCache(true);
            videoWV.clearHistory();
            videoWV.onPause();
            videoWV.removeAllViews();
            videoWV.destroyDrawingCache();
//                                    videoWV.pauseTimers();
            videoWV.loadUrl("");
//            videoWV = null;
        }
    }

    private void startMatching() {
        //Start Matching
        loadingMsgTW.setText("Waiting for match...");
        try {
            loadingMsgTW.setText("Waiting for match...");
            RequestBody requestBody = new RequestBody();
            requestBody.setId(Integer.parseInt(uuid));
            requestBody.setGender((byte) 2);
            requestBody.setIntrestedGender((byte) 2);
            requestBody.setMsgType("START_MATCHING");
            requestBody.setMatchingPresense(true);
            requestBody.setKeywords(new String[]{});
            requestBody.setMsgText("START_MATCHING");
            requestBody.setServiceId(1);
            requestBody.setServiceType("START_MATCHING");
            MessageBean bean = new MessageBean("" + uuid, "SYSTEM", requestBody);

            try {
                String str = new Gson().toJson(bean);
                System.out.println("Calling============================>" + str);
                webSocket.send(str);
            } catch (Exception e) {
                e.printStackTrace();
            }
            loadingFrame.setVisibility(View.VISIBLE);
            mAdView.setVisibility(View.VISIBLE);
        } catch (Exception ex) {
            onException();
            ex.printStackTrace();
        }
    }

    private void initializeWS() {
        try {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            okHttpClient = builder.build();
            String url = wsserverProtocol + "://" + serverHost + ":" + serverPort + serverBase + "/messenger/" + uuid;
            //Request request = new Request.Builder().url("ws://116.73.15.125:8080/LiveMatchingEngine/messenger/"+uuid).build();
            // Request request = new Request.Builder().url("ws://" + Util.BASE_PATH + "/messenger/" + uuid).build();
            Request request = new Request.Builder().url(url).build();
            webSocket = okHttpClient.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                    runOnUiThread(() -> {
//                        try {
//                            Toast.makeText(HomeActivity.this, "Connection Closed" + reason, Toast.LENGTH_SHORT).show();
//                        }catch (Exception e){
                        onException("Connection Closed");
//                        }
                    });
                }

                @Override
                public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                    try {
                        messageHandler(text);
                    } catch (Exception e) {
                        e.printStackTrace();
                        onException();
                    }
                }

                @Override
                public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
//                    runOnUiThread(() -> {
//                        Toast.makeText(HomeActivity.this, "Connected to the server", Toast.LENGTH_SHORT).show();
//                    });
                }

                @Override
                public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
//                    runOnUiThread(() -> {
//                        Toast.makeText(HomeActivity.this, "Closing session =" + reason, Toast.LENGTH_SHORT).show();
//                    });

                }

                @Override
                public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
//                    runOnUiThread(() -> {
//                        Toast.makeText(HomeActivity.this, "Failed session =" + t.getMessage(), Toast.LENGTH_LONG).show();
//                        t.printStackTrace();
//                    });

                    onException();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            onException();
        }
    }

    private void recieveSDP(MessageBean bean) {
        try {
            callJavascriptFunction("javascript:recieveSDP('" + bean.getMessageBody().getMsgText() + "')");
        } catch (Exception e) {
            e.printStackTrace();
            onException();
        }
    }


    private void strangerMatched(MessageBean bean) {
        try {

            if (status == 0) {
                currBean = bean;
                loadingFrame.setVisibility(View.GONE);
                mAdView.setVisibility(View.GONE);
                TransitionManager.beginDelayedTransition(videoFrame, new Slide(Gravity.RIGHT));
                videoFrame.setVisibility(View.VISIBLE);
                setupWebView();
                videoWV.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        callJavascriptFunction("javascript:init(" + bean.getMessageBody().isInitiator() + ")");
                    }
                });
                findViewById(R.id.swipeupView).setVisibility(View.GONE);
                findViewById(R.id.swipeUpToSkipTW).setVisibility(View.GONE);
                new Handler().postDelayed(() -> {
                    swipeListener = new SwipeListener(videoWV);
                    findViewById(R.id.swipeupView).setVisibility(View.VISIBLE);
                    findViewById(R.id.swipeUpToSkipTW).setVisibility(View.VISIBLE);
                }, 5000);
                status = 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            onException();
        }


    }

    private void messageHandler(String text) {
        //Waiting for reponse
        System.out.println("Message recieved-------------------------" + text);
        try {
            MessageBean bean = gson.fromJson(text, MessageBean.class);
            if (bean.getMessageBody() != null) {
                switch (bean.getMessageBody().getMsgType()) {
                    case "STRANGER_MATCHED":
                        runOnUiThread(() -> {
                            loadingMsgTW.setText("You got a match");
                            new Handler().postDelayed(() -> {
                                strangerMatched(bean);
                            }, 2000);


                            //Toast.makeText(HomeActivity.this, "You have matched with == " + bean.getMessageBody().getMatchedWith(), Toast.LENGTH_SHORT).show();
                        });
                        break;
                    case "SHARING_SDP":
                        runOnUiThread(() -> {
                            recieveSDP(bean);
                            //Toast.makeText(HomeActivity.this, "You have matched with == " + bean.getMessageBody().getMatchedWith(), Toast.LENGTH_SHORT).show();
                        });
                        break;

                    case "ONLINE_USERS":
                        runOnUiThread(() -> {
                            onlineUserTw.setText("Online Users : " + bean.getMessageBody().getMsgText());

                            //System.out.println("ONLINE USER ====="+bean.getMessageBody().getMsgText());
                            // recieveSDP(bean);
                            //Toast.makeText(HomeActivity.this, "You have matched with == " + bean.getMessageBody().getMatchedWith(), Toast.LENGTH_SHORT).show();
                        });
                        break;
                    default:
                        System.out.println("Unable to recognised text==" + text);
                }
            }
        } catch (Exception e) {
            onException();
        }
    }

    private void callJavascriptFunction(String scriptName) {
        System.out.println("---------------------------> Calling javascript  function");
        videoWV.post(() -> {
            videoWV.evaluateJavascript(scriptName, null);
        });
    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        videoWV.clearCache(true);
//        videoWV.clearHistory();
//        videoWV.onPause();
//        videoWV.removeAllViews();
//        videoWV.destroyDrawingCache();
//        videoWV.pauseTimers();
//        videoWV = null;
////        finish();
//    }

    private void setupWebView() {
        System.out.println("Setup WV calling  TAG");
        videoWV.getSettings().setJavaScriptEnabled(true);
        videoWV.getSettings().setMediaPlaybackRequiresUserGesture(false);
        videoWV.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    request.grant(request.getResources());
                }
            }


        });

        videoWV.addJavascriptInterface(new JSInterface() {
            @Override
            @android.webkit.JavascriptInterface()
            public void sendSDP(String sdpData) {
                RequestBody sdpBody = new RequestBody();
                sdpBody.setMsgType("SHARING_SDP");
                sdpBody.setMsgText(sdpData);
                sdpBody.setServiceId(1);
                sdpBody.setServiceType("SHARING_SDP");
                MessageBean bean = new MessageBean("" + uuid, "" + currBean.getMessageBody().getMatchedWith(), sdpBody);

                try {
                    String str = new Gson().toJson(bean);
                    System.out.println("Sending ============================>" + str);
                    webSocket.send(str);
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }

            @Override
            @android.webkit.JavascriptInterface()
            public void next() {
                try {
                    runOnUiThread(() -> {
                        skipToNext();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            @android.webkit.JavascriptInterface()
            public void onException() {
                onException();
            }
        }, "Android");
        String filePath = "file:android_asset/call.html?rand=" + new Random().nextInt();
        videoWV.loadUrl(filePath);
        //callJavascriptFunction("javascript:test('a','b')");

    }

    private void onException() {
        runOnUiThread(() -> {
            Toast.makeText(HomeActivity.this, "Something went wrong with our end. Please try again later", Toast.LENGTH_LONG).show();
            backToStartImpl();
        });

    }

    private void onException(String msg) {
        Toast.makeText(HomeActivity.this, "" + msg, Toast.LENGTH_LONG).show();
        backToStartImpl();
    }

    private void skipToNext() {

        status = 0;
        loadingFrame.setVisibility(View.GONE);
        videoFrame.setVisibility(View.GONE);
        ownFaceFrame.setVisibility(View.GONE);
        TransitionManager.beginDelayedTransition(loadingFrame, new Slide(Gravity.BOTTOM));
        loadingFrame.setVisibility(View.VISIBLE);
        mAdView.setVisibility(View.VISIBLE);
        videoWV.setOnTouchListener(null);
        showInterstitialAd();
        loadingMsgTW.setText("Waiting for match...");

    }

    public class SwipeListener implements View.OnTouchListener {
        GestureDetector gestureDetector;


        SwipeListener(View view) {
            System.out.println("Swipe Is INTIALIZEEED11111111111111111111111111");
            int threshold = 300;
            int velocityThreshold = 300;


            GestureDetector.SimpleOnGestureListener listener =
                    new GestureDetector.SimpleOnGestureListener() {

                        @Override
                        public boolean onDown(MotionEvent e) {

                            return true;
                        }

                        @Override
                        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                            float xDiff = e2.getX() - e1.getX();
                            float yDiff = e2.getY() - e1.getY();

                            try {
                                if (Math.abs(xDiff) > Math.abs(yDiff)) {
                                    if (Math.abs(xDiff) > threshold && Math.abs(velocityX) > velocityThreshold) {
                                        if (xDiff > 0) {
//                                            Toast.makeText(HomeActivity.this, "R", Toast.LENGTH_LONG).show();
                                        } else {
//                                            Toast.makeText(HomeActivity.this, "L", Toast.LENGTH_LONG).show();
                                        }

                                        return true;

                                    }

                                } else {
                                    if (Math.abs(yDiff) > threshold && Math.abs(velocityY) > velocityThreshold) {
                                        if (yDiff > 0) {
//                                            Toast.makeText(HomeActivity.this, "D", Toast.LENGTH_LONG).show();
                                        } else {
                                            callJavascriptFunction("close()");
                                            runOnUiThread(() -> {
                                                skipToNext();

                                            });

//                                            Toast.makeText(HomeActivity.this, "U", Toast.LENGTH_LONG).show();
                                        }

                                        return true;

                                    }

                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return false;
                        }
                    };


            gestureDetector = new GestureDetector(listener);
            view.setOnTouchListener(this);
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return gestureDetector.onTouchEvent(event);
        }
    }


}