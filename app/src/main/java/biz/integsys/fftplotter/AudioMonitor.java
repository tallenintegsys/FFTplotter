package biz.integsys.fftplotter;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * Created by tallen on 12/11/15.
 * This monitors the mic and sends callbacks based on what it "hears" e.g. DTMFs
 */
class AudioMonitor {
    private final String TAG = "AudioMonitor";
    private AudioRecord audioRecord;
    private Thread monitorThread;
    private int sampleSize;
    public static final int SAMPLE_RATE = 44100;
    public static final int STATE_INITIALIZED = AudioRecord.STATE_INITIALIZED;
    private short[] recordBuffer;
    private float[] re;
    private float[] im;
    private float[] zero;
    private Float[] amplitude;
    private FFT fft;
    private boolean enable;
    private AudioMonitorListener listener = null;

    public AudioMonitor(AudioMonitorListener listener) {
        this.listener = listener;
    }

    public int init(int exponent) {
        sampleSize = 2 << (exponent-1);
        if (audioRecord != null)
            audioRecord.release();
        recordBuffer = new short[sampleSize];
        re = new float[sampleSize];
        im = new float[sampleSize];
        zero = new float[sampleSize];
        amplitude = new Float[sampleSize];
        fft = new FFT(sampleSize);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, recordBuffer.length);
        return audioRecord.getState();
    }

    public void start() {
        enable = true;
        audioRecord.startRecording();
        monitorThread = new Thread(new Runnable() {
            public void run() {
                do {
                    int accumulated = 0;
                    do { // audioRecord.read(recordBuffer, 0, SAMPLE_SIZE, AudioRecord.READ_BLOCKING);
                        accumulated += audioRecord.read(recordBuffer, accumulated, recordBuffer.length - accumulated);
                    } while (accumulated < recordBuffer.length);
                    im = zero.clone();
                    for (int i=0; i<recordBuffer.length; i++)
                        re[i] = recordBuffer[i] * .00001f; //vectorized?
                    fft.fft(re, im);
                    if (listener != null)
                        listener.transformedResult(re);
                } while (enable);
                    audioRecord.stop();
            }
        });
        monitorThread.start();
    }

    public void stop() {
        enable = false;
    }

    public synchronized Float[] getAmplitude() {
        updateAmplitude();
        return amplitude;
    }

    private synchronized void updateAmplitude() {
        for (int i = 0; i < re.length; i++)
            amplitude[i] = (float) Math.cos(i/re.length);
    }

    public int getSampleSize() {
        return sampleSize;
    }

}
