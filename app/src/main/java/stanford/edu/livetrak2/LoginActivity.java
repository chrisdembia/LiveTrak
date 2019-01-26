package stanford.edu.livetrak2;

import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.media.MediaCas;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class LoginActivity extends AppCompatActivity {
    public static final String CODER_ID = "edu.stanford.livetraq.CODER_ID";
    // public static final String CONFIG_FILE = "edu.stanford.livetraq.CONFIG_FILE";
    // public static final String LANGUAGE = "edu.stanford.livetraq.LANGUAGE";
    public static final String SUBJECT_ID = "edu.stanford.livetraq.SUBJECT_ID";
    public static final String CONFIG_ID = "edu.stanford.livetraq.CONFIG_ID";

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
        File configDir = new File(root, SessionActivity.APP_STORAGE_DIR + "config");
        if (!configDir.exists()) configDir.mkdirs();

        Spinner configId = (Spinner) findViewById(R.id.configId);
        List<String> spinnerArray = new ArrayList<>();
        spinnerArray.add(SessionActivity.DEFAULT_CONFIG_FILE);
        File[] configFiles = configDir.listFiles();
        for (File file : configFiles) {
            if (!file.isDirectory()) {
                spinnerArray.add(file.getAbsolutePath().toString());
            }
        }
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, spinnerArray);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        configId.setAdapter(adapter);

        /*
        https://developer.android.com/guide/topics/providers/document-provider#java
        int READ_REQUEST_CODE = 42;

        Intent intentOpenConfig = new Intent(Intent.ACTION_GET_CONTENT);

        intentOpenConfig.addCategory(Intent.CATEGORY_OPENABLE);

        intentOpenConfig.setType("text/comma-separated-values");

        startActivityForResult(intentOpenConfig, READ_REQUEST_CODE);
        */
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
        // if (isEmpty(configId)) {
            // configId.setError("Please select a config file " +
            //         "(or add one to <Internal Storage>/LiveTrak/config via the Google Files app.");
        //     return;
        // }


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