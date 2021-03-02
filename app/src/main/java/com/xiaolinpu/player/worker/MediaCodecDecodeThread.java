package com.xiaolinpu.player.worker;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class MediaCodecDecodeThread extends Thread {

    public static final String TAG = "MCDThread";
    public static final boolean VERBOSE_LOG = false;
    private static AtomicInteger sCount = new AtomicInteger(0);

    private MediaExtractor extractor;
    private int trackNumber;
    private Surface outputSurface;
    private MediaFormat format;
    private MediaCodec codec;

    public MediaCodecDecodeThread(
            @NonNull MediaExtractor extractor,
            @IntRange(from = 0) int trackNumber,
            @NonNull Surface outputSurface
    ) throws IOException {
        super(TAG + "#" + sCount.incrementAndGet());
        this.extractor = extractor;
        this.trackNumber = trackNumber;
        this.outputSurface = outputSurface;
        readExtractorInfo();
        configureMediaCodec();
    }

    private void readExtractorInfo() {
        int trackCount = extractor.getTrackCount();
        android.util.Log.d(TAG, "media track count: " + trackCount + ", selection: " + trackNumber);

        if (trackNumber >= trackCount) {
            throw new IllegalArgumentException("track index out of bound");
        }

        this.format = extractor.getTrackFormat(trackNumber);
        android.util.Log.d(TAG, "media track format: " + format);

        extractor.selectTrack(trackNumber);
    }

    private void configureMediaCodec() throws IOException {
        String mime = format.getString(MediaFormat.KEY_MIME);
        if (mime == null) {
            throw new IllegalArgumentException("mime type is null for format " + format);
        }
        codec = MediaCodec.createDecoderByType(mime);
        codec.configure(format, outputSurface, null, 0);
        codec.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT);
        Log.d(TAG, "media codec info: " + codec.getCodecInfo());
    }

    @Override
    public void run() {
        boolean eof = false;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        codec.start();
        android.util.Log.d(TAG, "run, codec started");
        while(!interrupted() && !eof) {
            int bufferId = codec.dequeueInputBuffer(10000); // 20ms
            if (VERBOSE_LOG) {
                android.util.Log.d(TAG, "run, bufferId: " + bufferId);
            }
            if (bufferId > 0) {
                ByteBuffer buffer = codec.getInputBuffer(bufferId);
                if (buffer == null) {
                    android.util.Log.d(TAG, "run, buffer == null");
                    continue;
                }
                int read = extractor.readSampleData(buffer, 0);
                if (read < 0) {
                    android.util.Log.d(TAG, "run, EOF as read = " + read);
                    codec.queueInputBuffer(bufferId, 0, 0, 0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    eof = true;
                } else {
                    if (VERBOSE_LOG) {
                        android.util.Log.d(TAG, "run, read normal: " + read + ", pts: " + extractor.getSampleTime());
                    }
                    codec.queueInputBuffer(bufferId, 0, read, extractor.getSampleTime(), 0);
                    extractor.advance();
                }
                int outBufferId = codec.dequeueOutputBuffer(bufferInfo, 10000);
                if (VERBOSE_LOG) {
                    android.util.Log.d(TAG, "run, output buffer id: " + outBufferId + ", info: " + bufferInfo);
                }
                if (outBufferId > 0) {
                    codec.releaseOutputBuffer(outBufferId, true);
                }
            }
        }
        recycleResources();
    }

    private void recycleResources() {
        extractor.release();
        android.util.Log.d(TAG, "recycleResources");
    }
}
