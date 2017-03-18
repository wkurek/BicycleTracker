package tracker.hfad.com.bicycletracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class ShutdownReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
       SaveSharedPreference.setStarted(context, false);
    }
}
