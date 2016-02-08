package biz.integsys.fftplotter;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

/**
 * Created by tallen on 12/11/15.
 * This monitors the mic and sends callbacks with the FFT of what it "hears"
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
    private volatile boolean enable;
    private AudioMonitorListener listener = null;

    public AudioMonitor(AudioMonitorListener listener) {
        this.listener = listener;
    }

    /**
     * get mic access and set the sample size (e.g. n = 2 ^ x)
     * @param exponent
     * @return AudioRecord.STATE_INITIALIZED on success
     */
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

    /**
     * Begin monitoring mic
     */
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

    /**
     * stop monitoring mic
     */
    public void stop() {
        enable = false;
    }

    /**
     * check the state of mic monitoring
     * @return
     */
    public boolean isRunning() { return monitorThread.isAlive(); }

}
