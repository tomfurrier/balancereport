package com.tomfurrier.costreport;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Log;

public class SmsReceiverService extends IntentService {

    SmsReceiver smsReceiver = new SmsReceiver();

    public SmsReceiverService() {
        super("SmsReceiverService");
    }

    /*public SmsReceiverService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
*/
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.i("SmsServiceREc", "check sms manually!!!");
        SmsReceiver.completeWakefulIntent(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        registerReceiver(smsReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));

        return START_STICKY;
    }
}

