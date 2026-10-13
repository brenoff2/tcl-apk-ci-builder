package dk.fez.bluetoothquickconnect;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * For already-connected noisy A2DP headsets: try a genuine per-device
 * disconnect then reconnect. Never switch the whole TV Bluetooth adapter off.
 * Hidden A2DP methods are blocked by some TV firmwares; fallback to settings.
 */
final class BluetoothRecoverAction {
    private static final String TAG="TCLBluetooth";
    private BluetoothRecoverAction() {}

    static void recoverOrOpen(Context context, MappingStore.DeviceMapping mapping) {
        if(mapping==null) return;
        final Context app=context.getApplicationContext();
        final BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();
        if(adapter==null) { DeviceSettingsFallback.open(app,mapping);return; }
        final BluetoothDevice device;
        try {
            device=adapter.getRemoteDevice(mapping.address);
        }catch(RuntimeException error) {
            DeviceSettingsFallback.open(app,mapping);
            return;
        }
        final Handler handler=new Handler(Looper.getMainLooper());
        final AtomicBoolean completed=new AtomicBoolean(false);
        final Runnable timeout=()->{
            if(completed.compareAndSet(false,true)) DeviceSettingsFallback.open(app,mapping);
        };
        BluetoothProfile.ServiceListener listener=new BluetoothProfile.ServiceListener() {
            @Override public void onServiceConnected(int profile, BluetoothProfile proxy) {
                handler.removeCallbacks(timeout);
                if(!completed.compareAndSet(false,true)) {
                    adapter.closeProfileProxy(profile,proxy);
                    return;
                }
                boolean connected=false;
                boolean reliable=false;
                try {
                    List<BluetoothDevice> devices=proxy.getConnectedDevices();
                    reliable=true;
                    for(BluetoothDevice d:devices) {
                        if(d.getAddress().equals(mapping.address)) {connected=true;break;}
                    }
                }catch(RuntimeException exc) {
                    Log.w(TAG,"Unable to read connected A2DP devices",exc);
                }
                boolean disconnected=false;
                if(connected) {
                    try {
                        Method m=proxy.getClass().getMethod("disconnect",BluetoothDevice.class);
                        Object result=m.invoke(proxy,device);
                        disconnected=Boolean.TRUE.equals(result);
                    }catch(Exception exc) {
                        Log.w(TAG,"A2DP disconnect unavailable on this TCL firmware",exc);
                    }catch(LinkageError exc) {
                        Log.w(TAG,"A2DP disconnect API unavailable",exc);
                    }
                }
                try {
                    adapter.closeProfileProxy(profile,proxy);
                }catch(RuntimeException ignored) {}
                if(connected && disconnected) {
                    handler.postDelayed(()->BluetoothConnectAction.connectOrOpen(app,mapping),1500);
                }else if(connected || !reliable) {
                    DeviceSettingsFallback.open(app,mapping);
                }else {
                    BluetoothConnectAction.connectOrOpen(app,mapping);
                }
            }
            @Override public void onServiceDisconnected(int profile) {
                if(completed.compareAndSet(false,true)) {
                    handler.removeCallbacks(timeout);
                    DeviceSettingsFallback.open(app,mapping);
                }
            }
        };
        try {
            if(!adapter.getProfileProxy(app,listener,BluetoothProfile.A2DP)) {
                DeviceSettingsFallback.open(app,mapping);
                return;
            }
        }catch(RuntimeException error) {
            DeviceSettingsFallback.open(app,mapping);
            return;
        }
        handler.postDelayed(timeout,2700);
    }
}
