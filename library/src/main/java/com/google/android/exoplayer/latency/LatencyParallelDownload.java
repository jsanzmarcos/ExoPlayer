package com.google.android.exoplayer.latency;


import android.util.Log;

import com.google.android.exoplayer.upstream.DefaultHttpDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class LatencyParallelDownload {


    private static final String TAG = "AKZ-LATPD";
    private static LatencyParallelDownload singleton = null;
    LatencyCache far;
    LatencyCache near;
    private int firstSegmentSize;
    private long contentLength;
    private String currentUri;

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

    public void spawn(final String uri, final DefaultHttpDataSource defaultHttpDataSource, final LatencyCache cache) {


        Thread thread = new Thread(new Runnable(){
            @Override
            public void run(){



                Log.i(TAG, "Spawning download from remote host: " + uri);

                InputStream input = null;

                HttpURLConnection connection = null;
                URL url;
                try {
                    url = new URL(uri);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(defaultHttpDataSource.getConnectTimeoutMillis());
                    connection.setReadTimeout(defaultHttpDataSource.getReadTimeoutMillis());
                    long offset = 0;
                    connection.setRequestProperty("User-Agent", defaultHttpDataSource.getUserAgent());
                    connection.setRequestProperty("Accept-Encoding", "identity");
                    connection.setDoOutput(false);
                    connection.connect();


                    input = connection.getInputStream();
                    byte data[] = new byte[8192];
                    while (true) {

                        if (cache.isCancel()) {
                            Log.i(TAG+cache.getName(),"Cancelled");
                            break;
                        }

                        int count = input.read(data);

                        if (count == -1) {
                            break;
                        }

                        if (count>0) {

                            cache.push(data, count);
                            offset += count;
                        }
                    }

                    Log.i(TAG,"Total bytes read from remote host:" + offset);
                    cache.setDownloadComplete(true);


                } catch (MalformedURLException e) {
                    Log.w(TAG, "malformed URL exception:" + e.getMessage());
                    cache.setFailed(new IOException());
                } catch (IOException e) {
                    Log.e(TAG, "far site download exception:" + e.getMessage(), e);
                    cache.setFailed(e);
                } finally {
                    try {

                        if (input != null)
                            input.close();
                    } catch (IOException ignored) {
                    }

                    if (connection != null)
                        connection.disconnect();
                }

            }
        });
        thread.start();
    }

    public boolean sameSession(String uri) {

        if (currentUri != null && currentUri.equals(uri)) {
            Log.i(TAG,"It is the same session, threads were already spawned");
            return true;
        }
        currentUri = uri;
        return false;
    }

    public LatencyCache getNear() {
        return near;
    }

    public LatencyCache getFar() {
        return far;
    }

    public void initializeFarBuffer(long contentLength) {

        Log.i(TAG, "initializeFarBuffer:" + currentUri);
        firstSegmentSize = 256*1024;
        this.contentLength = contentLength;
        far = new LatencyCache((int) (contentLength - firstSegmentSize), "FR");
        near = new LatencyCache(firstSegmentSize, "NR");
    }

    public long getContentLength() {
        return contentLength;
    }


    public int adjustOffset(long position) {

        if (position<firstSegmentSize) {
            getNear().setBytesTransferred((int) position);
            getFar().setBytesTransferred(0);
            return getNear().isDownloadComplete()?3:1;
        } else {
            getNear().setBytesTransferred(firstSegmentSize);
            getFar().setBytesTransferred((int) position - firstSegmentSize);
            return 2;
        }
    }

    public void cancel() {
        currentUri = null;
    }

    public void closeAfterReading(int totalRead) {
        Log.i(TAG, "Connection was closed after reading: " +totalRead + " bytes");
    }

    public void continueDownload(final HttpURLConnection connection) {


        Thread thread = new Thread(new Runnable(){
            @Override
            public void run(){
                Log.i(TAG, "Continuing download from near host in a separate thread...");

                final LatencyCache cache = getNear();
                InputStream input = null;
                long offset = 0;
                try {
                    input = connection.getInputStream();
                    byte data[] = new byte[8192];
                    while (true) {

                        if (cache.isCancel()) {
                            break;
                        }

                        int count = input.read(data);


                        if (count == -1) {
                            break;
                        }

                        if (count>0) {
                            offset += count;
                            cache.push(data, count);
                        }
                    }

                    Log.i(TAG,"Total bytes read from near host:" + offset);
                    cache.setDownloadComplete(true);

                } catch (IOException e) {
                    cache.setFailed(e);
                } finally {
                    try {
                        if (input != null)
                            input.close();
                    } catch (IOException ignored) {
                    }

                    if (connection != null)
                        connection.disconnect();
                }

            }
        });
        thread.start();

    }

    public int getFirstSegmentSize() {
        return firstSegmentSize;
    }
}

