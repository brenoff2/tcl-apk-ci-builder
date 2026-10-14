package dk.fez.bluetoothquickconnect;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Try the precise TCL accessory flow at Bluetooth profile level:
 * A2DP disconnect -> wait -> A2DP connect -> verify state.
 *
 * Never turn the TV's entire Bluetooth adapter off, never launch Settings
 * without user intent, and never claim success from a mere method invocation.
 * Third-party apps cannot automatically tap privileged Settings dialogs
 * without Accessibility or system INJECT_EVENTS. On a firmware that blocks
 * the hidden profile APIs, report the limitation and leave settings closed.
 */
final class BluetoothRecoverAction {
    interface Result {
        void onResult(String status);
    }

    private interface Finish {
        void accept(String status);
    }

    private static final String TAG = "TCLBluetoothRecovery";
    private BluetoothRecoverAction() { }

    static void recoverOrOpen(Context context, MappingStore.DeviceMapping mapping) {
        recover(context, mapping, result ->
                Log.i(TAG, "Headset recovery status=" + result));
    }

    static void recover(Context context, MappingStore.DeviceMapping mapping, Result result) {
        if (mapping == null) {
            result.onResult("not_configured");
            return;
        }
        final Context app = context.getApplicationContext();
        if (Build.VERSION.SDK_INT >= 31 &&
            app.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
            result.onResult("permission_required");
            return;
        }
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            result.onResult("bluetooth_off");
            return;
        }

        final BluetoothDevice device;
        try {
            device = adapter.getRemoteDevice(mapping.address);
            if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                result.onResult("not_paired");
                return;
            }
        } catch (SecurityException error) {
            result.onResult("permission_required");
            return;
        } catch (RuntimeException error) {
            result.onResult("not_supported");
            return;
        }

        final Handler main = new Handler(Looper.getMainLooper());
        final AtomicBoolean finished = new AtomicBoolean(false);
        final BluetoothProfile[] currentProxy = new BluetoothProfile[1];
        final Runnable[] finishTimeout = new Runnable[1];

        final Finish finish = status -> {
            if (!finished.compareAndSet(false, true)) return;
            if (finishTimeout[0] != null) main.removeCallbacks(finishTimeout[0]);
            BluetoothProfile proxy = currentProxy[0];
            if (proxy != null) {
                try { adapter.closeProfileProxy(BluetoothProfile.A2DP, proxy); }
                catch (RuntimeException ignored) { }
            }
            result.onResult(status);
        };
        finishTimeout[0] = () -> finish.accept("timeout");
        main.postDelayed(finishTimeout[0], 9500L);

        BluetoothProfile.ServiceListener listener = new BluetoothProfile.ServiceListener() {
            @Override public void onServiceConnected(int profileId, BluetoothProfile proxy) {
                if (finished.get()) {
                    adapter.closeProfileProxy(profileId, proxy);
                    return;
                }
                if (profileId != BluetoothProfile.A2DP) {
                    finish.accept("not_supported");
                    return;
                }
                currentProxy[0] = proxy;
                boolean wasConnected;
                try {
                    wasConnected = proxy.getConnectionState(device) ==
                            BluetoothProfile.STATE_CONNECTED;
                } catch (SecurityException error) {
                    finish.accept("permission_required");
                    return;
                } catch (RuntimeException error) {
                    finish.accept("not_supported");
                    return;
                }

                // Exactly the Disconnect -> Connect sequence requested.
                // AOSP exposes connection state publicly, but A2DP connect/
                // disconnect are hidden and can be denied by vendor firmware.
                if (wasConnected && !invoke(proxy, "disconnect", device)) {
                    finish.accept("api_restricted");
                    return;
                }
                main.postDelayed(() -> {
                    if (finished.get()) return;
                    if (!invoke(proxy, "connect", device)) {
                        finish.accept("api_restricted");
                        return;
                    }
                    verifyConnected(proxy, device, main, finish, 0);
                }, wasConnected ? 1300L : 150L);
            }

            @Override public void onServiceDisconnected(int profileId) {
                finish.accept("profile_unavailable");
            }
        };
        try {
            if (!adapter.getProfileProxy(app, listener, BluetoothProfile.A2DP)) {
                finish.accept("profile_unavailable");
            }
        } catch (SecurityException error) {
            finish.accept("permission_required");
        } catch (RuntimeException | LinkageError error) {
            Log.w(TAG, "A2DP profile unavailable", error);
            finish.accept("not_supported");
        }
    }

    private static boolean invoke(BluetoothProfile proxy, String verb,
                                  BluetoothDevice device) {
        try {
            Method method = proxy.getClass().getMethod(verb, BluetoothDevice.class);
            Object response = method.invoke(proxy, device);
            return Boolean.TRUE.equals(response);
        } catch (Exception | LinkageError error) {
            Log.w(TAG, "TCL blocked Bluetooth A2DP " + verb, error);
            return false;
        }
    }

    private static void verifyConnected(
            BluetoothProfile proxy, BluetoothDevice device,
            Handler main, Finish finish, int attempt) {
        try {
            if (proxy.getConnectionState(device) == BluetoothProfile.STATE_CONNECTED) {
                finish.accept("connected");
                return;
            }
        } catch (SecurityException error) {
            finish.accept("permission_required");
            return;
        } catch (RuntimeException error) {
            finish.accept("not_supported");
            return;
        }
        if (attempt >= 5) {
            finish.accept("not_connected");
            return;
        }
        main.postDelayed(
                () -> verifyConnected(proxy, device, main, finish, attempt + 1),
                900L);
    }
}
