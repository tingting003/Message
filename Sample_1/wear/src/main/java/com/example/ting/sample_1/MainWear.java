package com.example.ting.sample_1;

import android.app.Activity;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

public class MainWear extends Activity {


    private static final int GOO_API_CLIENT_REQUEST_RESOLVE_ERROR=1000;//排除Google Play services連線錯誤用的辨識碼。
    private GoogleApiClient mGoogleApiClient;

    private boolean mbResolvingGooApiClientError=false;//處理Google Play services連線的錯誤
    private static final String WEARABLE_PATH_SEND_MESSAGE="/message";

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_wear);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.textView2);

                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });
        mGoogleApiClient=new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(gooApiClientConnCallback)
                .addOnConnectionFailedListener(gooApiClientOnConnFail)
                .build();
    }
    @Override
    protected void onStart(){
        super.onStart();
        mGoogleApiClient.connect();
    }
    @Override
    protected void onStop(){
        if (!mbResolvingGooApiClientError){
            Wearable.MessageApi.removeListener(mGoogleApiClient,wearableMsgListener);
        }
        super.onStop();
    }
    private GoogleApiClient.ConnectionCallbacks gooApiClientConnCallback=new GoogleApiClient.ConnectionCallbacks(){
        @Override
        public void onConnected(Bundle bundle){
            mbResolvingGooApiClientError=false;
            Wearable.MessageApi.addListener(mGoogleApiClient,wearableMsgListener);
        }
        @Override
        public void onConnectionSuspended(int i){
            Toast.makeText(getApplicationContext(), "Google API Client 無法連線", Toast.LENGTH_LONG).show();
        }

    };
    private GoogleApiClient.OnConnectionFailedListener gooApiClientOnConnFail=new GoogleApiClient.OnConnectionFailedListener(){
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            if (mbResolvingGooApiClientError) {
                return;
            } else if (connectionResult.hasResolution()) {
                try {
                    mbResolvingGooApiClientError = true;
                    connectionResult.startResolutionForResult(MainWear.this, GOO_API_CLIENT_REQUEST_RESOLVE_ERROR);
                } catch (IntentSender.SendIntentException e) {
                    mbResolvingGooApiClientError = false;
                    mGoogleApiClient.connect();
                }
            } else {
                mbResolvingGooApiClientError = false;
                Wearable.MessageApi.removeListener(mGoogleApiClient,wearableMsgListener);
            }
        }
    };
    private MessageApi.MessageListener wearableMsgListener = new MessageApi.MessageListener() {
                @Override
                public void onMessageReceived(final MessageEvent messageEvent) {
                    // 呼叫messageEvent.getData()取得message中附帶的位元組陣列。
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String s = null;
                            try {
                                s = new String(messageEvent.getData(), "UTF-8");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            mTextView.setText(s);

                            new AsyncTaskReplyMessage().execute(messageEvent.getSourceNodeId());
                        }
                    });
                }
            };
    private class AsyncTaskReplyMessage extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... strings) {
            byte[] payload = "對方收到留言".getBytes();
            // Send the message and get the result.
            MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                    mGoogleApiClient, strings[0], WEARABLE_PATH_SEND_MESSAGE,payload).await();
            return null;
        }
    }
}
