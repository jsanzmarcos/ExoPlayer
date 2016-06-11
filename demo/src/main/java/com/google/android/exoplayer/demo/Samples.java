/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer.demo;

import com.google.android.exoplayer.util.Util;

import java.util.Locale;

/**
 * Holds statically defined sample definitions.
 */
/* package */ class Samples {

    public static class Sample {

        public final String name;
        public final String contentId;
        public final String provider;
        public final String uri;
        public final int type;

        public Sample(String name, String uri, int type) {
            this(name, name.toLowerCase(Locale.US).replaceAll("\\s", ""), "", uri, type);
        }

        public Sample(String name, String contentId, String provider, String uri, int type) {
            this.name = name;
            this.contentId = contentId;
            this.provider = provider;
            this.uri = uri;
            this.type = type;
        }

    }


    public static final Sample[] MISC = new Sample[] {



            new Sample("Redstation", "http://mis5.vocvox.com:8081/slow/b60b2139-e859-4b9e-adb1-889d6660ffc6.mp3", Util.TYPE_OTHER),
            new Sample("Redstation+1connection", "http://mis5.vocvox.com:8081/slow/b60b2139-e859-4b9e-adb1-889d6660ffc6.mp3?#exoplayerboost", Util.TYPE_OTHER),
            new Sample("Greece", "http://mobile-stream-p.akazoo.com/store/stream-8937895858/mp4/320/194AC7B7-C44B-43A3-B13A-1B290B4B2A0E", Util.TYPE_OTHER),
            new Sample("Greece+1connection", "http://mobile-stream-p.akazoo.com/store/stream-8937895858/mp4/320/194AC7B7-C44B-43A3-B13A-1B290B4B2A0E?#exoplayerboost", Util.TYPE_OTHER),
            new Sample("Redstation+Singapoore", "http://mis5.vocvox.com:8081/fast/b60b2139-e859-4b9e-adb1-889d6660ffc6.mp3", Util.TYPE_OTHER)
            //   new Sample("Slow file", "http://192.168.0.189:8081/slow/ffff0a08e090f2edc9791cb5d72b5d8a.mp3", Util.TYPE_OTHER),
            //    new Sample("Akazoo", "http://192.168.0.189:8081/fast/ffff0a08e090f2edc9791cb5d72b5d8a.mp3|||http://192.168.0.189:8081/slow/ffff0a08e090f2edc9791cb5d72b5d8a.mp3", Util.TYPE_OTHER),
/*    new Sample("Apple AAC 10s", "https://devimages.apple.com.edgekey.net/"
        + "streaming/examples/bipbop_4x3/gear0/fileSequence0.aac", Util.TYPE_OTHER),
    new Sample("Apple TS 10s", "https://devimages.apple.com.edgekey.net/streaming/examples/"
        + "bipbop_4x3/gear1/fileSequence0.ts", Util.TYPE_OTHER),
    new Sample("Android screens (Matroska)", "http://storage.googleapis.com/exoplayer-test-media-1/"
        + "mkv/android-screens-lavf-56.36.100-aac-avc-main-1280x720.mkv",
        Util.TYPE_OTHER),
    new Sample("Big Buck Bunny (MP4 Video)",
        "http://redirector.c.youtube.com/videoplayback?id=604ed5ce52eda7ee&itag=22&source=youtube&"
        + "sparams=ip,ipbits,expire,source,id&ip=0.0.0.0&ipbits=0&expire=19000000000&signature="
        + "513F28C7FDCBEC60A66C86C9A393556C99DC47FB.04C88036EEE12565A1ED864A875A58F15D8B5300"
        + "&key=ik0", Util.TYPE_OTHER),
    new Sample("Google Play (MP3 Audio)",
        "http://storage.googleapis.com/exoplayer-test-media-0/play.mp3", Util.TYPE_OTHER),
    new Sample("Google Play (Ogg/Vorbis Audio)",
        "https://storage.googleapis.com/exoplayer-test-media-1/ogg/play.ogg", Util.TYPE_OTHER),
    new Sample("Google Glass (WebM Video with Vorbis Audio)",
        "http://demos.webmproject.org/exoplayer/glass_vp9_vorbis.webm", Util.TYPE_OTHER),
    new Sample("Big Buck Bunny (FLV Video)",
        "http://vod.leasewebcdn.com/bbb.flv?ri=1024&rs=150&start=0", Util.TYPE_OTHER),
  */};

    private Samples() {}

}
