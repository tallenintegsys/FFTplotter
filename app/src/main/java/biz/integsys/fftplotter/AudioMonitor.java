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
    public static final int BUFFER_SIZE = 2048;
    public static final int SAMPLE_SIZE = 2048;
    public static final int SAMPLE_RATE = 8000;
    public static final int STATE_INITIALIZED = AudioRecord.STATE_INITIALIZED;
    private final float[] recordBuffer= new float[BUFFER_SIZE];
    private final float[] re = new float[SAMPLE_SIZE];
    private float[] im = new float[SAMPLE_SIZE];
    private final float[] zero = new float[SAMPLE_SIZE];
    private final Float[] amplitude = new Float[SAMPLE_SIZE];
    private final FFT fft = new FFT(SAMPLE_SIZE);
    private boolean enable;
    private AudioMonitorListener listener = null;

    public AudioMonitor(AudioMonitorListener listener) {
        this.listener = listener;
    }

    public int init() {
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT, BUFFER_SIZE);
        return audioRecord.getState();
    }

    public void start() {
        enable = true;
        audioRecord.startRecording();
        monitorThread = new Thread(new Runnable() {
            public void run() {
                do {
                    audioRecord.read(recordBuffer, 0, BUFFER_SIZE, AudioRecord.READ_BLOCKING);
                    //Log.d(TAG, "read " + read + " floats.");
                    im = zero.clone(); //memset, I hope?
                    System.arraycopy(recordBuffer, 0, re, 0, SAMPLE_SIZE); //memset, I presume
                    fft.fft(re, im);
                    if (listener != null)
                        listener.transformedResult(re);
                } while (enable);
                for (int i = 0; i < SAMPLE_SIZE; i++) {
                    if ((Math.abs(im[i]) > 1000) || (Math.abs(re[i]) > 1000))
                        Log.v(TAG, "i="+i+"   x="+ im[i]+"   y="+ re[i]);
                    audioRecord.stop();
                }
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
            amplitude[i] = (float) Math.cos(i/ SAMPLE_SIZE);
    }
}
