package com.k12nt.k12netframe.fcm;

/**
 * Created by tarikcanturk on 21/09/16.
 */
import android.util.Log;

import com.k12nt.k12netframe.LoginActivity;
import com.k12nt.k12netframe.async_tasks.AsistoAsyncTask;
import com.k12nt.k12netframe.utils.definition.K12NetStaticDefinition;
import com.k12nt.k12netframe.utils.userSelection.K12NetUserReferences;
import com.k12nt.k12netframe.utils.webConnection.K12NetHttpClient;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;


public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {

    private static final String TAG = "MyFirebaseIIDService";

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    // [START refresh_token]
    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(refreshedToken);
    }
    // [END refresh_token]

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {

        TokenAsyncTask tokenAsyncTask = new TokenAsyncTask(token);
        tokenAsyncTask.execute();

    }

    public class TokenAsyncTask extends AsistoAsyncTask {

        private String token;

        public TokenAsyncTask(String token) {
            this.token = token;
        }

        @Override
        protected void onAsyncComplete() {

        }

        @Override
        protected Void doInBackground(Void... voids) {

            /*String requestQuery = K12NetUserReferences.getConnectionAddress() + "/SPSL.Web/ClientBin/Yuce-K12NET-SPServicesLibrary-SPDomainService.svc/json/SubmitChanges";

            String electronicIdJson = "{\"changeSet\": [{\"HasMemberChanges\": 0, \"Id\": 0, \"Operation\": 2, \"Entity\": {\"__type\": \"ElectronicId:#Yuce.K12NET.SPServicesLibrary\", \"ID\": \"00000000-0000-0000-0000-000000000000\", \"TypeID\": \""+ K12NetStaticDefinition.ASISTO_ANDROID_APPLICATION_ID+"\", \"Value\": \""+token+"\"}, \"Associations\": [{\"Key\": \"PersonalInfo_ElectronicIds\", \"Value\": [1]}]}, {\"HasMemberChanges\": 0, \"Id\": 1, \"Operation\": 2, \"Entity\": {\"__type\": \"PersonalInfo_ElectronicId:#Yuce.K12NET.SPServicesLibrary\", \"PersonalInfoID\": \""+ LoginActivity.providerId+"\", \"ElectronicIdID\": \"00000000-0000-0000-0000-000000000000\"}, \"Associations\": [{\"Key\": \"ElectronicId\", \"Value\": [0]}]}]}";

            try {
                String line = K12NetHttpClient.execute(requestQuery, electronicIdJson);
                Log.d("REGISTER", line);
            }
            catch (Exception e) {
                e.printStackTrace();
            }*/

            return null;
        }
    }
}