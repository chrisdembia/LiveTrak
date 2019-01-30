package stanford.edu.livetrak2;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.media.MediaCas;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;


public class LoginActivity extends AppCompatActivity {
    private static String TAG = "LoginActivity";
    public static final String CODER_ID = "edu.stanford.livetraq.CODER_ID";
    // public static final String CONFIG_FILE = "edu.stanford.livetraq.CONFIG_FILE";
    // public static final String LANGUAGE = "edu.stanford.livetraq.LANGUAGE";
    public static final String SUBJECT_ID = "edu.stanford.livetraq.SUBJECT_ID";
    public static final String CONFIG_ID = "edu.stanford.livetraq.CONFIG_ID";
    private static final int GET_CONTENT_CONFIG_FILE_CODE = 1;
    private Spinner configId = null;
    private ArrayAdapter<String> configSpinnerAdapter = null;
    public static File configDir = null;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isExternalStorageWritable()) {
            new Builder(this).setMessage("App cannot run because external storage is not available. Please insert an SD card.").setNeutralButton("Ok", new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    System.exit(0);
                }
            }).show();
        }

        setContentView(R.layout.activity_login);

        File root = Environment.getExternalStorageDirectory();
        configDir = new File(root, SessionActivity.APP_STORAGE_DIR + "config");
        if (!configDir.exists()) configDir.mkdirs();

        configId = (Spinner) findViewById(R.id.configId);
        List<String> spinnerArray = new ArrayList<>();
        spinnerArray.add(SessionActivity.DEFAULT_CONFIG_FILE);
        File[] configFiles = configDir.listFiles();
        for (File file : configFiles) {
            if (!file.isDirectory()) {
                spinnerArray.add(file.getName());
            }
        }
        // Create an ArrayAdapter using the string array and a default spinner layout
        configSpinnerAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, spinnerArray);
        // Specify the layout to use when the list of choices appears
        configSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        configId.setAdapter(configSpinnerAdapter);

        // Allow user to add config files.
        // https://developer.android.com/guide/topics/providers/document-provider#java
        final Button button = findViewById(R.id.addConfigFile);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intentOpenConfig = new Intent(Intent.ACTION_GET_CONTENT);
                intentOpenConfig.addCategory(Intent.CATEGORY_OPENABLE);
                intentOpenConfig.setType("text/comma-separated-values");
                startActivityForResult(intentOpenConfig, GET_CONTENT_CONFIG_FILE_CODE);
            }
        });
    }

    public boolean isExternalStorageWritable() {
        if ("mounted".equals(Environment.getExternalStorageState())) {
            return true;
        }
        return false;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    private boolean isEmpty(EditText field) {
        return field.getText().toString().trim().isEmpty();
    }

    protected void onActivityResult (int requestCode,
                                     int resultCode,
                                     Intent data) {
        if (requestCode == GET_CONTENT_CONFIG_FILE_CODE) {
            if(resultCode == Activity.RESULT_OK){
                String result = data.getStringExtra("result");
                Uri uri = null;
                uri = data.getData();
                if (uri != null) {
                    Log.i(TAG, "Uri: " + uri.toString());
                    String uriString = uri.toString();
                    String lastPathSegment = uri.getLastPathSegment();
                    String localFilename = lastPathSegment.substring(
                            lastPathSegment.lastIndexOf(":") + 1);
                    try {
                        saveFile((FileInputStream)getContentResolver().openInputStream(uri),
                                (new File(configDir, localFilename)).toString());
                        configSpinnerAdapter.add(localFilename);
                        configSpinnerAdapter.notifyDataSetChanged();
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
    private void saveFile(FileInputStream source, String destination) throws IOException
    {
        /*
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;

        try {
            bis = new BufferedInputStream(source);
            bos = new BufferedOutputStream(new FileOutputStream(destination, false));
            byte[] buf = new byte[1024];
            while(bis.read(buf) != -1) {
                bos.write(buf);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bis != null) bis.close();
                if (bos != null) bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        */

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

    public void newSession(View v) {
        EditText coderId = (EditText) findViewById(R.id.coderId);
        if (isEmpty(coderId)) {
            coderId.setError("Please enter your ID");
            return;
        }
        EditText subjectId = (EditText) findViewById(R.id.subjectId);
        if (isEmpty(subjectId)) {
            subjectId.setError("Please enter the subject ID");
            return;
        }
        Spinner configId = (Spinner) findViewById(R.id.configId);


        Intent intent = new Intent(this, SessionActivity.class);
        intent.putExtra(CODER_ID, coderId.getText().toString().trim());
        intent.putExtra(SUBJECT_ID, subjectId.getText().toString().trim());
        intent.putExtra(CONFIG_ID, configId.getSelectedItem().toString().trim());
        startActivity(intent);
    }
}

// TODO rename configId to configFile.
// TODO make the rest of the code actually USE the specified config file.
// TODO allow users to easily upload a config file.
// https://stackoverflow.com/questions/2034892/how-do-i-allow-a-user-to-browse-choose-a-file-for-my-app-use-in-android