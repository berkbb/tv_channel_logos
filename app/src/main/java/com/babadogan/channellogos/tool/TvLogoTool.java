package com.babadogan.channellogos.tool;

import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.Process;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

/**
 * Runs on the TV as the shell user (started through adb with app_process and this APK as the
 * class path). The shell holds ACCESS_ALL_EPG_DATA, which ordinary apps cannot get, so only this
 * process may read and write the logos of the TV's own broadcast channels.
 *
 * <pre>
 * app_process -Djava.class.path=APK /system/bin com.babadogan.channellogos.tool.TvLogoTool list
 *   → one line per channel: id \t number \t name \t inputId \t logoBytes \t serviceType \t onid \t tsid \t sid
 * ... TvLogoTool write ID PNG [ID PNG ...]
 *   → "OK id" or "FAIL id reason" per channel
 * ... TvLogoTool dump ID [ID ...]
 *   → "id \t base64 PNG" for each channel that has a logo (none: no line)
 * ... TvLogoTool remove ID [ID ...]
 *   → "OK id" or "FAIL id reason" per channel; the channel is left without a logo
 * </pre>
 *
 * The TV provider is reached the way the `content` shell command does it (a hidden
 * IContentProvider), through reflection with arguments matched by type, so the differing
 * signatures of Android 8 to 12 all work.
 */
public final class TvLogoTool {

    private static final String AUTHORITY = "android.media.tv";
    private static final String CALLING_PACKAGE = "com.android.shell";

    private TvLogoTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("usage: TvLogoTool list | write ID PNG [ID PNG ...]");
            System.exit(2);
        }
        Object activityManager = Class.forName("android.app.ActivityManager").getMethod("getService").invoke(null);
        IBinder token = new Binder();
        Object provider = acquireProvider(activityManager, token);
        try {
            if (args[0].equals("list")) {
                list(provider);
            } else if (args[0].equals("write")) {
                for (int i = 1; i + 1 < args.length; i += 2) {
                    write(provider, args[i], args[i + 1]);
                }
            } else if (args[0].equals("dump")) {
                for (int i = 1; i < args.length; i++) dump(provider, args[i]);
            } else if (args[0].equals("remove")) {
                for (int i = 1; i < args.length; i++) remove(provider, args[i]);
            }
        } finally {
            activityManager.getClass()
                    .getMethod("removeContentProviderExternal", String.class, IBinder.class)
                    .invoke(activityManager, AUTHORITY, token);
        }
    }

    private static Object acquireProvider(Object activityManager, IBinder token) throws Exception {
        for (Method m : activityManager.getClass().getMethods()) {
            if (!m.getName().equals("getContentProviderExternal")) continue;
            Class<?>[] types = m.getParameterTypes();
            Object[] values = new Object[types.length];
            boolean firstString = true;
            for (int i = 0; i < types.length; i++) {
                if (types[i] == String.class) {
                    // (name, userId, token) on Android 8-10; (name, userId, token, tag) later
                    values[i] = firstString ? AUTHORITY : "channel-logos";
                    firstString = false;
                } else if (types[i] == int.class) {
                    values[i] = 0;
                } else if (types[i] == IBinder.class) {
                    values[i] = token;
                }
            }
            Object holder = m.invoke(activityManager, values);
            if (holder == null) throw new IllegalStateException("TV provider not available");
            return holder.getClass().getField("provider").get(holder);
        }
        throw new IllegalStateException("getContentProviderExternal not found");
    }

    private static void list(Object provider) throws Exception {
        Uri uri = Uri.parse("content://" + AUTHORITY + "/channel");
        String[] projection = {"_id", "display_number", "display_name", "input_id", "service_type",
                "original_network_id", "transport_stream_id", "service_id"};
        Cursor cursor = (Cursor) invoke(provider, "query", uri, projection, "r");
        if (cursor == null) return;
        try {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                System.out.println(id + "\t" + clean(cursor.getString(1)) + "\t" + clean(cursor.getString(2))
                        + "\t" + clean(cursor.getString(3)) + "\t" + logoSize(provider, id)
                        + "\t" + clean(cursor.getString(4))
                        + "\t" + cursor.getInt(5) + "\t" + cursor.getInt(6) + "\t" + cursor.getInt(7));
            }
        } finally {
            cursor.close();
        }
    }

    private static long logoSize(Object provider, long channelId) {
        Uri uri = Uri.parse("content://" + AUTHORITY + "/channel/" + channelId + "/logo");
        try {
            ParcelFileDescriptor pfd = (ParcelFileDescriptor) invoke(provider, "openFile", uri, null, "r");
            if (pfd == null) return 0;
            long size = 0;
            try (InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(pfd)) {
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) > 0) size += n;
            }
            return size;
        } catch (Exception e) {
            return 0; // FileNotFoundException: the channel has no logo
        }
    }

    private static void write(Object provider, String channelId, String pngPath) {
        Uri uri = Uri.parse("content://" + AUTHORITY + "/channel/" + channelId + "/logo");
        try {
            ParcelFileDescriptor pfd = (ParcelFileDescriptor) invoke(provider, "openFile", uri, null, "w");
            try (OutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(pfd);
                 InputStream in = new FileInputStream(pngPath)) {
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
            }
            System.out.println("OK " + channelId);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            System.out.println("FAIL " + channelId + " " + clean(String.valueOf(cause)));
        }
    }

    private static void dump(Object provider, String channelId) {
        Uri uri = Uri.parse("content://" + AUTHORITY + "/channel/" + channelId + "/logo");
        try {
            ParcelFileDescriptor pfd = (ParcelFileDescriptor) invoke(provider, "openFile", uri, null, "r");
            if (pfd == null) return;
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            try (InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(pfd)) {
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) > 0) bytes.write(buffer, 0, n);
            }
            if (bytes.size() > 0) {
                System.out.println(channelId + "\t" + android.util.Base64.encodeToString(bytes.toByteArray(), android.util.Base64.NO_WRAP));
            }
        } catch (Exception e) {
            // no logo
        }
    }

    private static void remove(Object provider, String channelId) {
        Uri uri = Uri.parse("content://" + AUTHORITY + "/channel/" + channelId + "/logo");
        try {
            invoke(provider, "delete", uri, null, null);
            System.out.println("OK " + channelId);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            System.out.println("FAIL " + channelId + " " + clean(String.valueOf(cause)));
        }
    }

    /**
     * Calls [name] on the provider, filling its parameters by type: the calling package, the
     * URI, the projection, the open mode ("r"/"w") and, on Android 12+, an AttributionSource.
     */
    private static Object invoke(Object provider, String name, Uri uri, String[] projection, String mode)
            throws Exception {
        for (Method m : provider.getClass().getMethods()) {
            if (!m.getName().equals(name)) continue;
            Class<?>[] types = m.getParameterTypes();
            Object[] values = new Object[types.length];
            int strings = 0;
            boolean usable = true;
            for (int i = 0; i < types.length; i++) {
                Class<?> type = types[i];
                if (type == String.class) {
                    // The first String is the calling package; for openFile the last one is the mode.
                    values[i] = strings++ == 0 ? CALLING_PACKAGE : null;
                } else if (type == Uri.class) {
                    values[i] = uri;
                } else if (type == String[].class) {
                    values[i] = projection;
                } else if (type == Bundle.class) {
                    values[i] = null;
                } else if (type.getName().equals("android.content.AttributionSource")) {
                    values[i] = attributionSource();
                } else if (type.isPrimitive()) {
                    usable = false; // an older overload (e.g. with sort order ints); skip it
                }
            }
            if (!usable) continue;
            if (name.equals("openFile")) {
                for (int i = types.length - 1; i >= 0; i--) {
                    if (types[i] == String.class) {
                        values[i] = mode;
                        break;
                    }
                }
            }
            return m.invoke(provider, values);
        }
        throw new IllegalStateException(name + " not found on the content provider");
    }

    private static Object attributionSource() throws Exception {
        Class<?> builderClass = Class.forName("android.content.AttributionSource$Builder");
        Object builder = builderClass.getConstructor(int.class).newInstance(Process.myUid());
        builderClass.getMethod("setPackageName", String.class).invoke(builder, CALLING_PACKAGE);
        return builderClass.getMethod("build").invoke(builder);
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace('\t', ' ').replace('\n', ' ');
    }
}
