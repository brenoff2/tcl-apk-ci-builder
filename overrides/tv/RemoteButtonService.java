package dk.fez.bluetoothquickconnect;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.util.Log;

/**
 * Only uses remote color keys; does not inspect screen content or send data.
 * Red = recover headset; blue = launch/chose IPTV; yellow = change IPTV.
 */
public final class RemoteButtonService extends AccessibilityService {
    private TvAppBridge appBridge;
    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        appBridge = new TvAppBridge(this);
        appBridge.start();
        AccessibilityServiceInfo info=getServiceInfo();
        if(info!=null) {
            info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            setServiceInfo(info);
        }
    }
    @Override protected boolean onKeyEvent(KeyEvent event) {
        if(event.getAction()!=KeyEvent.ACTION_UP) return false;
        int key=event.getKeyCode();
        if(key==KeyEvent.KEYCODE_PROG_BLUE) {
            IptvAppsActivity.launchSavedOrChoose(this);
            return true;
        }
        if(key==KeyEvent.KEYCODE_PROG_YELLOW) {
            IptvAppsActivity.openChooser(this);
            return true;
        }
        if(key==KeyEvent.KEYCODE_PROG_RED) {
            MappingStore.DeviceMapping mapping=MappingStore.get(this,key);
            if(mapping==null) {
                Intent setup=new Intent(this,MainActivity.class);
                setup.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(setup);
            } else {
                BluetoothRecoverAction.recoverOrOpen(this,mapping);
            }
            return true;
        }
        return false;
    }
    @Override public void onDestroy() {
        if (appBridge != null) appBridge.stop();
        super.onDestroy();
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent e) { }
    @Override public void onInterrupt() { }
}
