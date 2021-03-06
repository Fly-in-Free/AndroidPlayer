package com.xiaolinpu.player.worker;

import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicInteger;

public class MediaCodecADecodeThread extends Thread {

    public static final String TAG = "AudioMCDThread";
    public static final boolean VERBOSE_LOG = false;
    private static AtomicInteger sCount = new AtomicInteger(0);

    private MediaExtractor extractor;
    private int trackNumber;
    private MediaFormat format;
    private MediaCodec codec;
    private AudioTrack track;

    @RequiresApi(api = Build.VERSION_CODES.N)
    public MediaCodecADecodeThread(
            @NonNull AssetFileDescriptor afd,
            @IntRange(from = 0) int trackNumber
    ) throws IOException {
        super(TAG + "#" + sCount.incrementAndGet());
        this.extractor = new MediaExtractor();
        this.extractor.setDataSource(afd);
        this.trackNumber = trackNumber;
        readExtractorInfo();
        configureAudioTrack();
        configureMediaCodec();
    }

    private void readExtractorInfo() {
        int trackCount = extractor.getTrackCount();
        Log.d(TAG, "media track count: " + trackCount + ", selection: " + trackNumber);

        if (trackNumber >= trackCount) {
            throw new IllegalArgumentException("track index out of bound");
        }

        this.format = extractor.getTrackFormat(trackNumber);
        Log.d(TAG, "media track format: " + format);

        extractor.selectTrack(trackNumber);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void configureAudioTrack() {
        track = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(format.getInteger(MediaFormat.KEY_SAMPLE_RATE))
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build())
                .setBufferSizeInBytes(
                        AudioTrack.getMinBufferSize(
                                format.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                                AudioFormat.CHANNEL_OUT_STEREO,
                                AudioFormat.ENCODING_PCM_16BIT
                        ) * 4
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();
    }

    private void configureMediaCodec() throws IOException {
        String mime = format.getString(MediaFormat.KEY_MIME);
        if (mime == null) {
            throw new IllegalArgumentException("mime type is null for format " + format);
        }
        codec = MediaCodec.createDecoderByType(mime);
        codec.configure(format, null, null, 0);
        Log.d(TAG, "media codec info: " + codec.getCodecInfo());
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void run() {
        boolean eof = false;
        long startMs = System.currentTimeMillis();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        codec.start();
        track.play();
        Log.d(TAG, "run, codec started");
        while(!interrupted() && !eof) {
            int bufferId = codec.dequeueInputBuffer(10000); // 20ms
            if (VERBOSE_LOG) {
                Log.d(TAG, "run, bufferId: " + bufferId);
            }
            if (bufferId > 0) {
                ByteBuffer buffer = codec.getInputBuffer(bufferId);
                if (buffer == null) {
                    Log.d(TAG, "run, buffer == null");
                    continue;
                }
                buffer.clear();
                int read = extractor.readSampleData(buffer, 0);
                if (read < 0) {
                    Log.d(TAG, "run, EOF as read = " + read);
                    codec.queueInputBuffer(bufferId, 0, 0, 0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    eof = true;
                } else {
                    if (VERBOSE_LOG) {
                        Log.d(TAG, "run, read normal: " + read + ", pts: " + extractor.getSampleTime());
                    }
                    codec.queueInputBuffer(bufferId, 0, read, extractor.getSampleTime(), 0);
                    extractor.advance();
                }
                int outBufferId = codec.dequeueOutputBuffer(bufferInfo, 10000);
                while (outBufferId > 0) {
                    if (VERBOSE_LOG) {
                        Log.d(TAG, "run, output buffer id: " + outBufferId);
                    }
                    ByteBuffer outBuffer = codec.getOutputBuffer(outBufferId);
                    if (outBuffer != null) {
                        byte[] data = new byte[bufferInfo.size];
                        outBuffer.get(data, 0, bufferInfo.size);
                        track.write(data, 0, bufferInfo.size, AudioTrack.WRITE_BLOCKING);
                        outBuffer.clear();
                    }
                    codec.releaseOutputBuffer(outBufferId, false);
                    outBufferId = codec.dequeueOutputBuffer(bufferInfo, 1000);
                }
                long diff = bufferInfo.presentationTimeUs / 1000L - (System.currentTimeMillis() - startMs);
                if (diff > 0) {
                    try {
                        if (VERBOSE_LOG) {
                            Log.d(TAG, "run, ahead of presentation, sleep ms: " + diff);
                        }
                        sleep(diff);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        recycleResources();
    }

    private void recycleResources() {
        extractor.release();
        Log.d(TAG, "recycleResources");
    }
}
