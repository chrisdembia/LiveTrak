package com.example.livetrak3;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private static String TAG = "LoginActivity";
    public static final String CODER_ID = "edu.stanford.livetrak.CODER_ID";
    public static final String SUBJECT_ID = "edu.stanford.livetrak.SUBJECT_ID";
    public static final String CONFIG_ID = "edu.stanford.livetrak.CONFIG_ID";
    public static final String APP_STORAGE_DIR = "LiveTrak/";
    public static final String DEFAULT_CONFIG_FILE = "LiveTrak_soil_v1.csv";
    public static final int LIVETRAK_REQUEST_WRITE_EXTERNAL_STORAGE = 2;
    private static final int GET_CONTENT_CONFIG_FILE_CODE = 1;
    public static File configDir = null;
    private EditText mCoderId;
    private EditText mSubjectId;
    private Spinner mConfigId;
    private boolean mSpinnerIsPopulated = false;
    private ArrayAdapter<String> mConfigSpinnerAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mCoderId = (EditText) findViewById(R.id.coder_id);
        mSubjectId = (EditText) findViewById(R.id.subject_id);

        if (!isExternalStorageWritable()) {
            new Builder(this).setMessage(
                    "App cannot run because external storage is not " +
                            "available" + ". Please insert an SD card.")
                    .setNeutralButton("Ok", new OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    })
                    .show();
        }

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            }

            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    LIVETRAK_REQUEST_WRITE_EXTERNAL_STORAGE);
        } else {
            populateConfigIDSpinner();
        }


        // Allow user to add config files.
        // https://developer.android
        // .com/guide/topics/providers/document-provider#java
        final Button button = findViewById(R.id.add_config_file);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intentOpenConfig = new Intent(Intent.ACTION_GET_CONTENT);
                intentOpenConfig.addCategory(Intent.CATEGORY_OPENABLE);
                intentOpenConfig.setType("text/comma-separated-values");
                startActivityForResult(intentOpenConfig,
                        GET_CONTENT_CONFIG_FILE_CODE);
            }
        });
    }

    private void populateConfigIDSpinner() {
        if (mSpinnerIsPopulated) return;
        mSpinnerIsPopulated = true;
        File root = Environment.getExternalStorageDirectory();
        configDir = new File(root, APP_STORAGE_DIR + "config");
        if (!configDir.exists()) configDir.mkdirs();

        mConfigId = (Spinner) findViewById(R.id.config_id);
        List<String> spinnerArray = new ArrayList<>();
        spinnerArray.add(DEFAULT_CONFIG_FILE);
        File[] configFiles = configDir.listFiles();
        for (File file : configFiles) {
            if (!file.isDirectory()) {
                spinnerArray.add(file.getName());
            }
        }
        // Create an ArrayAdapter using the string array and a default
        // spinner layout
        mConfigSpinnerAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item,
                spinnerArray);
        // Specify the layout to use when the list of choices appears
        mConfigSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mConfigId.setAdapter(mConfigSpinnerAdapter);
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case LIVETRAK_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    populateConfigIDSpinner();
                } else {
                    AlertDialog alertDialog = new AlertDialog.Builder(
                            LoginActivity.this).create();
                    alertDialog.setTitle("Error");
                    alertDialog.setMessage(
                            "LiveTrak must read/write files. Select ALLOW in "
                                    + "the upcoming dialog.");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL,
                            "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    dialog.dismiss();
                                    startActivity(new Intent(LoginActivity.this,
                                            LoginActivity.class));
                                }
                            });
                    alertDialog.show();
                }
                return;
            }
        }
    }

    public boolean isExternalStorageWritable() {
        if ("mounted".equals(Environment.getExternalStorageState())) {
            return true;
        }
        return false;
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GET_CONTENT_CONFIG_FILE_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                String result = data.getStringExtra("result");
                Uri uri = null;
                uri = data.getData();
                if (uri != null) {
                    Log.i(TAG, "Uri: " + uri.toString());
                    String uriString = uri.toString();
                    String lastPathSegment = uri.getLastPathSegment();
                    String localFilename = lastPathSegment.substring(lastPathSegment.lastIndexOf(":") + 1);
                    try {
                        saveFile((FileInputStream) getContentResolver().openInputStream(uri),
                                (new File(configDir, localFilename)).toString());
                        boolean containsElement = false;
                        for (int i = 0; i < mConfigSpinnerAdapter.getCount(); ++i) {
                            if (mConfigSpinnerAdapter.getItem(i).equals(localFilename)) {
                                containsElement = true;
                                break;
                            }
                        }
                        if (!containsElement) {
                            mConfigSpinnerAdapter.add(localFilename);
                            mConfigSpinnerAdapter.notifyDataSetChanged();
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                // Nothing to do.
            }
        }

    }

    // https://stackoverflow.com/questions/13133579/android-save-a-file-from-an-existing-uri
    private void saveFile(FileInputStream source, String destination) throws IOException {

        FileChannel outputChannel = null;
        FileChannel inputChannel = null;
        try {
            outputChannel = new FileOutputStream(destination).getChannel();
            inputChannel = source.getChannel();
            inputChannel.transferTo(0, inputChannel.size(), outputChannel);
            inputChannel.close();
        } finally {
            if (inputChannel != null) inputChannel.close();
            if (outputChannel != null) outputChannel.close();
        }
    }

    private boolean isEmpty(EditText field) {
        return field.getText().toString().trim().isEmpty();
    }

    public void newSession(View view) {
        if (isEmpty(mCoderId)) {
            mCoderId.setError("Please enter your ID");
            return;
        }
        if (isEmpty(mSubjectId)) {
            mSubjectId.setError("Please enter the subject ID");
            return;
        }
        Spinner configId = (Spinner) findViewById(R.id.config_id);

        Intent intent = new Intent(this, SessionActivity.class);
        intent.putExtra(CODER_ID, mCoderId.getText().toString().trim());
        intent.putExtra(SUBJECT_ID, mSubjectId.getText().toString().trim());
        intent.putExtra(CONFIG_ID, configId.getSelectedItem().toString().trim());
        startActivity(intent);
    }

}

