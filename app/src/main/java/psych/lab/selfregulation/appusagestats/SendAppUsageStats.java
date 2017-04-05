package psych.lab.selfregulation.appusagestats;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by 14leec1 on 4/1/2017.
 *
 *
 * NOTICE: The App Currently ONLY sends and updates data AFTER the app itself is closed and not on the foreground. The moment it is created, it will set the context as "this"
 *
 * POTENTIAL CONCERN: There may be concerns that the app will not retrieve data of the app foreground times PRIOR to the app being installed. This is not true. As noted in the test cases, the quickgooglesearchbox has more foreground time than the app usage stats app, despite it being used less than the app after it was installed. This shows how the app records most of the information.
 */

public class SendAppUsageStats extends Activity {

    public class Statistics {//Data Type that will be used to update and collect foreground times

        private String appname;
        private long appforegroundtime;
        private long foregroundtime;

        public Statistics(){//Default Constructor

            appname = "";
            appforegroundtime = 0;
            foregroundtime = 0;

        }

        public String getAppName() {return appname;}
        public long getTime() {return appforegroundtime;}
        public long getFTime() {return foregroundtime;}
        public void setAppName(String name) {this.appname = name;}
        public void setFTime(long time) {this.foregroundtime = time;}
        public void setTime(long time) {this.appforegroundtime = time;}

    }


    //RULE FOR JAVA: ALWAYS INITIALIZE CODE INSIDE ONCREATE AND ONLY DECLARE VARIABLES (GLOBALLY) WITHOUT ASSIGNING A VALUE
    private Context context;
    final String myTag = "DocsUpload";
    String name;
    String group_tag; //For notifications targeted at multiple groups
    ArrayList<String> stats; //Must set stats as ArrayList<String> to match output of function involving foreground time
    List<UsageStats> TempHolder;
    ArrayList<Statistics> UsageStatsStats;
    EditText input_name;
    EditText input_group;
    Button stats_button;
    ToggleButton toggle;
    FirebaseIDService TokenSender; //Class object that calls upon FirebaseIDService class
    String groupTags[]; //Possible tags for topics (FCM Messenger)

    //Variables Necessary for OkHttp3
    public static final String URL="https://docs.google.com/forms/d/e/1FAIpQLSe0kgqCF-wdlBpdFbGt0MXq_NJWH1pwakglCH9q-JCmvCwlOA/formResponse"; //URL For Response Form (MODIFIABLE)
    public static final MediaType FORM_DATA_TYPE
            = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.send_usage_stats);
        Log.i(myTag, "OnCreate()");

        UsageStatsStats = new ArrayList<Statistics>();

        input_name = (EditText) findViewById(R.id.input_name);
        input_group = (EditText) findViewById(R.id.input_group);

        stats_button = (Button) findViewById(R.id.send_data);
        toggle = (ToggleButton) findViewById(R.id.save_input);
        groupTags = new String[]{getString(R.string.topic_control),getString(R.string.topic_exp_1),getString(R.string.topic_exp_2)};

        context = this;
        TokenSender = new FirebaseIDService();

        SharedPreferences sharedPrefs = getSharedPreferences("psychapp", MODE_PRIVATE);
        boolean ToggleChecked = sharedPrefs.getBoolean("setText", false);//Boolean that is retrieved based on existing saved toggle

        if (ToggleChecked){

            toggle.setChecked(ToggleChecked);
            input_name.setText(sharedPrefs.getString("setText_St1", ""));
            input_group.setText(sharedPrefs.getString("setText_St2", ""));

        }

        //TODO: Find a way to update the context such that it will set the context as "this" the moment the user hits send

        stats_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                SharedPreferences appsSharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()); //Gets the existing stats that have been saved if available.
                Gson gson = new Gson();
                Type type = new TypeToken<List<Statistics>>(){}.getType();
                String json = appsSharedPref.getString("key", null);

                if (json != null) {//Tests whether something has been saved beforehand
                    UsageStatsStats = gson.fromJson(json, type);
                }

                TempHolder = UsageStatistics.getUsageStatsList(context);

                //The following variables test whether or not permission has been granted for the app to check statistics
                AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);

                int mode = appOps.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), context.getPackageName().toString());

                boolean granted = mode == AppOpsManager.MODE_ALLOWED;

                if (!granted) {

                    //TEST 1: Initiate the test, such that you can determine if permission has been access

                    new AlertDialog.Builder(SendAppUsageStats.this).setTitle(getString(R.string.permissions_title)).setMessage(getString(R.string.get_permissions)).setPositiveButton(getString(R.string.dialog_continue), new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface dialog, int which) {
                            // Proceed to settings page
                            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                            startActivity(intent);
                        }

                    }).setNegativeButton(getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {

                            //Cancel dialog message and return to activity

                            dialog.cancel();

                        }

                    }).show();

                } else {

                    name = input_name.getText().toString();
                    group_tag = input_group.getText().toString();

                    //TEST 2: Tests whether the UsageStatsStats is Empty (i.e. on initial input)
                    if (UsageStatsStats == null || UsageStatsStats.isEmpty()) {//If Empty, will add everything from TempHolder to the UsageStatsStats
                        for (int i = 0; i < TempHolder.size(); i++){

                            Statistics Object = new Statistics();

                            Object.setAppName(TempHolder.get(i).getPackageName().toString());
                            Object.setTime(TempHolder.get(i).getTotalTimeInForeground());
                            Object.setFTime(TempHolder.get(i).getTotalTimeInForeground());

                            UsageStatsStats.add(Object);

                        }//TODO: Save UsageStatsStats so that it can be retrievable on first creation

                    } /* else {//If Not Empty, will first check whether there is an updated foregroundtime in TempHolder and add to UsageStatsStats

                        for (int i = 0; i < UsageStatsStats.size(); i++){

                            for (int j = 0; j < TempHolder.size(); j++){

                                if (UsageStatsStats.get(i).getAppName() == TempHolder.get(j).getPackageName().toString()){ //Tests whether name matches and if the time in the list is the same as the total foreground time

                                    if (UsageStatsStats.get(i).getFTime() != TempHolder.get(j).getTotalTimeInForeground()) {

                                        UsageStatsStats.get(i).setTime(UsageStatsStats.get(i).getTime() + TempHolder.get(j).getTotalTimeInForeground()); //Adds the new foreground time with the previously saved foreground time in the app
                                        UsageStatsStats.get(i).setFTime(TempHolder.get(j).getTotalTimeInForeground());
                                        TempHolder.remove(TempHolder.get(j));
                                        break;

                                    }

                                }

                            }

                        }

                        for (int l = 0; l < TempHolder.size(); l++) {//Adds all other ForegroundTimes to UsageStatsStats

                            boolean no_repeat = true;
                            for (int k = 0; k < UsageStatsStats.size(); k++){

                                if (TempHolder.get(l).getPackageName().toString() == UsageStatsStats.get(k).getAppName()){
                                    no_repeat = false;
                                    break;

                                }

                            }

                            if (no_repeat) {

                                Statistics Object = new Statistics();

                                Object.setTime(TempHolder.get(l).getTotalTimeInForeground());
                                Object.setAppName(TempHolder.get(l).getPackageName().toString());

                                UsageStatsStats.add(Object);

                            }

                        }

                    } */

                    SharedPreferences.Editor prefsEditor = appsSharedPref.edit(); //Saves the input from previous loops
                    json = gson.toJson(UsageStatsStats);
                    prefsEditor.putString("key", json);
                    prefsEditor.commit();

                    stats = UsageStatistics.AppTimeUsageStats(UsageStatistics.getUsageStatsList(context)); //Overloaded function that will retrieve saved UsageStatsList
                    StringBuilder stats_to_string = new StringBuilder();
                    for (String s: stats){

                        stats_to_string.append(s);

                    }

                    //TEST 3: Check if input is empty

                    if (name.isEmpty() || (stats_to_string.toString().isEmpty()) || group_tag.isEmpty()) {

                        Toast.makeText(context, getString(R.string.mandatory_fill), Toast.LENGTH_LONG).show();

                    } else {

                        //TEST 4: Check if GroupTag is valid

                        boolean valid_group = false;
                        if ((group_tag.equalsIgnoreCase(getString(R.string.topic_control)) || group_tag.equalsIgnoreCase(getString(R.string.topic_exp_1))) || group_tag.equalsIgnoreCase(getString(R.string.topic_exp_2))){

                            valid_group = true;

                        }

                        if (valid_group == false) {

                            Toast.makeText(context, getString(R.string.wrong_string), Toast.LENGTH_LONG).show();

                        } else {

                            FirebaseMessaging.getInstance().subscribeToTopic(group_tag); //Sets subscription only if valid_group is true

                            String stats_final = stats_to_string.toString();

                            PostDataTask postDataTask = new PostDataTask();

                            postDataTask.execute(URL, name, group_tag, stats_final);//Change username_tag to group_tag

                        }

                    }

                }

            }

        });

        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {

                    // The toggle is enabled
                    SharedPreferences.Editor editor = getSharedPreferences("psychapp", MODE_PRIVATE).edit();
                    editor.putBoolean("setText", true);
                    editor.putString("setText_St1", name);
                    editor.putString("setText_St2", group_tag);
                    editor.commit();


                } else {

                    // The toggle is disabled

                    SharedPreferences.Editor editor = getSharedPreferences("psychapp", MODE_PRIVATE).edit();
                    editor.putBoolean("setText", false);
                    editor.commit();


                }

            }
        });

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.

        if (toggle.isChecked()) {
            savedInstanceState.putBoolean("setText", true);
            savedInstanceState.putString("setText_St1", name);
            savedInstanceState.putString("setText_St2", group_tag);
            // etc.
        }



    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.
        input_name.setText(savedInstanceState.getString("setText_St1"));
        input_group.setText(savedInstanceState.getString("setText_St2"));
        toggle.setChecked(savedInstanceState.getBoolean("setText", false));
    }


    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first

        if (toggle.isChecked()) {
            SharedPreferences.Editor editor = getSharedPreferences("psychapp", MODE_PRIVATE).edit();
            editor.putBoolean("setText", true);
            editor.putString("setText_St1", name);
            editor.putString("setText_St2", group_tag);
            editor.commit();
        } else {

            // The toggle is disabled

            SharedPreferences.Editor editor = getSharedPreferences("psychapp", MODE_PRIVATE).edit();
            editor.putBoolean("setText", false);
            editor.commit();

        }

        SharedPreferences appsSharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor prefsEditor = appsSharedPref.edit(); //Saves the input from previous loops
        Gson gson = new Gson();
        String json = gson.toJson(UsageStatsStats);
        prefsEditor.putString("key", json);
        prefsEditor.commit();

    }

    @Override
    protected void onStop() {
        // call the superclass method first
        super.onStop();

        if (toggle.isChecked()) {
            SharedPreferences.Editor editor = getSharedPreferences("psychapp", MODE_PRIVATE).edit();
            editor.putBoolean("setText", true);
            editor.putString("setText_St1", name);
            editor.putString("setText_St2", group_tag);
            editor.commit();
        } else {

            // The toggle is disabled

            SharedPreferences.Editor editor = getSharedPreferences("psychapp", MODE_PRIVATE).edit();
            editor.putBoolean("setText", false);
            editor.commit();

        }

        SharedPreferences appsSharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor prefsEditor = appsSharedPref.edit(); //Saves the input from previous loops
        Gson gson = new Gson();
        String json = gson.toJson(UsageStatsStats);
        prefsEditor.putString("key", json);
        prefsEditor.commit();
    }

    @Override
    public void onResume(){
        super.onResume();

        context = this;
        TokenSender = new FirebaseIDService();

        SharedPreferences sharedPrefs = getSharedPreferences("psychapp", MODE_PRIVATE);
        boolean ToggleChecked = sharedPrefs.getBoolean("setText", true);//Boolean that is retrieved based on existing saved toggle

        if (ToggleChecked){

            toggle.setChecked(ToggleChecked);
            input_name.setText(sharedPrefs.getString("setText_St1", ""));
            input_group.setText(sharedPrefs.getString("setText_St2", ""));

        }


        //TODO: Retrieve Toggled information

        stats_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                SharedPreferences appsSharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()); //Gets the existing stats that have been saved if available.
                Gson gson = new Gson();
                Type type = new TypeToken<List<Statistics>>(){}.getType();
                String json = appsSharedPref.getString("key", null);

                if (json != null) {//Tests whether something has been saved beforehand
                    UsageStatsStats = gson.fromJson(json, type);
                }

                TempHolder = UsageStatistics.getUsageStatsList(context);

                //The following variables test whether or not permission has been granted for the app to check statistics
                AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);

                int mode = appOps.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), context.getPackageName().toString());

                boolean granted = mode == AppOpsManager.MODE_ALLOWED;

                if (!granted) {

                    //TEST 1: Initiate the test, such that you can determine if permission has been access

                    new AlertDialog.Builder(SendAppUsageStats.this).setTitle(getString(R.string.permissions_title)).setMessage(getString(R.string.get_permissions)).setPositiveButton(getString(R.string.dialog_continue), new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface dialog, int which) {
                            // Proceed to settings page
                            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                            startActivity(intent);
                        }

                    }).setNegativeButton(getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {

                            //Cancel dialog message and return to activity

                            dialog.cancel();

                        }

                    }).show();

                } else {

                    name = input_name.getText().toString();
                    group_tag = input_group.getText().toString();

                    //TEST 2: Tests whether the UsageStatsStats is Empty (i.e. on initial input)
                    if (UsageStatsStats == null || UsageStatsStats.isEmpty()) {//If Empty, will add everything from TempHolder to the UsageStatsStats
                        for (int i = 0; i < TempHolder.size(); i++){

                            Statistics Object = new Statistics();

                            Object.setAppName(TempHolder.get(i).getPackageName().toString());
                            Object.setTime(TempHolder.get(i).getTotalTimeInForeground());

                            UsageStatsStats.add(Object);

                        }//TODO: Save UsageStatsStats so that it can be retrievable on first creation

                    } /*else {//If Not Empty, will first check whether there is an updated foregroundtime in TempHolder and add to UsageStatsStats

                        for (int i = 0; i < UsageStatsStats.size(); i++){

                            for (int j = 0; j < TempHolder.size(); j++){

                                if (UsageStatsStats.get(i).getAppName() == TempHolder.get(j).getPackageName().toString()){ //Tests whether name matches and if the time in the list is the same as the total foreground time

                                    if (UsageStatsStats.get(i).getFTime() != TempHolder.get(j).getTotalTimeInForeground()) {

                                        UsageStatsStats.get(i).setTime(UsageStatsStats.get(i).getTime() + TempHolder.get(j).getTotalTimeInForeground()); //Adds the new foreground time with the previously saved foreground time in the app
                                        UsageStatsStats.get(i).setFTime(TempHolder.get(j).getTotalTimeInForeground());
                                        TempHolder.remove(TempHolder.get(j));;
                                        break;

                                    }

                                }

                            }

                        }//TODO: Update Context for OnResume and OnPause and OnStop

                        for (int l = 0; l < TempHolder.size(); l++) {//Adds all other ForegroundTimes to UsageStatsStats

                            boolean no_repeat = true;
                            for (int k = 0; k < UsageStatsStats.size(); k++){

                                if (TempHolder.get(l).getPackageName().toString() == UsageStatsStats.get(k).getAppName()){
                                    no_repeat = false;
                                    break;

                                }

                            }

                            if (no_repeat) {

                                Statistics Object = new Statistics();

                                Object.setTime(TempHolder.get(l).getTotalTimeInForeground());
                                Object.setAppName(TempHolder.get(l).getPackageName().toString());

                                UsageStatsStats.add(Object);

                            }

                        }

                    } */



                    stats = UsageStatistics.AppTimeUsageStats(UsageStatistics.getUsageStatsList(context)); //Overloaded function that will retrieve saved UsageStatsList
                    StringBuilder stats_to_string = new StringBuilder();
                    for (String s: stats){

                        stats_to_string.append(s);

                    }

                    SharedPreferences.Editor prefsEditor = appsSharedPref.edit(); //Saves the input from previous loops
                    json = gson.toJson(UsageStatsStats);
                    prefsEditor.putString("key", json);
                    prefsEditor.commit();
                    //TEST 3: Check if input is empty

                    if (name.isEmpty() || (stats_to_string.toString().isEmpty()) || group_tag.isEmpty()) {

                        Toast.makeText(context, getString(R.string.mandatory_fill), Toast.LENGTH_LONG).show();

                    } else {

                        //TEST 4: Check if GroupTag is valid

                        boolean valid_group = false;
                        if ((group_tag.equalsIgnoreCase(getString(R.string.topic_control)) || group_tag.equalsIgnoreCase(getString(R.string.topic_exp_1))) || group_tag.equalsIgnoreCase(getString(R.string.topic_exp_2))){

                            valid_group = true;

                        }

                        if (valid_group == false) {

                            Toast.makeText(context, getString(R.string.wrong_string), Toast.LENGTH_LONG).show();

                        } else {

                            FirebaseMessaging.getInstance().subscribeToTopic(group_tag); //Sets subscription only if valid_group is true

                            String stats_final = stats_to_string.toString();

                            PostDataTask postDataTask = new PostDataTask();

                            postDataTask.execute(URL, name, group_tag, stats_final);//Change username_tag to group_tag

                        }

                    }

                }

            }

        });


        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {

                    // The toggle is enabled
                    SharedPreferences.Editor editor = getSharedPreferences("psychapp", MODE_PRIVATE).edit();
                    editor.putBoolean("setText", true);
                    editor.putString("setText_St1", name);
                    editor.putString("setText_St2", group_tag);
                    editor.commit();


                } else {

                    // The toggle is disabled

                    SharedPreferences.Editor editor = getSharedPreferences("psychapp", MODE_PRIVATE).edit();
                    editor.putBoolean("setText", false);
                    editor.commit();


                }

            }
        });


        //TODO: Retrieve Existing saved UsageStatsList

    }

    @Override
    public void onStart(){
        super.onStart();

        context = this;
        TokenSender = new FirebaseIDService();

        SharedPreferences sharedPrefs = getSharedPreferences("psychapp", MODE_PRIVATE);
        boolean ToggleChecked = sharedPrefs.getBoolean("setText", false);//Boolean that is retrieved based on existing saved toggle

        if (ToggleChecked){

            toggle.setChecked(ToggleChecked);
            input_name.setText(sharedPrefs.getString("setText_St1", ""));
            input_group.setText(sharedPrefs.getString("setText_St2", ""));

        }


        //TODO: Save UsageStatsList manually by pressing button
        stats_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                SharedPreferences appsSharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()); //Gets the existing stats that have been saved if available.
                Gson gson = new Gson();
                Type type = new TypeToken<List<Statistics>>(){}.getType();
                String json = appsSharedPref.getString("key", null);

                if (json != null) {//Tests whether something has been saved beforehand
                    UsageStatsStats = gson.fromJson(json, type);
                }

                TempHolder = UsageStatistics.getUsageStatsList(context);

                //The following variables test whether or not permission has been granted for the app to check statistics
                AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);

                int mode = appOps.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), context.getPackageName().toString());

                boolean granted = mode == AppOpsManager.MODE_ALLOWED;

                if (!granted) {

                    //TEST 1: Initiate the test, such that you can determine if permission has been access

                    new AlertDialog.Builder(SendAppUsageStats.this).setTitle(getString(R.string.permissions_title)).setMessage(getString(R.string.get_permissions)).setPositiveButton(getString(R.string.dialog_continue), new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface dialog, int which) {
                            // Proceed to settings page
                            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                            startActivity(intent);
                        }

                    }).setNegativeButton(getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {

                            //Cancel dialog message and return to activity

                            dialog.cancel();

                        }

                    }).show();

                } /*else {

                    name = input_name.getText().toString();
                    username_tag = TokenSender.TransferToken(); //Calls upon the function TransferToken, such that it can transfer the token string as username_tag
                    group_tag = input_group.getText().toString();

                    //TEST 2: Tests whether the UsageStatsStats is Empty (i.e. on initial input)
                    if (UsageStatsStats == null || UsageStatsStats.isEmpty()) {//If Empty, will add everything from TempHolder to the UsageStatsStats
                        for (int i = 0; i < TempHolder.size(); i++){

                            Statistics Object = new Statistics();

                            Object.setAppName(TempHolder.get(i).getPackageName().toString());
                            Object.setTime(TempHolder.get(i).getTotalTimeInForeground());

                            UsageStatsStats.add(Object);

                        }

                    } else {//If Not Empty, will first check whether there is an updated foregroundtime in TempHolder and add to UsageStatsStats

                        for (int i = 0; i < UsageStatsStats.size(); i++){

                            for (int j = 0; j < TempHolder.size(); j++){

                                if (UsageStatsStats.get(i).getAppName() == TempHolder.get(j).getPackageName().toString()){ //Tests whether name matches and if the time in the list is the same as the total foreground time

                                    if (UsageStatsStats.get(i).getFTime() != TempHolder.get(j).getTotalTimeInForeground()) {

                                        UsageStatsStats.get(i).setTime(UsageStatsStats.get(i).getTime() + TempHolder.get(j).getTotalTimeInForeground()); //Adds the new foreground time with the previously saved foreground time in the app
                                        UsageStatsStats.get(i).setFTime(TempHolder.get(j).getTotalTimeInForeground());
                                        TempHolder.remove(TempHolder.get(j));;
                                        break;

                                    }

                                }

                            }

                        }//TODO: Update Context for OnResume and OnPause and OnStop

                        for (int l = 0; l < TempHolder.size(); l++) {//Adds all other ForegroundTimes to UsageStatsStats

                            boolean no_repeat = true;
                            for (int k = 0; k < UsageStatsStats.size(); k++){

                                if (TempHolder.get(l).getPackageName().toString() == UsageStatsStats.get(k).getAppName()){
                                    no_repeat = false;
                                    break;

                                }

                            }

                            if (no_repeat) {

                                Statistics Object = new Statistics();

                                Object.setTime(TempHolder.get(l).getTotalTimeInForeground());
                                Object.setAppName(TempHolder.get(l).getPackageName().toString());

                                UsageStatsStats.add(Object);

                            }

                        }

                    } */

                    SharedPreferences.Editor prefsEditor = appsSharedPref.edit(); //Saves the input from previous loops
                    json = gson.toJson(UsageStatsStats);
                    prefsEditor.putString("key", json);
                    prefsEditor.commit();

                    stats = UsageStatistics.AppTimeUsageStats(UsageStatistics.getUsageStatsList(context)); //Overloaded function that will retrieve saved UsageStatsList
                    StringBuilder stats_to_string = new StringBuilder();
                    for (String s: stats){

                        stats_to_string.append(s);

                    }

                    //TEST 3: Check if input is empty

                    if (name.isEmpty() || (stats_to_string.toString().isEmpty()) || group_tag.isEmpty()) {

                        Toast.makeText(context, getString(R.string.mandatory_fill), Toast.LENGTH_LONG).show();

                    } else {

                        //TEST 4: Check if GroupTag is valid

                        boolean valid_group = false;
                        if ((group_tag.equalsIgnoreCase(getString(R.string.topic_control)) || group_tag.equalsIgnoreCase(getString(R.string.topic_exp_1))) || group_tag.equalsIgnoreCase(getString(R.string.topic_exp_2))){

                            valid_group = true;

                        }

                        if (valid_group == false) {

                            Toast.makeText(context, getString(R.string.wrong_string), Toast.LENGTH_LONG).show();

                        } else {

                            FirebaseMessaging.getInstance().subscribeToTopic(group_tag); //Sets subscription only if valid_group is true

                            String stats_final = stats_to_string.toString();

                            PostDataTask postDataTask = new PostDataTask();

                            postDataTask.execute(URL, name, group_tag, stats_final);//Change username_tag to group_tag

                        }

                    }

                }

            });

        //TODO: Retrieve Toggled Information
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {

                    // The toggle is enabled
                    SharedPreferences.Editor editor = getSharedPreferences("psychapp", MODE_PRIVATE).edit();
                    editor.putBoolean("setText", true);
                    editor.putString("setText_St1", name);
                    editor.putString("setText_St2", group_tag);
                    editor.commit();


                } else {

                    // The toggle is disabled

                    SharedPreferences.Editor editor = getSharedPreferences("psychapp", MODE_PRIVATE).edit();
                    editor.putBoolean("setText", false);
                    editor.commit();


                }

            }
        });


    }




                //TODO: check if data actually sends to designated Google Form and whether it is properly formatted *IF* strings exist

                //AsyncTask to send data as a http POST request
                private class PostDataTask extends AsyncTask<String, Void, Boolean> {

                    @Override
                    protected Boolean doInBackground (String... contactData) {
                        Boolean result = true;
                        String url = contactData[0];
                        String name_input = contactData[1];
                        String tag_input = contactData[2];
                        String stats_input = contactData[3];
                        String data = "";

                        try {

                            //all values must be URL encoded to make sure that special characters like & | ",etc.

                            data  = "entry_1190097673" + "=" + URLEncoder.encode(name_input,"UTF-8") + //Must change the "." in the entry points to "_"
                                    "&" + "entry_121440545" + "=" + URLEncoder.encode(tag_input,"UTF-8") +
                                    "&" + "entry_355541728" + "=" + URLEncoder.encode(stats_input,"UTF-8"); //Entry points for Response Form (MODIFIABLE)

                            //Refer to sticky note about entry points for the google form

                        } catch (UnsupportedEncodingException ex) {

                            result = false;

                        }

                        try{
                            //Create OkHttpClient for sending request

                            OkHttpClient client = new OkHttpClient();

                            //Create the request body with the help of Media Type

                            RequestBody body = RequestBody.create(FORM_DATA_TYPE, data);
                            Request request = new Request.Builder()
                                    .url(url)
                                    .post(body)
                                    .build();

                            //Send the request

                            Response response = client.newCall(request).execute();

                            if (!response.isSuccessful())
                            {throw new IOException("Unexpected code " + response);}

                        } catch (IOException exception){

                            result = false;
                        }

                        return result;
                    }

                    @Override
                    protected void onPostExecute (Boolean result){

                        //Print Success or failure message accordingly

                        Toast.makeText(context,result?getString(R.string.toast_success):getString(R.string.toast_fail),Toast.LENGTH_LONG).show();

                    }

                }

            }



