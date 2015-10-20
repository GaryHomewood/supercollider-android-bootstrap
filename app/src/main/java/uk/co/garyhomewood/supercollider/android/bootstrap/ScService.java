package uk.co.garyhomewood.supercollider.android.bootstrap;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by garyhomewood on 11/10/15.
 */
public class ScService extends Service {

    public static final String SYNTHDEF = "default.scsyndef";
    public static String dataDirStr;
    // TODO BOOTSTRAP: add your package name here and/or find a better way of doing this
    public static final String dllDirStr = "/data/data/uk.co.garyhomewood.supercollider.android.bootstrap/lib";

    /**
     * Our AIDL implementation to allow a bound Activity to talk to us
     */
    private final ISuperCollider.Stub mBinder = new ISuperCollider.Stub() {
        //@Override
        public void start() throws RemoteException {
            ScService.this.start();
        }
        //@Override
        public void stop() throws RemoteException {
            ScService.this.stop();
        }
        //@Override
        public void sendMessage(OscMessage oscMessage) throws RemoteException {
            ScService.this.audioThread.sendMessage(oscMessage);
        }
        public void openUDP(int port) throws RemoteException {
            ScService.this.audioThread.openUDP(port);
        }
        public void closeUDP() throws RemoteException {
            ScService.this.audioThread.closeUDP();
        }
        public void sendQuit() throws RemoteException {
            ScService.this.audioThread.sendQuit();
        }

    };

    private int NOTIFICATION_ID = 1;
    private SCAudio audioThread;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void start() {
        if (audioThread == null || !audioThread.isRunning() ) {
            audioThread = new SCAudio(dllDirStr);
            audioThread.start();
        }
    }

    @Override
    public void onCreate() {
        File appFilesDir = getExternalFilesDir(null);
        if (appFilesDir != null) {
            dataDirStr = appFilesDir.getPath();
            deliverDefaultSynthDefs();
        } else {
            Log.e(SCAudio.TAG, "Could not load synthdefs to " + dataDirStr);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int START_STICKY = 1;
        try {
            // Android 2.1 API allows us to specify that this service is a foreground task
            Notification notification = new Notification(R.drawable.icon,
                    getText(R.string.app_name), System.currentTimeMillis());
            Class<?> superClass = super.getClass();
            Method startForeground = superClass.getMethod("startForeground",
                    new Class[] {
                            int.class,
                            Class.forName("android.app.Notification")
                    }
            );
            Field startStickyValue = superClass.getField("START_STICKY");
            START_STICKY=startStickyValue.getInt(null);
            startForeground.invoke(this, new Object[] {
                            NOTIFICATION_ID,
                            notification}
            );
        } catch (Exception nsme) {
            // We can't get the newer methods
        }
        return START_STICKY;
    }

    public void stop() {
        try {
            mBinder.sendQuit();
        } catch (RemoteException re) {
            re.printStackTrace();
        }
        while(!audioThread.isEnded()){
            try{
                Thread.sleep(50L);
            }catch(InterruptedException err){
                err.printStackTrace();
                break;
            }
        }
    }

    // Called by Android API when not the front app any more. For this one we'll quit
    @Override
    public void onDestroy(){
        stop();
        super.onDestroy();
    }

    /**
     * Copies the default synth defs out, ScService calls it the first time the supercollider
     * data dir is created.
     */
    public void deliverDefaultSynthDefs() {
        try {
            InputStream is = getAssets().open(SYNTHDEF);
            File outputFile = new File(getExternalFilesDir(null), SYNTHDEF);
            FileOutputStream os = new FileOutputStream(outputFile);

            byte[] buf = new byte[1024];
            int bytesRead = 0;
            while (-1 != (bytesRead = is.read(buf))) {
                os.write(buf,0,bytesRead);
            }
            is.close();
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
