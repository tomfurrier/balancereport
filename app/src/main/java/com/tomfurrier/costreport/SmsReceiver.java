package com.tomfurrier.costreport;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.sheets.v4.model.Sheet;

import java.io.IOException;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tomfurrier.costreport.Constants.SCOPES;
import static com.tomfurrier.costreport.StringUtils.containsIgnoreCase;

public class SmsReceiver extends BroadcastReceiver {

    final SmsManager sms = SmsManager.getDefault();

    @Override
    public void onReceive(Context context, Intent intent) {
        // Retrieves a map of extended data from the intent.
        final Bundle bundle = intent.getExtras();

        try {

            if (bundle != null) {

                final Object[] pdusObj = (Object[]) bundle.get("pdus");

                for (int i = 0; i < pdusObj.length; i++) {

                    SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdusObj[i]);
                    String phoneNumber = currentMessage.getDisplayOriginatingAddress();

                    String senderNum = phoneNumber;
                    String message = currentMessage.getDisplayMessageBody();

                    Log.i("SmsReceiver", "pdusObj[" + i + "] senderNum: "+ senderNum + "; message: " + message);

                    // Show Alert
                    int duration = Toast.LENGTH_LONG;
                    Toast toast = Toast.makeText(context,
                            "senderNum: "+ senderNum + ", message: " + message, duration);
                    toast.show();

                    if (containsIgnoreCase(message, "OTPdirekt")) {
                        // it's probably a balance report SMS, go on with extracting info

                        extractBalanceChange(message);
                        Intent uploadServiceIntent = new Intent(context, UploadService.class);
                        context.startService(uploadServiceIntent);
                    }

                } // end for loop

            } // bundle is null

        } catch (Exception e) {
            Log.e("SmsReceiver", "Exception smsReceiver" +e);

        }
    }

    public static String extractBalanceChange(String message) {
        String messageWithoutAccents = Normalizer.normalize(message, Normalizer.Form.NFD);
        messageWithoutAccents = messageWithoutAccents.replaceAll("\\p{M}", "");

        String[] messageParts = messageWithoutAccents.split(";");

        Pattern p = Pattern.compile("(-?|\\+?)[0-9]+(\\.[0-9]+)? HUF");
        Matcher m = p.matcher(messageParts[0]);
        String result = "";

        if (m.find()) {
            result = m.group();
        }

        Log.i("info", "extracted balance change: " + result);

        return result;
    }
}
