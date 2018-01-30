package tracker.hfad.com.bicycletracker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;


public class LogActivity extends Activity {
    private static final String TAG = LogActivity.class.getSimpleName();
    private static final String LOGIN_URL = "http://rower.lowicz.com.pl/BicycleTracker%20www/mobile_log.php";
    private static final String REGISTER_URL = "http://rower.lowicz.com.pl/BicycleTracker%20www/rejestracja.php";

    private static final String KEY_LOGIN = "login";
    private static final String KEY_PASSWORD = "password";

    private EditText loginView, passwordView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        if(SaveSharedPreference.getUserID(getApplicationContext()) >= 0) {
            Log.i(TAG, "User has already logged in.");
            Intent intent = new Intent(LogActivity.this, MainActivity.class);
            startActivity(intent);
            this.finish();
        }

        loginView = (EditText)findViewById(R.id.log_login);
        passwordView = (EditText)findViewById(R.id.log_password);

        Button loginButton = (Button) findViewById(R.id.log_log_button);
        Button registerButton = (Button) findViewById(R.id.log_register_button);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                login(loginView.getText().toString(),
                        passwordView.getText().toString());
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(REGISTER_URL));
                startActivity(browserIntent);
            }
        });

    }

    private void login(final String login, final String password) {
        Log.i(TAG, "Create login request.");
        StringRequest request = new StringRequest(Request.Method.POST, LOGIN_URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if((!response.equals("error")) && !response.isEmpty()) {
                    initUser(Integer.parseInt(response));
                } else {
                    Toast.makeText(getApplicationContext(), "Incorrect login or password. Try again!",
                            Toast.LENGTH_LONG).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(TAG, error.getMessage());
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put(KEY_LOGIN, login);
                params.put(KEY_PASSWORD, password);
                return params;
            }
        };

        VolleySingleton.newInstance(this).addToRequestQueue(request);
    }

    private void initUser(Integer userId) {
        SaveSharedPreference.setUserID(this, userId);

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
