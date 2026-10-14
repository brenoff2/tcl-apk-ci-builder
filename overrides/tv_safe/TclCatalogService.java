package dk.fez.bluetoothquickconnect;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

/**
 * Foreground LAN server started explicitly from our activity.
 * Android automatically shows the user that this service is active.
 */
public final class TclCatalogService extends Service {
    private static final String TAG = "TclCatalogService";
    private static final String CHANNEL = "tcl_tv_catalog";
    private static final int NOTIFICATION_ID = 19865;
    private static volatile boolean running;
    private TvAppBridge bridge;

    public static boolean isRunning() {
        return running;
    }

    @Override public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL, "Controle TCL TV", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Conexão local entre a TCL e o controle no celular");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification notification() {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= 26) builder = new Notification.Builder(this, CHANNEL);
        else builder = new Notification.Builder(this);
        return builder
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentTitle("Controle TCL TV ativo")
                .setContentText("Aplicativos disponíveis para o controle na rede local")
                .setOngoing(true)
                .setShowWhen(false)
                .build();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(
                        NOTIFICATION_ID, notification(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIFICATION_ID, notification());
            }
            if (bridge == null) {
                bridge = new TvAppBridge(this);
                bridge.start();
            }
            running = true;
            return START_STICKY;
        } catch (RuntimeException error) {
            Log.e(TAG, "Failed to start visible catalog bridge", error);
            running = false;
            stopSelf();
            return START_NOT_STICKY;
        }
    }

    @Override public void onDestroy() {
        running = false;
        if (bridge != null) {
            bridge.stop();
            bridge = null;
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }
}
