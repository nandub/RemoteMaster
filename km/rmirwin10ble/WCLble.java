// ------------------------------------------------------------------------------
//  <autogenerated>
//      This code was generated by jni4net. See http://jni4net.sourceforge.net/ 
// 
//      Changes to this file may cause incorrect behavior and will be lost if 
//      the code is regenerated.
//  </autogenerated>
// ------------------------------------------------------------------------------

package rmirwin10ble;

@net.sf.jni4net.attributes.ClrType
public class WCLble extends system.Object implements rmirwin10ble.IBleInterface {
    
    //<generated-proxy>
    private static system.Type staticType;
    
    protected WCLble(net.sf.jni4net.inj.INJEnv __env, long __handle) {
            super(__env, __handle);
    }
    
    @net.sf.jni4net.attributes.ClrConstructor("()V")
    public WCLble() {
            super(((net.sf.jni4net.inj.INJEnv)(null)), 0);
        rmirwin10ble.WCLble.__ctorWCLble0(this);
    }
    
    @net.sf.jni4net.attributes.ClrMethod("()V")
    private native static void __ctorWCLble0(net.sf.jni4net.inj.IClrProxy thiz);
    
    @net.sf.jni4net.attributes.ClrMethod("(LSystem/String;)LSystem/String;")
    public native java.lang.String ConnectBLE(java.lang.String portName);
    
    @net.sf.jni4net.attributes.ClrMethod("()V")
    public native void DisconnectBLE();
    
    @net.sf.jni4net.attributes.ClrMethod("(Z)V")
    public native void DiscoverUEI(boolean start);
    
    @net.sf.jni4net.attributes.ClrMethod("(LSystem/String;)Z")
    public native boolean ConnectUEI(java.lang.String address);
    
    @net.sf.jni4net.attributes.ClrMethod("()LSystem/String;")
    public native java.lang.String DisconnectUEI();
    
    @net.sf.jni4net.attributes.ClrMethod("()Z")
    public native boolean DiscoverServices();
    
    @net.sf.jni4net.attributes.ClrMethod("()Z")
    public native boolean GetFeatures();
    
    @net.sf.jni4net.attributes.ClrMethod("()LSystem/String;")
    public native java.lang.String GetSubscription();
    
    @net.sf.jni4net.attributes.ClrMethod("()I")
    public native int GetStage();
    
    @net.sf.jni4net.attributes.ClrMethod("(Z)V")
    public native void SetDisconnecting(boolean disconnecting);
    
    @net.sf.jni4net.attributes.ClrMethod("()Z")
    public native boolean IsDisconnecting();
    
    @net.sf.jni4net.attributes.ClrMethod("()Z")
    public native boolean IsScanning();
    
    @net.sf.jni4net.attributes.ClrMethod("()Z")
    public native boolean IsConnected();
    
    @net.sf.jni4net.attributes.ClrMethod("()Z")
    public native boolean HasCCCD();
    
    @net.sf.jni4net.attributes.ClrMethod("()Z")
    public native boolean NeedsCCCD();
    
    @net.sf.jni4net.attributes.ClrMethod("()I")
    public native int GetListSize();
    
    @net.sf.jni4net.attributes.ClrMethod("(I)LSystem/String;")
    public native java.lang.String GetListItem(int ndx);
    
    @net.sf.jni4net.attributes.ClrMethod("(I)LSystem/String;")
    public native java.lang.String GetItemName(int ndx);
    
    @net.sf.jni4net.attributes.ClrMethod("(I)I")
    public native int GetRssi(int ndx);
    
    @net.sf.jni4net.attributes.ClrMethod("()I")
    public native int GetInCount();
    
    @net.sf.jni4net.attributes.ClrMethod("()I")
    public native int GetInDataSize();
    
    @net.sf.jni4net.attributes.ClrMethod("(I)[B")
    public native byte[] GetInData(int ndx);
    
    @net.sf.jni4net.attributes.ClrMethod("(LSystem/String;)[B")
    public native byte[] ReadUserDescription(java.lang.String uuid);
    
    @net.sf.jni4net.attributes.ClrMethod("()I")
    public native int ReadSignalStrength();
    
    @net.sf.jni4net.attributes.ClrMethod("([B)V")
    public native void WritePacket(byte[] pkt);
    
    @net.sf.jni4net.attributes.ClrMethod("()I")
    public native int GetSentState();
    
    @net.sf.jni4net.attributes.ClrMethod("(I)V")
    public native void SetSentState(int state);
    
    @net.sf.jni4net.attributes.ClrMethod("(IIII)V")
    public native void UpdateConnection(int interval_min, int interval_max, int latency, int timeout);
    
    @net.sf.jni4net.attributes.ClrMethod("()LSystem/String;")
    public native java.lang.String GetBLEStack();
    
    public static system.Type typeof() {
        return rmirwin10ble.WCLble.staticType;
    }
    
    private static void InitJNI(net.sf.jni4net.inj.INJEnv env, system.Type staticType) {
        rmirwin10ble.WCLble.staticType = staticType;
    }
    //</generated-proxy>
}
