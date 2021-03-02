package com.xiaolinpu.player.pages;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.xiaolinpu.player.R;
import com.xiaolinpu.player.worker.MediaCodecDecodeThread;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;

public class MediaCodecActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    public static final String TAG = "MediaCodecActivity";

    private SurfaceView videoSurface;
    private MediaExtractor extractor;
    private MediaCodecList codecList;
    private MediaCodecDecodeThread decodeThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_codec);

        getMediaCodecList();
        initMedia();
        videoSurface = findViewById(R.id.video_surface);
        videoSurface.getHolder().addCallback(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void initMedia() {
        extractor = new MediaExtractor();
        try {
            AssetFileDescriptor afd = getAssets().openFd("video.mp4");
            Log.d(TAG, "file: " + afd.toString());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                extractor.setDataSource(afd);
            } else {
                extractor.setDataSource(afd.getFileDescriptor());
            }
            afd.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        android.util.Log.d(TAG, "surfaceChanged, holder: " + holder);
        try {
            decodeThread = new MediaCodecDecodeThread(extractor, 0, holder.getSurface());
            decodeThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        android.util.Log.d(TAG, "surfaceDestroyed, holder: " + holder);
    }
}