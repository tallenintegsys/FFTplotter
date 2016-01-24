package biz.integsys.fftplotter;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

public class MainActivity extends AppCompatActivity implements biz.integsys.fftplotter.AudioMonitorListener {
    private final String TAG = "MainActivity";
    private final AudioMonitor audioMonitor = new AudioMonitor(this);
    private XYPlot plot;
    private static final int RECORD_AUDIO_PERMISSION = 1;
    private Switch enableSwitch;
    private PlotData plotData;

    class PlotData implements XYSeries {
        private float am[];

        public PlotData(int sampleSize) {
            super();
            am = new float[sampleSize];
        }
        public void set (float[] am) {
            this.am = am;
        }
        @Override
        public int size() { return am.length/2; }

        @Override
        public Number getX(int index) {return index * AudioMonitor.SAMPLE_RATE / am.length;
        }

        @Override
        public Number getY(int index) {
            return am[index]; //this is efficient??!
        }

        @Override
        public String getTitle() {
            return "";
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_PERMISSION);
        else
            audioMonitor.init(11);
    }

    @Override
    protected void onResume() {
        super.onResume();
        plotData = new PlotData(11);
        plot = (XYPlot) findViewById(R.id.plot);
        LineAndPointFormatter series1Format = new LineAndPointFormatter(Color.RED, null, null, null);
        plot.addSeries(plotData, series1Format);
        plot.setUserRangeOrigin(0);
        plot.setRangeBoundaries(-10,10, BoundaryMode.FIXED);
        plot.getGraphWidget().setDomainLabelOrientation(45);
        plot.getGraphWidget().setDomainTickLabelHorizontalOffset(15);
        plot.getGraphWidget().setDomainTickLabelVerticalOffset(15);
        plot.getLegendWidget().setVisible(false);

        enableSwitch = (Switch) findViewById(R.id.enable);
        enableSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    audioMonitor.start();
                else
                    audioMonitor.stop();
            }
        });
        plot.redraw();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case RECORD_AUDIO_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    audioMonitor.init(11);
                } else {
                    // permission denied
                    enableSwitch.setEnabled(false);
                }
            }
        }
    }

    public void transformedResult(float[] result) {
        plotData.set(result);
        plot.redraw();
        //Log.i(TAG, "result... ");
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
