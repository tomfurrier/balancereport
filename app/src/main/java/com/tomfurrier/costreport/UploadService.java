package com.tomfurrier.costreport;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.preference.PreferenceManager;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.text.TextUtils;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.sheets.v4.model.Sheet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import static com.tomfurrier.costreport.Constants.PREF_ACCOUNT_NAME;
import static com.tomfurrier.costreport.Constants.SCOPES;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class UploadService extends IntentService {
    public UploadService() {
        super("UploadService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {

            uploadBalanceChange(this);
        }
    }

    private void uploadBalanceChange(Context context) {
        // Initialize credentials and service object.
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                context, Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        String accountName = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_ACCOUNT_NAME, null);
        if (accountName != null) {
            credential.setSelectedAccountName(accountName);
        } else {
            Log.e("error", "accountName null. should open app & choose");
        }

        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        com.google.api.services.sheets.v4.Sheets service = new com.google.api.services.sheets.v4.Sheets.Builder(
                transport, jsonFactory, credential)
                .setApplicationName("Cost Report")
                .build();


        String spreadsheetId = "1qLFneJFtHfVrlvtH46-D84khkscrHODTB2NkJX3mAe4";
        List<String> results = new ArrayList<String>();

        List<Sheet> sheets = null;
        try {
            sheets = service.spreadsheets().get(spreadsheetId).execute().getSheets();
        } catch (Exception e) {
            Log.e("exception", e.getMessage() + ", stacktrace: " + e.getStackTrace());
        }

        if (sheets != null) {
            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("yyyy. MM.");
            String formattedDate = df.format(c.getTime());

            for (Sheet sheet : sheets) {
                if (sheet.getProperties().getTitle().equals(formattedDate)) {
                    results.add("operation: sheet exists for current date: " + formattedDate);
                    break;
                }
            }

            Log.i("info", "upload blanace chaGNE operation result: " + TextUtils.join("\n", results));
        } else {
            Log.i("info", "sheets null!!!!!");
        }
    }
}
