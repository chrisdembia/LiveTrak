package com.example.livetrak3;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static com.example.livetrak3.LoginActivity.APP_STORAGE_DIR;
import static com.example.livetrak3.LoginActivity.DEFAULT_CONFIG_FILE;

public class SessionActivity extends AppCompatActivity {
    private static String TAG = "SessionActivity";
    private static File appOutputDir = null;
    boolean isRunning = false;
    long timeOffset = -1;
    long pauseTime = -1;
    private FileWriter fw;
    private HashMap<String, RadioButtonGroup> buttonGroups = new HashMap();
    private LayoutData mLayoutData;
    private Timer timer;
    private TimerTask timerTask;
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    static private RadioButtonX addNewRadioButton(SessionActivity context,
                                                  OptionData od,
                                                  HashMap<String,
                                                          RadioButtonGroup> groups) {
        RadioButtonX rb = new RadioButtonX(context, od);
        rb.setPadding(rb.getPaddingLeft(),
                rb.getPaddingTop(),
                rb.getPaddingRight(),
                0);
        if (!(od == null || od.group == null || od.group.equalsIgnoreCase(
                "END"))) {
            RadioButtonGroup rbg;
            if (groups.containsKey(od.group)) {
                rbg = (RadioButtonGroup) groups.get(od.group);
            } else {
                rbg = new RadioButtonGroup(context, od.group);
                groups.put(od.group, rbg);
            }
            rbg.addButton(rb);
        }
        return rb;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appOutputDir = getOutputDir();
        Intent intent = getIntent();
        this.fw = createNewFile(intent);
        initFileHeader(intent);
        initializeUi(intent);
    }

    private File getOutputDir() {
        File root = Environment.getExternalStorageDirectory();
        Log.i(TAG,
                "getExternalStoragePublicDirectory(): " + root.getAbsolutePath());
        File outputDir = new File(root, APP_STORAGE_DIR);
        if (!(outputDir.exists() || outputDir.mkdirs())) {
            displayDialogAndExit(
                    "Output directory could not be created. App will exit.",
                    "Okay");
        }
        Log.i(TAG, "output dir exists now: " + outputDir.getAbsolutePath());
        return outputDir;
    }

    Long getRecordingTime() {
        return Long.valueOf(SystemClock.elapsedRealtime())
                .longValue() - timeOffset;
    }

    private void initFileHeader(Intent intent) {
        Bundle extras = intent.getExtras();
        writeLineToOutput("CODER_ID: " + extras.get(LoginActivity.CODER_ID));
        writeLineToOutput("SUBJECT_ID: " + extras.get(LoginActivity.SUBJECT_ID));
        writeLineToOutput("CONFIG_FILE: " + extras.get(LoginActivity.CONFIG_ID));
    }

    private void initializeUi(Intent intent) {

        setContentView(R.layout.activity_session);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        Bundle extras = intent.getExtras();
        getSupportActionBar().setTitle("Session for subject '" + extras.get(
                LoginActivity.SUBJECT_ID) + "'");

        String configFile = (String) extras.get(LoginActivity.CONFIG_ID);
        mLayoutData = loadData(configFile);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(
                getSupportFragmentManager(),
                mLayoutData);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);

        tabLayout.setupWithViewPager(mViewPager, true);
        // mViewPager.addOnPageChangeListener(new TabLayout
        // .TabLayoutOnPageChangeListener(tabLayout));
        // tabLayout.addOnTabSelectedListener(new TabLayout
        // .ViewPagerOnTabSelectedListener(mViewPager));

        // requestWindowFeature(1);
        // getWindow().setFlags(1024, 1024);
    }

    private LayoutData loadData(String configFileName) {

        try {
            AssetManager assetManager = getAssets();
            InputStream inputStream = null;
            if (configFileName.equals(DEFAULT_CONFIG_FILE)) {
                inputStream = assetManager.open(configFileName);
            } else {
                inputStream =
                        new FileInputStream((new File(LoginActivity.configDir,
                        configFileName)).toString());
            }
            LayoutData layoutData = (new LayoutCsvParser()).parse(inputStream);
            return layoutData;
        } catch (IOException e) {
            String errorMsg =
                    "Error: Could not open config file (" + configFileName +
                            "). App will exit.";
            Log.e(TAG, errorMsg);
            displayDialogAndExit(errorMsg, "Okay");
        } catch (Exception e) {
            String errorMsg = "Could not load config file. The format is " +
                    "likely incorrect. Details: " + e
                    .getMessage();
            Log.e(TAG, errorMsg);
            displayDialogAndExit(errorMsg, "Okay");
        }
        return null;

    }

    private FileWriter createNewFile(Intent intent) {
        try {
            Log.i(TAG, "Creating filewriter");
            Log.i(TAG, "filename: " + getFilename(intent));
            Log.i(TAG,
                    "file path: " + appOutputDir.isDirectory() + appOutputDir.exists());
            FileWriter fw = new FileWriter(appOutputDir + getFilename(intent));
            Log.i(TAG, "FileWriter created: " + fw.getClass().toString());
            return fw;
        } catch (IOException e) {
            e.printStackTrace();
            displayDialogAndExit(
                    "Output file could not be created. App will exit.",
                    "Okay");
            return null;
        }
    }

    public void displayDialogAndExit(String msg, String buttonText) {
        AlertDialog dialog =
                new AlertDialog.Builder(SessionActivity.this).create();
        dialog.setMessage(msg);
        dialog.setButton(AlertDialog.BUTTON_NEUTRAL,
                buttonText,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                });
        dialog.show();
    }

    private String getFilename(Intent intent) {
        return new StringBuilder(String.valueOf(new SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date()))).append("_")
                .append(intent.getStringExtra(LoginActivity.SUBJECT_ID))
                .append(".csv")
                .toString();
    }

    public void writeLineToOutput(String str) {
        Log.i(TAG, "Writing to output: " + str);
        try {
            this.fw.write(new StringBuilder(String.valueOf(str)).append("\n")
                    .toString());
        } catch (IOException e) {
            displayDialogAndExit("Error writing \"" + str + "\" to output file",
                    "Okay");
            e.printStackTrace();
        }
    }

    private void writeOutputHeader(Long observationTime) {
        this.timeOffset = observationTime.longValue();
        writeLineToOutput("DATE/TIME: " + new SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date()));
        writeLineToOutput("");
        //writeLineToOutput("Elapsed time (ms), Location, Observation");
        String columnLabels = new String("Elapsed time (ms)");
        for (String s : buttonGroups.keySet()) {
            columnLabels += "," + s;
        }
        writeLineToOutput(columnLabels);
    }


    public void recordChange(RadioButtonGroup group) {
        if (!isRunning) return;

        /*
        Long observationTime = Long.valueOf(SystemClock.elapsedRealtime());
        if (this.timeOffset < 0) {
            writeOutputHeader(observationTime);
        }
        for(RadioButtonGroup g : buttonGroups.values()) {
            // TODO remove this, replace with a start button.
            if (!g.isChecked()) {
                //Do not record an observation if a radio button group does
                not have a selectio
                return;
            }
        }*/

        String row = new String(new StringBuilder(String.valueOf(
                getRecordingTime())));
        for (RadioButtonGroup g : buttonGroups.values()) {
            Log.w(TAG, "DEBUG GROUP NAME: " + g.groupName);
            View view = g.getCheckedRadioButton();
            row += ",";
            if (view != null) {
                row += g.getCheckedRadioButton().toString();
            } else {
                row += "N/A";
            }
        }
        Log.w(TAG, "DEBUG row " + row);
        writeLineToOutput(row);
        // writeLineToOutput(append(radioButtonGroupName).append(",").append
        // (selectedOption).toString());
/*
        if (this.timeOffset < 0) {
            //for the first observation, record everything
            for(RadioButtonGroup g : buttonGroups.values()) {
                recordObservation(observationTime, g.groupName, g
                .getCheckedRadioButton().toString());
            }
        } else {
            recordObservation(observationTime, group.groupName, group
            .getCheckedRadioButton().toString());
        }*/
    }

    /*private void recordObservation(Long observationTime, String
    radioButtonGroupName, String selectedOption) {
        if (this.timeOffset < 0) {
            writeOutputHeader(observationTime);
        }
        writeLineToOutput(new StringBuilder(String.valueOf(observationTime
        .longValue() - this.timeOffset)).append(",").append
        (radioButtonGroupName).append(",").append(selectedOption).toString());
    }*/

    private void endSession() {
        timer.cancel();
        try {
            writeLineToOutput(new StringBuilder(String.valueOf(getRecordingTime()))
                    .append(", END")
                    .toString());
            this.fw.close();
            AlertDialog alertDialog =
                    new AlertDialog.Builder(SessionActivity.this)
                    .create();
            alertDialog.setTitle("Success");
            alertDialog.setMessage("Session output saved successfully!");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL,
                    "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            startActivity(new Intent(SessionActivity.this,
                                    LoginActivity.class));
                        }
                    });
            alertDialog.show();
        } catch (IOException e) {
            Log.e(TAG, "Error closing file");
            e.printStackTrace();
            AlertDialog alertDialog =
                    new AlertDialog.Builder(SessionActivity.this)
                    .create();
            alertDialog.setTitle("Error");
            alertDialog.setMessage("Error closing session output :(");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL,
                    "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            startActivity(new Intent(SessionActivity.this,
                                    LoginActivity.class));
                        }
                    });
            alertDialog.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_session, menu);
        final Menu menuInTimer = menu;
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Has recording started?
                        if (isRunning) {
                            Long time = getRecordingTime();
                            Long hours = TimeUnit.MILLISECONDS.toHours(time);
                            Long minutes =
                                    TimeUnit.MILLISECONDS.toMinutes(time);
                            Long seconds =
                                    TimeUnit.MILLISECONDS.toSeconds(time);
                            String timeText = String.format("%02d:%02d" +
                                            ":%02d",
                                    hours,
                                    minutes - TimeUnit.HOURS.toMinutes(hours),
                                    seconds - TimeUnit.MINUTES.toSeconds(minutes));
                            MenuItem timerText =
                                    menuInTimer.findItem(R.id.timer_text);
                            String msg = "TIME: " + timeText;
                            Log.i(TAG, msg);
                            timerText.setTitle(msg);
                        }
                    }
                });
            }
        };
        timer.schedule(timerTask, 0, 500);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_record) {

            // https://stackoverflow.com/questions/9738239/android-accessing
            // -ui-element-from-timer-thread

            if (isRunning) {
                pauseTime = Long.valueOf(SystemClock.elapsedRealtime())
                        .longValue();
                recordChange(null);
                // We write a line even if pausing. The user could pause,
                // wait 5 minutes, then hit END,
                // and the END row will have a timestamp 5 minutes after
                // PAUSE/RESUME was pressed.
                // It may be useful to know the time at which the pause
                // happened.
                item.setTitle("RESUME");
            } else {
                if (timeOffset == -1) {
                    for (RadioButtonGroup g : buttonGroups.values()) {
                        View view = g.getCheckedRadioButton();
                        if (view == null) {
                            AlertDialog alertDialog = new AlertDialog.Builder(
                                    this).create();
                            alertDialog.setTitle("Alert");
                            alertDialog.setMessage(
                                    "You must select a button for " + g.groupName);
                            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL,
                                    "OK",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                            alertDialog.show();
                            return true;
                        }
                    }
                    writeOutputHeader(Long.valueOf(SystemClock.elapsedRealtime())
                            .longValue());
                } else {
                    timeOffset =
                            timeOffset + (Long.valueOf(SystemClock.elapsedRealtime())
                            .longValue() - pauseTime);
                }
                item.setTitle("PAUSE");
            }
            isRunning = !isRunning;
            recordChange(null);
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_end) {
            endSession();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class SessionFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_TAB_NUMBER = "tab_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static SessionFragment newInstance(int sectionNumber) {
            SessionFragment fragment = new SessionFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_TAB_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container
                , Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_session,
                    container,
                    false);

            final SessionActivity sessionActivity =
                    (SessionActivity) getActivity();
            TabData tabData = sessionActivity.mLayoutData.tabDatas.get(
                    getArguments().getInt(ARG_TAB_NUMBER));

            LinearLayout grid = (LinearLayout) rootView.findViewById(R.id.grid);
            int numColumns = tabData.columnDatas.size();
            for (int colIndx = 0; colIndx < numColumns; colIndx++) {
                ColumnData colData = tabData.columnDatas.get(colIndx);
                LinearLayout col = new LinearLayout(getActivity());
                col.setOrientation(LinearLayout.VERTICAL);
                col.setLayoutParams(new LayoutParams(-1, -1, 1.0f));
                TextView text = new TextView(getActivity());

                Log.i(TAG, "COL DATA LABEL: " + colData.label);
                text.setText(colData.label);
                text.setTextSize(17.0f);
                col.addView(text);
                Iterator<OptionData> it = colData.optionDatas.iterator();
                while (it.hasNext()) {
                    OptionData od = it.next();
                    RadioButtonX rb = addNewRadioButton(sessionActivity,
                            od,
                            sessionActivity.buttonGroups);
                    rb.setChecked(od.checked);
//                    rb.getLayoutParams().height = buttonHeight;
                    if (!(od == null || od.group == null || od.group.equals(
                            "") || !od.group
                            .equalsIgnoreCase("end"))) {
                        rb.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                sessionActivity.endSession();
                            }
                        });
                    }
                    col.addView(rb);
                }
                grid.addView(col);
            }
            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private LayoutData mLayoutData;

        public SectionsPagerAdapter(FragmentManager fm, LayoutData layoutData) {
            super(fm);
            mLayoutData = layoutData;
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a SessionFragment (defined as a static inner class below).
            return SessionFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return mLayoutData.tabDatas.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mLayoutData.tabDatas.get(position).label;
        }
    }
}

// TODO: Add button to remove config files.