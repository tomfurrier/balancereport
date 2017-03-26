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
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;


import static com.tomfurrier.costreport.Constants.INTENT_EXTRA_BALANCE_CHANGE_INFO;
import static com.tomfurrier.costreport.Constants.PREF_ACCOUNT_NAME;
import static com.tomfurrier.costreport.Constants.SCOPES;
import static com.tomfurrier.costreport.Constants.SPREADSHEET_ID;

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

            try {
                ArrayList<String> balanceChangeInfo = intent.getStringArrayListExtra(INTENT_EXTRA_BALANCE_CHANGE_INFO);
                uploadBalanceChange(this, balanceChangeInfo);
            } catch (IOException e) {
                Log.e("exception", e.getMessage() + ", stacktrace: " + e.getStackTrace());
            }
        }
    }

    private void uploadBalanceChange(Context context, ArrayList<String>  balanceChangeInfo) throws IOException{
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

        List<String> results = new ArrayList<String>();

        List<Sheet> sheets = service.spreadsheets().get(SPREADSHEET_ID).execute().getSheets();

        if (sheets != null) {
            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("yyyy. MM.");
            String formattedDate = df.format(c.getTime());

            String range = formattedDate + "!A1:A4";
            ValueRange requestBody = new ValueRange();
            requestBody.setMajorDimension("ROWS");
            requestBody.setRange(range);

            balanceChangeInfo.add(accountName); // Name


            List<Object> row = new ArrayList<Object>();

            for (String dataString : balanceChangeInfo) {
                row.add((Object)dataString);
            }

            requestBody.setValues(Arrays.asList(row));
            String valueInputOption = "USER_ENTERED";
            Sheets.Spreadsheets.Values.Append appendRequest =
                    service.spreadsheets().values().append(SPREADSHEET_ID, range, requestBody);
            appendRequest.setValueInputOption(valueInputOption);
            appendRequest.execute();

            Log.i("info", "upload blanace change operation result: " + TextUtils.join("\n", results));
        } else {
            Log.i("info", "sheets null!!!!!");
        }
    }
}
