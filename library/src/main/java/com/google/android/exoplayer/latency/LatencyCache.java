package com.google.android.exoplayer.latency;

import android.util.Log;

import java.io.IOException;

public class LatencyCache {

    private static final String TAG = "AKZ-LC-";
    private final String name;
    private final byte[] buffer;
    private int bytesDownloaded;
    private IOException failed;
    private boolean cancel;


    public void setBytesTransferred(int bytesTransferred) {
        this.bytesTransferred = bytesTransferred;
    }

    private int bytesTransferred;
    private boolean downloadComplete;

    public LatencyCache(int size, String name) {
        this.name = name;
        buffer = new byte[size];
        bytesDownloaded = 0;
        bytesTransferred = 0;
        Log.i(TAG + name, "allocating " + size + " bytes");
    }


    long noticeAt = 0;

    public void push(byte[] block, int count) {

        if (bytesDownloaded + count > buffer.length) {
            Log.w(TAG, "too many bytes to read:" + bytesDownloaded);
        } else {

            synchronized (buffer) {
                System.arraycopy(block, 0, buffer, bytesDownloaded, count);
                bytesDownloaded += count;
                buffer.notify();
            }

            noticeAt += count;
            if (noticeAt >= 1024 * 1024) {
                Log.d(TAG + name, "Downloaded " + bytesDownloaded + " so far (" + bytesTransferred + " transferred)");
                noticeAt -= 1024 * 1024;
            }
        }
    }

    public void setDownloadComplete(boolean downloadComplete) {
        this.downloadComplete = downloadComplete;
    }

    public boolean isDownloadComplete() {
        return downloadComplete;
    }




    public int read(byte[] outputBuffer, int offset, int readLength, boolean block) throws IOException, InterruptedException {

        if (block) {

            while (true) {
                if (isDownloadComplete() || getFailed() != null) {
                    break;
                }
                synchronized (buffer) {
                    if (bytesTransferred + readLength <= bytesDownloaded) {
                        break;
                    }
                    buffer.wait();
                }
            }
        }


        if (getFailed() != null) {
            throw getFailed();
        }


        if (isDownloadComplete()) {
            int pending = bytesDownloaded - bytesTransferred;
            if (pending > 0) {
                if (pending > readLength) {
                    System.arraycopy(buffer, bytesTransferred, outputBuffer, offset, readLength);
                    bytesTransferred += readLength;
                    return readLength;
                } else {
                    System.arraycopy(buffer, bytesTransferred, outputBuffer, offset, pending);
                    bytesTransferred += pending;
                    return pending;
                }
            } else {

                Log.i(TAG + name, "download complete:" + pending + " read:" + bytesDownloaded + " transferred:" + bytesTransferred + ", requested:" + readLength);
                return -1;
            }
        }

        synchronized (buffer) {
            System.arraycopy(buffer, bytesTransferred, outputBuffer, offset, readLength);
            bytesTransferred += readLength;
            return readLength;
        }
    }



    public int getBytesTransferred() {
        return bytesTransferred;
    }

    public String getName() {
        return name;
    }

    public void setFailed(IOException failed) {
        this.failed = failed;
    }

    public IOException getFailed() {
        return failed;
    }


    public boolean isCancel() {
        return cancel;
    }

    public void setCancel(boolean cancel) {
        this.cancel = cancel;
    }
}

