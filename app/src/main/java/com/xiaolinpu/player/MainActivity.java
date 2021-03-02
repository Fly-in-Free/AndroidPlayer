package com.xiaolinpu.player;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.xiaolinpu.player.pages.MediaCodecActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnMediaCodec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnMediaCodec = findViewById(R.id.btn_media_codec);
        btnMediaCodec.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        // keep for later extension
        //noinspection SwitchStatementWithTooFewBranches
        switch (v.getId()) {
            case R.id.btn_media_codec:
                startActivity(new Intent(this, MediaCodecActivity.class));
                break;
            default:
                break;
        }
    }
}
