package tcl.shared;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

/**
 * Handles the installer-owned confirmation UI. Not exported to other apps.
 * No silent installation or "unknown sources" bypass.
 */
public final class TclInstallResultReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(
                PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirmation;
            if (Build.VERSION.SDK_INT >= 33) {
                confirmation = intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent.class);
            } else {
                confirmation = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            }
            if (confirmation == null) {
                Log.e("TclUpdater", "Missing confirmation activity");
                return;
            }
            confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(confirmation);
            } catch (RuntimeException e) {
                Log.e("TclUpdater", "Could not open Android update confirmation", e);
                Toast.makeText(context,
                        "Abra o instalador Android para confirmar a atualização.",
                        Toast.LENGTH_LONG).show();
            }
        } else if (status == PackageInstaller.STATUS_SUCCESS) {
            Toast.makeText(context, "Controle TCL atualizado.", Toast.LENGTH_LONG).show();
        } else {
            String reason = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
            Log.e("TclUpdater", "Installation rejected: " + status + " " + reason);
            Toast.makeText(context,
                    "Falha na atualização: " + (reason == null ? status : reason),
                    Toast.LENGTH_LONG).show();
        }
    }
}
