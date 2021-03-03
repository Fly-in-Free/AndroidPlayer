package com.xiaolinpu.player.pages;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.res.AssetFileDescriptor;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.xiaolinpu.player.R;
import com.xiaolinpu.player.worker.MediaCodecADecodeThread;
import com.xiaolinpu.player.worker.MediaCodecVDecodeThread;

import java.io.IOException;

public class MediaCodecActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    public static final String TAG = "MediaCodecActivity";

    private SurfaceView videoSurface;
    private MediaCodecList codecList;
    private MediaCodecVDecodeThread decodeThread;
    private MediaCodecADecodeThread audioThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_codec);

        getMediaCodecList();
        videoSurface = findViewById(R.id.video_surface);
        videoSurface.getHolder().addCallback(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void getMediaCodecList() {
        codecList = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        for (MediaCodecInfo info: codecList.getCodecInfos()) {
            android.util.Log.d(TAG, "supported codec: " + info.getName());
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        android.util.Log.d(TAG, "surfaceCreated, holder: " + holder);
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.d(TAG, "surfaceChanged, holder: " + holder);
            try {
                AssetFileDescriptor afd = getAssets().openFd("video.mp4");
                decodeThread = new MediaCodecVDecodeThread(afd, 0, holder.getSurface());
                afd.close();
                afd = getAssets().openFd("video.mp4");
                audioThread = new MediaCodecADecodeThread(afd, 1);
                afd.close();
                decodeThread.start();
                audioThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        android.util.Log.d(TAG, "surfaceDestroyed, holder: " + holder);
    }
}