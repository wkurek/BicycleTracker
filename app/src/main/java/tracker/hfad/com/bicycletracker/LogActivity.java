package tracker.hfad.com.bicycletracker;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;



public class LogActivity extends Activity
{

    private EditText log_login, log_password;
    private String login, password;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        if(SaveSharedPreference.getUserID(getApplicationContext()) >= 0)
        {
            Intent intent = new Intent(LogActivity.this, MainActivity.class);
            startActivity(intent);
            this.finish();
        }

        log_login = (EditText)findViewById(R.id.log_login);
        log_password = (EditText)findViewById(R.id.log_password);

    }

    public void log(View view)
    {
        login = log_login.getText().toString();
        password = log_password.getText().toString();

        ExternDatabaseTasks.LogTask externLogTask = new ExternDatabaseTasks.LogTask(this, login, password);
        externLogTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void register(View view)
    {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://rower.lowicz.com.pl/BicycleTracker%20www/rejestracja.php"));
        startActivity(browserIntent);
    }
}
