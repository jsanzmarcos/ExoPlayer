package com.google.android.exoplayer.latency;


import android.util.Log;

import com.google.android.exoplayer.upstream.DefaultHttpDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


public class LatencyParallelDownload {


    private static final String TAG = "AKZ-LATPD";
    private static final int MAX_SESSIONS = 5;
    private static LatencyParallelDownload singleton = null;

    private synchronized static void createInstance() {
        if (singleton == null) {
            try {
                singleton = new LatencyParallelDownload();
            } catch (final Exception e) {
                Log.w(TAG, e.getMessage());
            }
        }
    }

    public static LatencyParallelDownload getInstance() {
        createInstance();
        return singleton;
    }





    HashMap<String,LatencyBoostSession> sessions = new HashMap<String,LatencyBoostSession>();


    public LatencyBoostSession findSession(String rawUri) {


        synchronized (sessions) {
            LatencyBoostSession f = sessions.get(rawUri);
            if (f != null) {
                return f;
            }


            if (sessions.size()>MAX_SESSIONS) {

                LatencyBoostSession oldest = null;
                for(Map.Entry<String,LatencyBoostSession> s : sessions.entrySet()) {
                    if (oldest == null) {
                        oldest =s.getValue();
                    } else if (s.getValue().getCreatedAt()<oldest.getCreatedAt()) {
                        oldest = s.getValue();
                    }
                    s.getValue().setCancel(true);
                }
                if (oldest!=null) {
                    sessions.remove(oldest.getRawUri());
                }
            }
            f = new LatencyBoostSession(rawUri);
            sessions.put(rawUri, f);
            return f;
        }

    }

}

