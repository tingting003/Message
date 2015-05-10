package com.example.ting.sample_1;

import android.content.IntentSender;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;


public class MainMobile extends ActionBarActivity implements View.OnClickListener{

    private static final int GOO_API_CLIENT_REQUEST_RESOLVE_ERROR=1000;//排除Google Play services連線錯誤用的辨識碼。
    private GoogleApiClient mGoogleApiClient;

    private boolean mbResolvingGooApiClientError=false;//處理Google Play services連線的錯誤
    private static final String WEARABLE_PATH_SEND_MESSAGE="/message";

    EditText notesET;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_mobile);

        notesET=(EditText)findViewById(R.id.editText);

        Button bt=(Button)findViewById(R.id.button);
        bt.setOnClickListener(this);


        mGoogleApiClient=new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(gooApiClientConnCallback)
                .addOnConnectionFailedListener(gooApiClientOnConnFail)
                .build();
    }
    @Override
    protected void onStart(){
        super.onStart();
        if(!mbResolvingGooApiClientError){
            mGoogleApiClient.connect();
        }
    }
    @Override
    protected void onStop(){
        if (!mbResolvingGooApiClientError){
            Wearable.MessageApi.removeListener(mGoogleApiClient,wearableMsgListener);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }
    private GoogleApiClient.ConnectionCallbacks gooApiClientConnCallback=new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {
            mbResolvingGooApiClientError=false;
            Wearable.MessageApi.addListener(mGoogleApiClient,wearableMsgListener);
        }

        @Override
        public void onConnectionSuspended(int i) {
            Toast.makeText(getApplicationContext(), "Google API Can't connected", Toast.LENGTH_LONG).show();

        }
    };
    private GoogleApiClient.OnConnectionFailedListener gooApiClientOnConnFail=new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            if (mbResolvingGooApiClientError){
                return;
            }else if(connectionResult.hasResolution()){
                try {
                    mbResolvingGooApiClientError=true;
                    connectionResult.startResolutionForResult(MainMobile.this,GOO_API_CLIENT_REQUEST_RESOLVE_ERROR);
                }catch (IntentSender.SendIntentException e){
                    mbResolvingGooApiClientError=false;
                    mGoogleApiClient.connect();
                }
            }else {
                mbResolvingGooApiClientError=false;
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

                            Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            };
    private class AsyncTaskSendMessageToOtherDevices extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] objects) {
            // 取得所有連線的Android裝置。
            NodeApi.GetConnectedNodesResult connectedWearableDevices =
                    Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

            // 傳送message給每一個連線的Android裝置。
            for (Node node : connectedWearableDevices.getNodes()) {
                // 把Message要附帶的資料儲存在一個位元組陣列中。
                String notes=notesET.getText()+"";
                byte[] payload = notes.getBytes();
                // 傳送message。
                Wearable.MessageApi.sendMessage( mGoogleApiClient, node.getId(),WEARABLE_PATH_SEND_MESSAGE,payload);
            }

            return null;
        }
    }
    @Override
    public void onClick(View view) {
        new AsyncTaskSendMessageToOtherDevices().execute();
    }

}
