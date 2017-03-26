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
import java.lang.reflect.Array;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tomfurrier.costreport.Constants.INTENT_EXTRA_BALANCE_CHANGE_INFO;
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
                    /*int duration = Toast.LENGTH_LONG;
                    Toast toast = Toast.makeText(context,
                            "senderNum: "+ senderNum + ", message: " + message, duration);
                    toast.show();*/

                    if (containsIgnoreCase(message, "OTPdirekt")) {
                        // it's probably a balance report SMS, go on & extract info

                        ArrayList<String> balanceChangeInfo = extractBalanceChangeInfo(message);
                        Intent uploadServiceIntent = new Intent(context, UploadService.class);
                        uploadServiceIntent.putStringArrayListExtra(INTENT_EXTRA_BALANCE_CHANGE_INFO, balanceChangeInfo);
                        context.startService(uploadServiceIntent);
                    }

                } // end for loop

            } // bundle is null

        } catch (Exception e) {
            Log.e("SmsReceiver", "Exception smsReceiver" + e + ", Stacktrace: " + e.getStackTrace());

        }
    }

    private ArrayList<String> extractBalanceChangeInfo(String message) throws ParseException {
        ArrayList<String> result = new ArrayList<>();

        String messageWithoutAccents = Normalizer.normalize(message, Normalizer.Form.NFD);
        messageWithoutAccents = messageWithoutAccents.replaceAll("\\p{M}", "");

        String[] messageParts = messageWithoutAccents.split(";");

        result.add(extractTimestamp(messageParts[0].substring(0, 12))); // Timestamp
        result.add(extractAmount(messageParts[0])); // Amount
        result.add(messageParts[1]); // Details

        return result;
    }

    private String extractTimestamp(String dateString) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd HH:mm");
        Date date = dateFormat.parse(dateString);

        return date.toString();
    }

    private String extractAmount(String messagePart) {
        Pattern p = Pattern.compile("(-?|\\+?)[0-9]+(\\.[0-9]+)? HUF");
        Matcher m = p.matcher(messagePart);
        String result = "";

        if (m.find()) {
            result = m.group();
            result = result.replace("+", "").replace(" HUF", "").replace(".", ",");
        }

        Log.i("info", "extracted amount " + result);

        return result;
    }
}
