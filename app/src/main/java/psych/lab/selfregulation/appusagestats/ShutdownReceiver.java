package psych.lab.selfregulation.appusagestats;

import android.app.usage.UsageStats;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

import com.google.gson.Gson;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class ShutdownReceiver extends BroadcastReceiver {

    class Statistics {//Data Type that will be used to update and collect foreground times

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


    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals("android.intent.action.ACTION_SHUTDOWN") || intent.getAction().equals("android.intent.action.QUICKBOOT_POWEROFF")) {
            // Set the alarm here.
            //Insert code here
            //TODO: On Shutdown automatically send stats
            //TODO: On Shutdown save updated copy of stats

            ArrayList<Statistics> UsageStatsStats = new ArrayList<>();
            List<UsageStats> TempHolder = UsageStatistics.getUsageStatsList(context);

            SharedPreferences appsSharedPref = PreferenceManager.getDefaultSharedPreferences(context); //Gets the existing stats that have been saved if available.
            Gson gson = new Gson();
            Type type = new TypeToken<List<UsageStatistics>>() {
            }.getType();
            String json = appsSharedPref.getString("key", null);

            if (json != null) {//Tests whether something has been saved beforehand
                UsageStatsStats = gson.fromJson(json, type);
            }

            if (UsageStatsStats == null || UsageStatsStats.isEmpty()) {//If Empty, will add everything from TempHolder to the UsageStatsStats
                for (int i = 0; i < TempHolder.size(); i++) {

                    Statistics Object = new Statistics();

                    Object.setAppName(TempHolder.get(i).getPackageName().toString());
                    Object.setTime(TempHolder.get(i).getTotalTimeInForeground());

                    UsageStatsStats.add(Object);

                }

            } /*else {//If Not Empty, will first check whether there is an updated foregroundtime in TempHolder and add to UsageStatsStats

                for (int i = 0; i < UsageStatsStats.size(); i++) {

                    for (int j = 0; j < TempHolder.size(); j++) {

                        if (UsageStatsStats.get(i).getAppName() == TempHolder.get(j).getPackageName().toString()) { //Tests whether name matches and if the time in the list is the same as the total foreground time

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

                        if (TempHolder.get(l).getPackageName() == UsageStatsStats.get(k).getAppName().toString()){
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

            }*/

            SharedPreferences.Editor prefsEditor = appsSharedPref.edit(); //Saves the input from previous loops
            json = gson.toJson(UsageStatsStats);
            prefsEditor.putString("key", json);
            prefsEditor.commit();

        }}

}
