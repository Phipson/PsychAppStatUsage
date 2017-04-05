package psych.lab.selfregulation.appusagestats;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class home_page extends AppCompatActivity {

    public static home_page mainActivity;
    public static Boolean isVisible = false;
    private static final String TAG = "MainActivity";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    Button send_data;
    Button settings;
    Button send_notification;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        settings = (Button) findViewById(R.id.settings_button);
        send_data = (Button) findViewById(R.id.send_stats_button);



        send_data.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){

                Intent intent = new Intent(home_page.this, SendAppUsageStats.class);
                startActivity(intent);

            }


        });

        settings.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){

                Intent intent = new Intent(home_page.this, SendAppUsageStats.class);
                startActivity(intent);

            }


        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        isVisible = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isVisible = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isVisible = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        isVisible = false;
    }

    public void showDialog() //Only used when the app needs to display the stats (new activity needed)
    {

        LayoutInflater li = LayoutInflater.from(context);
        View promptsView = li.inflate(R.layout.alert_prompt, null);
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView
                .findViewById(R.id.user_input);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setMessage(getString(R.string.request_password))
                .setNegativeButton("Go",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                /** DO THE METHOD HERE WHEN PROCEED IS CLICKED*/
                                String user_text = (userInput.getText()).toString();

                                /** CHECK FOR USER'S INPUT **/
                                if (user_text.equals("psychappusagestats"))
                                {
                                    Intent intent = new Intent (home_page.this, DisplayUsageStats.class); //If password matches, then you will be directed to a new page
                                    startActivity(intent);

                                }
                                else{
                                    Log.d(user_text,"string is empty");
                                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                    builder.setTitle(getString(R.string.error_title));
                                    builder.setMessage(getString(R.string.error_password));
                                    builder.setPositiveButton(getString(R.string.cancel_button), null);
                                    builder.setNegativeButton(getString(R.string.retry_button), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int id) {
                                            showDialog();
                                        }
                                    });
                                    builder.create().show();

                                }
                            }
                        })
                .setPositiveButton(getString(R.string.cancel_button),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }

                        }

                );

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }

  }