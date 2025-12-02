package audio;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SoundManager {
    private static final Map<String, Clip> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, byte[]> AUDIO_CACHE = new HashMap<>();
    private static volatile boolean muted = false;  // global state of sound
    private static volatile String currentLooping = null;
    private static final Map<String, java.util.List<Clip>> PLAY_MAP = new ConcurrentHashMap<>();
    private static final Map<String, Clip> SINGLE_LOOP_MAP = new ConcurrentHashMap<>();
    private static final int MAX_SIMULTANEOUS = 8;
    private static final Map<String, List<Clip>> POOL = new ConcurrentHashMap<>();
    private static final Map<String, Clip> SINGLE_LOOP_CHANNEL_MAP =
            new ConcurrentHashMap<>();

    public static void play(String resourcePath) {
        if (muted) return;

        try {
            byte[] audioData = AUDIO_CACHE.computeIfAbsent(resourcePath, SoundManager::loadAudioData);
            if (audioData == null) return;

            Clip clip = AudioSystem.getClip();
            AudioInputStream ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(audioData));
            clip.open(ais);

            // PLAY_MAP에 등록
            PLAY_MAP.computeIfAbsent(resourcePath, k -> new java.util.ArrayList<>()).add(clip);

            clip.addLineListener(event -> {
                LineEvent.Type type = event.getType();
                if (type == LineEvent.Type.STOP || type == LineEvent.Type.CLOSE) {

                    List<Clip> l = PLAY_MAP.get(resourcePath);
                    if (l != null) {
                        l.remove(clip);
                    }
                    try {
                        clip.close();
                    } catch (Exception ignore) {}

                    try {
                        ais.close();
                    } catch (Exception ignore) {}
                }
            });

            clip.start();

        } catch (Exception e) {
            System.err.println("[Sound] Play failed: " + resourcePath + " -> " + e.getMessage());
        }
    }

    private static byte[] loadAudioData(String resourcePath) {
        String p = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        try (InputStream is = SoundManager.class.getResourceAsStream(p);
             BufferedInputStream bis = new BufferedInputStream(is)) {
            return bis.readAllBytes();
        } catch (Exception e) {
            System.err.println("[Sound] Load failed: " + p);
            return null;
        }
    }

    private static Clip loadClip(String path) {
        String p = path.startsWith("/") ? path : "/" + path;
        try (InputStream raw = SoundManager.class.getResourceAsStream(p)) {
            if (raw == null) throw new IllegalArgumentException("Resource not found: " + p);
            try (BufferedInputStream in = new BufferedInputStream(raw);
                 AudioInputStream ais = AudioSystem.getAudioInputStream(in)) {
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                return clip;
            }
        } catch (Exception e) {
            System.err.println("[Sound] Load failed: " + p + " -> " + e);
            return null;
        }
    }


    public static void playLoop(String resourcePath) {
        if (muted) return;  // no sound played
        try {
            Clip c = CACHE.computeIfAbsent(resourcePath, SoundManager::loadClip);
            if (c == null) return;
            c.setFramePosition(0);
            c.loop(Clip.LOOP_CONTINUOUSLY);
            c.start();
            currentLooping = resourcePath;  // useful for unmute
        } catch (Exception e) {
            System.err.println("[Sound] Loop failed: " + resourcePath + " -> " + e.getMessage());
        }
    }

    public static void cutBGM() {
        muted = true;
        if (currentLooping != null) {
            Clip c = CACHE.get(currentLooping);
            c.stop();
            setVolume(c, -80.0f);
        }
        System.out.println("[Sound] Global sound muted.");
    }

    public static void uncutBGM() {
        muted = false;
        System.out.println("[Sound] Global sound unmuted");
        System.out.println("[Sound] current looping : " + currentLooping);

        if (currentLooping != null) {
            Clip c = CACHE.get(currentLooping);
            c.start();
            setVolume(c, 0.0f);
            c.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public static void stop(String resourcePath) {
        try {
            java.util.List<Clip> list = PLAY_MAP.get(resourcePath);
            if (list == null) return;

            for (Clip c : list) {
                if (c != null) {
                    c.stop();
                    c.flush();
                    c.setFramePosition(0);
                }
            }

            list.clear();

        } catch (Exception e) {
            System.err.println("[Sound] Stop failed: " + resourcePath + " -> " + e.getMessage());
        }
    }


    public static void stopAll() {
        for (Clip c : CACHE.values()) {
            if (c.isRunning()) c.stop();
        }
    }
    public static void setVolume(Clip clip, float dB) {
        try {
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            gain.setValue(dB);
        }
        catch (Exception e) {
            System.err.println("[Sound] Volume control failed: " + e.getMessage());
        }
    }

    public static boolean isCurrentLoop(String path) {
        return currentLooping != null && currentLooping.equals(path);
    }
    public static void playSingleLoop(String path) {
        if (muted) return;

        try {
            Clip c = SINGLE_LOOP_MAP.get(path);

            if (c == null) {
                c = loadClip(path);
                if (c == null) return;
                SINGLE_LOOP_MAP.put(path, c);
            }

            if (!c.isRunning()) {
                c.setFramePosition(0);
                c.loop(Clip.LOOP_CONTINUOUSLY);
                c.start();
            }

        } catch (Exception e) {
            System.err.println("[Sound] playSingleLoop fail: " + e);
        }
    }

    public static void stopSingleLoop(String path) {
        Clip c = SINGLE_LOOP_MAP.get(path);
        if (c != null && c.isRunning()) {
            c.stop();
            c.flush();
            c.setFramePosition(0);
        }
    }
    public static void playPooled(String path) {
        if (muted) return;

        try {
            List<Clip> list = POOL.computeIfAbsent(path, k -> new java.util.ArrayList<>());
            Clip target = null;
            for (Clip c : list) {
                if (!c.isRunning()) {
                    target = c;
                    break;
                }
            }
            if (target == null && list.size() < MAX_SIMULTANEOUS) {
                target = loadClip(path);
                if (target != null) list.add(target);
            }

            if (target != null) {
                target.setFramePosition(0);
                target.start();
            }

        } catch (Exception e) {
            System.err.println("[Sound] playPooled fail : " + e);
        }
    }
    public static void playSingleLoopChannel(String path, String channel) {
        if (muted) return;

        try {
            String key = path + "#" + channel;

            Clip c = SINGLE_LOOP_CHANNEL_MAP.get(key);

            if (c == null) {
                c = loadClip(path);
                if (c == null) return;

                SINGLE_LOOP_CHANNEL_MAP.put(key, c);
            }

            if (!c.isRunning()) {
                c.setFramePosition(0);
                c.loop(Clip.LOOP_CONTINUOUSLY);
                c.start();
            }

        } catch (Exception e) {
            System.err.println("[Sound] playSingleLoopChannel fail: " + e);
        }
    }

    public static void stopSingleLoopChannel(String path, String channel) {

        String key = path + "#" + channel;

        Clip c = SINGLE_LOOP_CHANNEL_MAP.get(key);

        if (c != null && c.isRunning()) {
            c.stop();
            c.flush();
            c.setFramePosition(0);
        }
    }

}
