package com.onetrust.mobileConsentCollectionDemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.adobe.marketing.mobile.*;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.button_in).setOnClickListener(this);
        findViewById(R.id.button_out).setOnClickListener(this);
        findViewById(R.id.button_unknown).setOnClickListener(this);


        // set the default UI status
        TextView label = findViewById(R.id.currentStatus);
        label.setText("Loading...");
        
        // get the latest from Adobe and update UI
        MobileCore.getPrivacyStatus(new AdobeCallback<MobilePrivacyStatus>() {
            @Override
            public void call(MobilePrivacyStatus mobilePrivacyStatus) {
                TextView label = findViewById(R.id.currentStatus);
                label.setText(mobilePrivacyStatus.toString());
            }
        });

    }


    @Override
    public void onClick(View view) {

        TextView label = findViewById(R.id.currentStatus);
        label.setText("Sending...");

        // Send to Adobe
        switch (view.getId()) {
            case R.id.button_in:
                MobileCore.setPrivacyStatus(MobilePrivacyStatus.OPT_IN);
                break;
            case R.id.button_out:
                MobileCore.setPrivacyStatus(MobilePrivacyStatus.OPT_OUT);
                break;
            case R.id.button_unknown:
                MobileCore.setPrivacyStatus(MobilePrivacyStatus.OPT_IN);
                break;
        }


        // Get From Adobe and Update UI
        MobileCore.getPrivacyStatus(new AdobeCallback<MobilePrivacyStatus>() {
            @Override
            public void call(MobilePrivacyStatus mobilePrivacyStatus) {
                TextView label = findViewById(R.id.currentStatus);
                label.setText(mobilePrivacyStatus.toString());
            }
        });

    }

}
