package uk.co.garyhomewood.supercollider.android.bootstrap;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.LinkedList;

/**
 * Created by garyhomewood on 11/10/15.
 */
public class OscMessage implements Parcelable {

    public static final int defaultNodeId = 999;

    ///////////////////////////////////////////////////////////////////////////
    // Static templates for common operations
    ///////////////////////////////////////////////////////////////////////////

    public static OscMessage createGroupMessage(int nodeId, int addAction, int target) {
        OscMessage theMessage = new OscMessage();
        theMessage.add("/g_new");
        theMessage.add(nodeId);
        theMessage.add(addAction);
        theMessage.add(target);
        return theMessage;
    }

    public static OscMessage createSynthMessage(String name, int nodeId, int addAction, int target) {
        OscMessage theMessage = new OscMessage();
        theMessage.add("/s_new");
        theMessage.add(name);
        theMessage.add(nodeId);
        theMessage.add(addAction);
        theMessage.add(target);
        return theMessage;
    }

    // TODO: This message seems to be getting parsed by doOsc, but it
    // doesn't affect the output.  What's that all about then eh?
    public static OscMessage noteMessage(int note, int velocity) {
        OscMessage retval =  new OscMessage();

        OscMessage notebundle = new OscMessage();
        notebundle.add("/n_set");
        notebundle.add(defaultNodeId);
        notebundle.add("/note");
        notebundle.add(note);

        OscMessage velbundle = new OscMessage();
        velbundle.add("/n_set");
        velbundle.add(defaultNodeId);
        velbundle.add("/velocity");
        velbundle.add(velocity);

        retval.add(notebundle);
        retval.add(velbundle);
        return retval;
    }

    public static OscMessage setControl(int node,String control,float value) {
        OscMessage controlValue = new OscMessage();
        controlValue.add("n_set");
        controlValue.add(node);
        controlValue.add(control);
        controlValue.add(value);
        return controlValue;
    }

    /*
     * NOTE: it is better not to send a plain quit() message yourself,
     * instead call SCAudio.sendQuit() which tidies up the java part of the audio too.
     */
    public static OscMessage quitMessage() {
        OscMessage theMessage = new OscMessage();
        theMessage.add("/quit");
        return theMessage;
    }

    ///////////////////////////////////////////////////////////////////////////
    // The actual OscMessage implementation
    ///////////////////////////////////////////////////////////////////////////

    private LinkedList<Object> message = new LinkedList<Object>();
    /*
     * Creates an empty OscMessage
     */
    public OscMessage() {}
    /*
     * Convenience constructor for creating a whole message in a oner
     */
    public OscMessage(Object[] message) {
        for (Object token : message) {
            if (token instanceof Integer) add ((Integer) token);
            else if (token instanceof Float) add((Float) token);
            else if (token instanceof Long) add((Long) token);
            else if (token instanceof String) add ((String) token);
        }
    }
    public boolean add(int i) { return message.add(i); }
    public boolean add(float f) { return message.add(f); }
    public boolean add(String s) { return message.add(s); }
    public boolean add(long ii) {return message.add(ii); }
    public boolean add(OscMessage m) {return message.add(m);}
    public Object[] toArray() { return message.toArray(); }
    public Object get(int location) {
        return message.get(location);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Parcelling code for AIDL
    ///////////////////////////////////////////////////////////////////////////

    public static final Creator<OscMessage> CREATOR = new Creator<OscMessage>() {
        //@Override
        public OscMessage createFromParcel(Parcel source) {
            OscMessage retval = new OscMessage();
            source.readList(retval.message, null);
            return retval;
        }

        //@Override
        public OscMessage[] newArray(int size) {
            OscMessage[] retval = new OscMessage[size];
            for(int i = 0; i<size;++i) retval[i] = new OscMessage();
            return retval;
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }
}
