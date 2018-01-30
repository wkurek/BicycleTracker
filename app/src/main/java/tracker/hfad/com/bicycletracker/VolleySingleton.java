package tracker.hfad.com.bicycletracker;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Singleton class used as an interface to Volley.
 * Allow to set one RequestQueue object and access it from application during entire runtime.
 */
class VolleySingleton {
    private static VolleySingleton instance;
    private RequestQueue requestQueue;
    private Context context;

    private VolleySingleton(Context context) {
        this.context = context;
        requestQueue = getRequestQueue();
    }

    public static VolleySingleton newInstance(Context context) {
        if(instance == null) {
            instance = new VolleySingleton(context);
        }
        return instance;
    }

    RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        }
        return requestQueue;
    }

    <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }
}
