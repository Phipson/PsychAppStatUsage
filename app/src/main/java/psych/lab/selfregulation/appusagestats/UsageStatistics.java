package psych.lab.selfregulation.appusagestats;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by 14leec1 on 4/1/2017.
 */

public class UsageStatistics extends SendAppUsageStats{

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
    public static final String TAG = UsageStatistics.class.getSimpleName();

    @SuppressWarnings("ResourceType")

    //getStats function specifically retrieves the TimeStamp for the app (i.e. when it was last used). Unsure if it is needed for the experiment.

   /* The Following Class Gets the time that the App was last used

   public static void getStats(Context context){

        @SuppressWarnings("WrongConstant")
        UsageStatsManager stats_manager = (UsageStatsManager) context.getSystemService("usagestats");
        int interval = UsageStatsManager.INTERVAL_YEARLY;
        Calendar calendar = Calendar.getInstance();
        long endTime = calendar.getTimeInMillis();
        calendar.add(Calendar.YEAR, -1);
        long startTime = calendar.getTimeInMillis();

        Log.d(TAG, "Range start:" + dateFormat.format(startTime));
        Log.d(TAG, "Range end:" + dateFormat.format(endTime));

        UsageEvents events  = stats_manager.queryEvents(startTime, endTime);
        while (events.hasNextEvent()){

            UsageEvents.Event e = new UsageEvents.Event();
            events.getNextEvent(e);

            if (e != null){

                Log.d(TAG, "Event: " + e.getPackageName() + "\t" + e.getTimeStamp());

            }

        } //TODO: Get the Notification Listener Time and also post it

    } */

    public static List<UsageStats> getUsageStatsList (Context context){

        UsageStatsManager stats_manager = getUsageStatsManager (context);

        Calendar BeginCalendar = Calendar.getInstance();
        BeginCalendar.set(2017, 1, 1);


        Calendar EndCalendar = Calendar.getInstance();


        /* This is only necessary if you are trying to display the start range and end range
        Log.d(TAG, "Range start: " + dateFormat.format(startTime));
        Log.d(TAG, "Range end: " + dateFormat.format(endTime));
       */

        List<UsageStats> usageStatsList = stats_manager.queryUsageStats(UsageStatsManager.INTERVAL_YEARLY, BeginCalendar.getTimeInMillis(), EndCalendar.getTimeInMillis());

        for (int i = 0; i < usageStatsList.size(); i++){

            for (int j = usageStatsList.size()-1; j > i; j--) {

                if (usageStatsList.get(j).getPackageName() == usageStatsList.get(i).getPackageName()){

                    usageStatsList.remove(j);

                }

            }

        }

        return usageStatsList; //returns a list involving usage stats, identified by the variable usageStatsList

    }

    /*public static ArrayList<String> AppTimeUsageStats (List<UsageStats> usageStatsList, Context context){ //Requires context parameter to get the specific static context from SendAppUsageStats class

        SharedPreferences appSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context); //Uses static context that has been sent from class

        Gson gson = new Gson();

        Type type = new TypeToken<List<Statistics>>(){}.getType();

        String json = appSharedPrefs.getString("key", null);

        List<Statistics> UsageStatsStats = gson.fromJson(json, type);

        ArrayList<String> foregroundTimeList = new ArrayList<>();

        for (Statistics u : UsageStatsStats){

                if (u.getTime() > 0) {

                    String timeData = "Package: " + u.getAppName() + "\t" + "Foreground Time: " + u.getTime() + "\n";

                    foregroundTimeList.add(timeData);
                }

        }

        return foregroundTimeList;

    }*/

    //TODO: Application Overload

    public static ArrayList<String> AppTimeUsageStats (List<UsageStats> usageStatsList){ //Requires context parameter to get the specific static context from SendAppUsageStats class

        ArrayList<String> foregroundTimeList = new ArrayList<>();

        for (UsageStats u : usageStatsList){

            if (u.getTotalTimeInForeground() > 0) {

                String timeData = "Package: " + u.getPackageName() + "\t" + "Foreground Time: " + u.getTotalTimeInForeground() + "\n";

                foregroundTimeList.add(timeData);
            }

        }

        return foregroundTimeList;

    }


    private static UsageStatsManager getUsageStatsManager (Context context){

        @SuppressWarnings("WrongConstant") //Need suppresswarning to enable "usagestats" for API 21. See more: http://stackoverflow.com/questions/39882317/android-get-usagestatsmanager-in-api-level-21
        UsageStatsManager stats_manager = (UsageStatsManager) context.getSystemService("usagestats");

        return stats_manager;

    }


}
