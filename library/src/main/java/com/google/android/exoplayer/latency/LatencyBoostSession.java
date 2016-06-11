package com.google.android.exoplayer.latency;

import android.util.Log;

import com.google.android.exoplayer.upstream.DefaultHttpDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by jesus on 11/6/16.
 */
public class LatencyBoostSession {

    private static final String TAG = "AKZ-LBS";
    private String rawUri;
    private String uri;
    private boolean boost;
    private long length;
    private Stage stage;
    private long contentLength;
    private String latencyHeader;
    private Mode mode = Mode.Unset;
    private long createdAt;
    private boolean cancel;
    private long position;
    private LatencyCache mainCache;
    private boolean cached;
    private String remoteURI;
    private LatencyCache auxCache;
    private InputStream inputStream;
    private Phase phase = Phase.Unset;

    public LatencyBoostSession(String rawUri) {

        this.rawUri = rawUri;

        boost = false;
        uri = rawUri;
        if (uri.endsWith("?#exoplayerboost")) {
            boost = true;
            uri = rawUri.substring(0,rawUri.length()- "?#exoplayerboost".length());
        }



        setCreatedAt(System.currentTimeMillis());
    }


    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public void close() {
        Log.i(TAG, "Session closed");
    }

    public Phase getPhase() {
        return phase;
    }

    public boolean isBoost() {
        return boost;
    }

    public enum Phase {
        FirstCache, SecondCache, Unset
    }

    public void adjustOffset() {

        switch(mode) {
            case SingleHostDownload:
                mainCache.setBytesTransferred((int) position);
                break;
            case TwoHostDownload:
                if (position<mainCache.getSize()) {
                    phase = Phase.FirstCache;
                    mainCache.setBytesTransferred((int) position);
                    auxCache.setBytesTransferred(0);
                } else {
                    phase = Phase.SecondCache;
                    mainCache.setBytesTransferred((int) mainCache.getSize());
                    auxCache.setBytesTransferred((int) position);
                }
                break;
        }

    }

    public void continueDownloadInAnotherThread(final HttpURLConnection connection) {

        Thread thread = new Thread(new Runnable(){
            @Override
            public void run(){
                Log.i(TAG, "Continuing download from near host in a separate thread...");

                final LatencyCache cache = mainCache;
                InputStream input = null;
                long offset = 0;
                try {
                    input = connection.getInputStream();
                    byte data[] = new byte[8192];
                    while (true) {

                        if (isCancel()) {
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

                    cache.setDownloadComplete(!isCancel());


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

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public void setLatencyHeader(String latencyHeader) {
        this.latencyHeader = latencyHeader;
    }

    public String getLatencyHeader() {
        return latencyHeader;
    }

    public void configure(long position) {
        this.position = position;

        if (latencyHeader==null) {
            /// this is a normal download
            if (boost) {
                setMode(Mode.SingleHostDownload);
                setCached(true);
                mainCache = new LatencyCache((int) contentLength, "main");
            } else {
                setCached(false);
                setMode(Mode.NormalExoPlayerDownload);
            }
            return;

        }

        Log.i(TAG,"latency header:" + latencyHeader);

        String[] p = latencyHeader.contains("|")?latencyHeader.split("\\|"):latencyHeader.split(",");

        mainCache = new LatencyCache(256*1024, "first");



        setContentLength(Long.parseLong(p[0]));

        auxCache = new LatencyCache((int) (getContentLength() - mainCache.getSize()), "second");

        setRemoteURI(p[1]);
        setMode(Mode.TwoHostDownload);
        setCached(true);


        adjustOffset();
    }


    public void spawnSecondSegmentDownload(final DefaultHttpDataSource defaultHttpDataSource) {


        Thread thread = new Thread(new Runnable(){
            @Override
            public void run(){


                Log.i(TAG, "Spawning download from remote host: " + getRemoteURI());

                InputStream input = null;

                HttpURLConnection connection = null;
                URL url;
                try {
                    url = new URL(getRemoteURI());
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

                        if (isCancel()) {
                            Log.i(TAG+auxCache.getName(),"Cancelled");
                            break;
                        }

                        int count = input.read(data);

                        if (count == -1) {
                            break;
                        }

                        if (count>0) {

                            auxCache.push(data, count);
                            offset += count;
                        }
                    }

                    Log.i(TAG,"Total bytes read from remote host:" + offset);
                    auxCache.setDownloadComplete(!isCancel());


                } catch (MalformedURLException e) {
                    Log.w(TAG, "malformed URL exception:" + e.getMessage());
                    auxCache.setFailed(new IOException());
                } catch (IOException e) {
                    Log.e(TAG, "far site download exception:" + e.getMessage(), e);
                    auxCache.setFailed(e);
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

    public int readBuffer(byte[] buffer, int offset, int readLength) throws IOException, InterruptedException {


        switch(getMode()) {


            case NormalExoPlayerDownload:
                return inputStream.read(buffer, offset, readLength);

            case SingleHostDownload:
                return mainCache.read(buffer, offset, readLength, true);

            case TwoHostDownload:

                switch(phase) {


                    case FirstCache:
                        int r = mainCache.read(buffer, offset, readLength, true);

                        if (mainCache.getBytesTransferred() >= mainCache.getSize()) {
                            Log.i(TAG, "near-remote change 1 ==> 2 (total reached) " + mainCache.getBytesTransferred() + " vs " + mainCache.getSize());
                            phase = Phase.SecondCache;
                            return r;

                        } else if (r == -1 ) {
                            Log.i(TAG, "latency stage change: 1 ==> 2 (reaching end)");
                            phase = Phase.SecondCache;
                            return auxCache.read(buffer, offset, readLength, true);
                        } else {

                            return r;
                        }

                    case SecondCache:
                        return auxCache.read(buffer, offset, readLength, true);

                    case Unset:
                        break;
                }

                break;
        }

        return -1;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getRawUri() {
        return rawUri;
    }

    public void setRawUri(String rawUri) {
        this.rawUri = rawUri;
    }

    public void setCancel(boolean cancel) {
        this.cancel = cancel;
    }

    public boolean isCancel() {
        return cancel;
    }

    public boolean isCached() {
        return cached;
    }

    public void setCached(boolean cached) {
        this.cached = cached;
    }

    public void setRemoteURI(String remoteURI) {
        this.remoteURI = remoteURI;
    }

    public String getRemoteURI() {
        return remoteURI;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    public enum Mode {
        Unset,
        NormalExoPlayerDownload,
        SingleHostDownload,
        TwoHostDownload
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }


    public enum Stage {
        AllFromB
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }
}
