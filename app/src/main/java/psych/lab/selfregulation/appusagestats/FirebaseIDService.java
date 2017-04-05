package psych.lab.selfregulation.appusagestats;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

public class FirebaseIDService extends FirebaseInstanceIdService {
    private static final String TAG = "FirebaseIDService";
    private String Token;


    String TransferToken(){//Potential function to return the Token

        return Token;

    }

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        Token = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + Token);

        // TODO: Implement this method to send any registration to your app's servers.
        sendRegistrationToServer(Token);
    }

    private void sendRegistrationToServer(String token) {
        // Add custom implementation, as needed.
    }
}