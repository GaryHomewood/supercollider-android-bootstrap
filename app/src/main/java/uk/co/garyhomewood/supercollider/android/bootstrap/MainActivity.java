package uk.co.garyhomewood.supercollider.android.bootstrap;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private ServiceConnection conn = new ScServiceConnection();
    private ISuperCollider.Stub superCollider;
    private TextView mainWidget = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainWidget = new TextView(this);
        setContentView(mainWidget);

        // Request the audio engine
        Intent i = new Intent(this, ScService.class);
        bindService(i, conn, BIND_AUTO_CREATE);

        mainWidget.setTypeface(Typeface.MONOSPACE);
        mainWidget.setTextSize(14);
        mainWidget.setText("Welcome to a SuperCollider-Android instrument!\n"
                        + "Y axis is volume, X axis is pitch\n"
                        + "\n"
                        + "                  ~+I777?                \n"
                        + "           :++?I77I====~?I7I             \n"
                        + "     ~=~+I77I??===+?IIII++~+?7           \n"
                        + " 77I~?777+===+?????+++?+II??~==7 ,      \n"
                        + " 7777 I~=II7?++=?+????++=+?IIII~~~+7:   \n"
                        + " I7=I?777 ~~+?7I?+==++?II?++=~~+I7I+++  \n"
                        + " ?7~, ~?=+777~~=+7IIII+~~=+??++++++,,+  \n"
                        + " ?7~      =~+ 77~~~~~=++++++=:,,,,  :+  \n"
                        + " +7= ??=~=      77++++==~~~:,   ,:, ,=  \n"
                        + " +7= +?+???+?=,, 7++:     ,:~~~===, ,=  \n"
                        + " =7= ++=+==???I, I=+   ,,:~=====~=, ,~  \n"
                        + " ~7+ =+=     +I: ?=+,  ==~~     ~=: ,~  \n"
                        + " ~7? ~+=  =~ +I: ?~+,  ~~       ~=: ,:  \n"
                        + " :7? ~++  == =I~ +~+:  ~~   ~::  =~ ,:  \n"
                        + " :7?  +++++= =I~ +~+:  :~   ~~:  =~ ,:  \n"
                        + " ,7I=    =~~ ~I= =:+:  :=~~~~~:  =~  ,  \n"
                        + "  7III~~      I+ ~:+~  :=~~~     ==  ,  \n"
                        + "    I??7II=+=,I+ ~,+~       :~~====     \n"
                        + "        ++=7II?I? :,+=  ,:::~=+==+=:     \n"
                        + "        ,  =~:7I? , +=~=++++=~::,,,      \n"
                        + "              :,  , ++===~~~,            \n"
                        + "              ,     +~     ,             \n"
                        + "                 ,                       \n"
        );
    }

    /**
     * Get a SuperCollider service.
     */
    private class ScServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName name, IBinder service) {
            MainActivity.this.superCollider = (ISuperCollider.Stub) service;
            try {
                // Kick off the supercollider playback routine
                superCollider.start();

                // Start a synth playing
                superCollider.sendMessage(OscMessage.createSynthMessage("default", OscMessage.defaultNodeId, 0, 1));

                // now we have an audio engine, let the activity hook up its controls
                setUpControls();
            } catch (RemoteException re) {
                re.printStackTrace();
            }
        }

        public void onServiceDisconnected(ComponentName name) {
        }
    }

    public void setUpControls() {
        if (mainWidget != null) {
            mainWidget.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction()==MotionEvent.ACTION_UP) {

                        OscMessage noteMessage = new OscMessage( new Object[] {
                                "/n_set", OscMessage.defaultNodeId, "amp", 0f
                        });

                        try {
                            // Now send it over the interprocess link to SuperCollider running as a Service
                            superCollider.sendMessage(noteMessage);
                        } catch (RemoteException e) {
                            Toast.makeText(MainActivity.this, "Failed to communicate with SuperCollider!", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    } else if ((event.getAction() == MotionEvent.ACTION_DOWN)
                            || (event.getAction() == MotionEvent.ACTION_MOVE)) {

                        float vol = 1f - event.getY()/mainWidget.getHeight();

                        OscMessage noteMessage = new OscMessage( new Object[] {
                                "/n_set", OscMessage.defaultNodeId, "amp", vol
                        });

                        // float freq = 150+event.getX();
                        // 0 to mainWidget.getWidth() becomes sane-ish range of midinotes:
                        float midinote = event.getX() * (70.f / mainWidget.getWidth()) + 28.f;
                        float freq = sc_midicps(Math.round(midinote));

                        OscMessage pitchMessage = new OscMessage( new Object[] {
                                "/n_set", OscMessage.defaultNodeId, "freq", freq
                        });

                        try {
                            superCollider.sendMessage(noteMessage);
                            superCollider.sendMessage(pitchMessage);
                        } catch (RemoteException e) {
                            Toast.makeText(MainActivity.this, "Failed to communicate with SuperCollider!", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    }
                    return true;
                }
            });
        }

        try {
            superCollider.openUDP(57110);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Convert midi notes to floating point pitches - based on sc_midicps in the SC C++ code
     */
    float sc_midicps(float note) {
        return (float) (440.0 * Math.pow((float)2., (note - 69.0) * (float)0.083333333333));
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            // Free up audio when the activity is not in the foreground
            if (superCollider!=null) superCollider.stop();
            this.finish();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            // Free up audio when the activity is not in the foreground
            if (superCollider!=null) superCollider.stop();
            this.finish();
        } catch (RemoteException re) {
            re.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            superCollider.closeUDP();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        unbindService(conn);
    }
}