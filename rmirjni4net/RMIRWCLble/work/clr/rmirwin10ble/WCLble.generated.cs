//------------------------------------------------------------------------------
// <auto-generated>
//     This code was generated by jni4net. See http://jni4net.sourceforge.net/ 
//     Runtime Version:4.0.30319.42000
//
//     Changes to this file may cause incorrect behavior and will be lost if
//     the code is regenerated.
// </auto-generated>
//------------------------------------------------------------------------------

namespace RMIRWin10BLE {
    
    
    #region Component Designer generated code 
    public partial class WCLble_ {
        
        public static global::java.lang.Class _class {
            get {
                return global::RMIRWin10BLE.@__WCLble.staticClass;
            }
        }
    }
    #endregion
    
    #region Component Designer generated code 
    [global::net.sf.jni4net.attributes.JavaProxyAttribute(typeof(global::RMIRWin10BLE.WCLble), typeof(global::RMIRWin10BLE.WCLble_))]
    [global::net.sf.jni4net.attributes.ClrWrapperAttribute(typeof(global::RMIRWin10BLE.WCLble), typeof(global::RMIRWin10BLE.WCLble_))]
    internal sealed partial class @__WCLble : global::java.lang.Object {
        
        internal new static global::java.lang.Class staticClass;
        
        private @__WCLble(global::net.sf.jni4net.jni.JNIEnv @__env) : 
                base(@__env) {
        }
        
        private static void InitJNI(global::net.sf.jni4net.jni.JNIEnv @__env, java.lang.Class @__class) {
            global::RMIRWin10BLE.@__WCLble.staticClass = @__class;
        }
        
        private static global::System.Collections.Generic.List<global::net.sf.jni4net.jni.JNINativeMethod> @__Init(global::net.sf.jni4net.jni.JNIEnv @__env, global::java.lang.Class @__class) {
            global::System.Type @__type = typeof(__WCLble);
            global::System.Collections.Generic.List<global::net.sf.jni4net.jni.JNINativeMethod> methods = new global::System.Collections.Generic.List<global::net.sf.jni4net.jni.JNINativeMethod>();
            methods.Add(global::net.sf.jni4net.jni.JNINativeMethod.Create(@__type, "ConnectBLE", "ConnectBLE0", "(Ljava/lang/String;)Ljava/lang/String;"));
            methods.Add(global::net.sf.jni4net.jni.JNINativeMethod.Create(@__type, "DisconnectBLE", "DisconnectBLE1", "()V"));
            methods.Add(global::net.sf.jni4net.jni.JNINativeMethod.Create(@__type, "DiscoverUEI", "DiscoverUEI2", "(Z)V"));
            methods.Add(global::net.sf.jni4net.jni.JNINativeMethod.Create(@__type, "ConnectUEI", "ConnectUEI3", "(Ljava/lang/String;)Z"));
            methods.Add(global::net.sf.jni4net.jni.JNINativeMethod.Create(@__type, "DisconnectUEI", "DisconnectUEI4", "()Ljava/lang/String;"));
            methods.Add(global::net.sf.jni4net.jni.JNINativeMethod.Create(@__type, "DiscoverServices", "DiscoverServices5", "()Z"));
            methods.Add(global::net.sf.jni4net.jni.JNINativeMethod.Create(@__type, "GetFeatures", "GetFeatures6", "()Z"));
            methods.Add(global::net.sf.jni4net.jni.JNINativeMethod.Create(@__type, "GetSubscription", "GetSubscription7", "()Ljava/lang/String;"));
            methods.Add(global::net.sf.jni4net.jni.JNINativeMethod.Create(@__type, "GetStage", "GetStage8", "()I"));
            methods.Add(global::net.sf.jni4net.jni.JNINativeMethod.Create(@__type, "SetDisconnecting", "SetDisconnecting9", "(Z)V"));
            methods.Add(global::net.sf.jni4net.jni.JNINativeMethod.Create(@__type, "IsDisconnecting", "IsDisconnecting10", "()Z"));
            methods.Add(global::net.sf.jni4net.jni.JNINativeMethod.Create(@__type, "IsScanning", "IsScanning11", "()Z"));
            methods.Add(global::net.sf.jni4net.jni.JNINativeMethod.Create(@__type, "IsConnected", "IsConnected12", "()Z"));
            methods.Add(global::net.sf.jni4net.jni.JNINativeMethod.Create(@__type, "HasCCCD", "HasCCCD13", "()Z"));
            methods.Add(global::net.sf.jni4net.jni.JNINativeMethod.Create(@__type, "NeedsCCCD", "NeedsCCCD14", "()Z"));
            methods.Add(global::net.sf.jni4net.jni.JNINativeMethod.Create(@__type, "GetListSize", "GetListSize15", "()I"));
            methods.Add(global::net.sf.jni4net.jni.JNINativeMethod.Create(@__type, "GetListItem", "GetListItem16", "(I)Ljava/lang/String;"));
            methods.Add(global::net.sf.jni4net.jni.JNINativeMethod.Create(@__type, "GetItemName", "GetItemName17", "(I)Ljava/lang/String;"));
            methods.Add(global::net.sf.jni4net.jni.JNINativeMethod.Create(@__type, "GetRssi", "GetRssi18", "(I)I"));
            methods.Add(global::net.sf.jni4net.jni.JNINativeMethod.Create(@__type, "GetInCount", "GetInCount19", "()I"));
            methods.Add(global::net.sf.jni4net.jni.JNINativeMethod.Create(@__type, "GetInDataSize", "GetInDataSize20", "()I"));
            methods.Add(global::net.sf.jni4net.jni.JNINativeMethod.Create(@__type, "GetInData", "GetInData21", "(I)[B"));
            methods.Add(global::net.sf.jni4net.jni.JNINativeMethod.Create(@__type, "ReadSignalStrength", "ReadSignalStrength22", "()I"));
            methods.Add(global::net.sf.jni4net.jni.JNINativeMethod.Create(@__type, "WritePacket", "WritePacket23", "([B)V"));
            methods.Add(global::net.sf.jni4net.jni.JNINativeMethod.Create(@__type, "GetSentState", "GetSentState24", "()I"));
            methods.Add(global::net.sf.jni4net.jni.JNINativeMethod.Create(@__type, "SetSentState", "SetSentState25", "(I)V"));
            methods.Add(global::net.sf.jni4net.jni.JNINativeMethod.Create(@__type, "UpdateConnection", "UpdateConnection26", "(IIII)V"));
            methods.Add(global::net.sf.jni4net.jni.JNINativeMethod.Create(@__type, "GetBLEStack", "GetBLEStack27", "()Ljava/lang/String;"));
            methods.Add(global::net.sf.jni4net.jni.JNINativeMethod.Create(@__type, "__ctorWCLble0", "__ctorWCLble0", "(Lnet/sf/jni4net/inj/IClrProxy;)V"));
            return methods;
        }
        
        private static global::net.sf.jni4net.utils.JniHandle ConnectBLE0(global::System.IntPtr @__envp, global::net.sf.jni4net.utils.JniLocalHandle @__obj, global::net.sf.jni4net.utils.JniLocalHandle portName) {
            // (Ljava/lang/String;)Ljava/lang/String;
            // (LSystem/String;)LSystem/String;
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.Wrap(@__envp);
            global::net.sf.jni4net.utils.JniHandle @__return = default(global::net.sf.jni4net.utils.JniHandle);
            try {
            global::RMIRWin10BLE.WCLble @__real = global::net.sf.jni4net.utils.Convertor.StrongJp2C<global::RMIRWin10BLE.WCLble>(@__env, @__obj);
            @__return = global::net.sf.jni4net.utils.Convertor.StrongC2JString(@__env, ((global::RMIRWin10BLE.IBleInterface)(@__real)).ConnectBLE(global::net.sf.jni4net.utils.Convertor.StrongJ2CString(@__env, portName)));
            }catch (global::System.Exception __ex){@__env.ThrowExisting(__ex);}
            return @__return;
        }
        
        private static void DisconnectBLE1(global::System.IntPtr @__envp, global::net.sf.jni4net.utils.JniLocalHandle @__obj) {
            // ()V
            // ()V
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.Wrap(@__envp);
            try {
            global::RMIRWin10BLE.WCLble @__real = global::net.sf.jni4net.utils.Convertor.StrongJp2C<global::RMIRWin10BLE.WCLble>(@__env, @__obj);
            ((global::RMIRWin10BLE.IBleInterface)(@__real)).DisconnectBLE();
            }catch (global::System.Exception __ex){@__env.ThrowExisting(__ex);}
        }
        
        private static void DiscoverUEI2(global::System.IntPtr @__envp, global::net.sf.jni4net.utils.JniLocalHandle @__obj, bool start) {
            // (Z)V
            // (Z)V
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.Wrap(@__envp);
            try {
            global::RMIRWin10BLE.WCLble @__real = global::net.sf.jni4net.utils.Convertor.StrongJp2C<global::RMIRWin10BLE.WCLble>(@__env, @__obj);
            ((global::RMIRWin10BLE.IBleInterface)(@__real)).DiscoverUEI(start);
            }catch (global::System.Exception __ex){@__env.ThrowExisting(__ex);}
        }
        
        private static bool ConnectUEI3(global::System.IntPtr @__envp, global::net.sf.jni4net.utils.JniLocalHandle @__obj, global::net.sf.jni4net.utils.JniLocalHandle address) {
            // (Ljava/lang/String;)Z
            // (LSystem/String;)Z
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.Wrap(@__envp);
            bool @__return = default(bool);
            try {
            global::RMIRWin10BLE.WCLble @__real = global::net.sf.jni4net.utils.Convertor.StrongJp2C<global::RMIRWin10BLE.WCLble>(@__env, @__obj);
            @__return = ((bool)(((global::RMIRWin10BLE.IBleInterface)(@__real)).ConnectUEI(global::net.sf.jni4net.utils.Convertor.StrongJ2CString(@__env, address))));
            }catch (global::System.Exception __ex){@__env.ThrowExisting(__ex);}
            return @__return;
        }
        
        private static global::net.sf.jni4net.utils.JniHandle DisconnectUEI4(global::System.IntPtr @__envp, global::net.sf.jni4net.utils.JniLocalHandle @__obj) {
            // ()Ljava/lang/String;
            // ()LSystem/String;
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.Wrap(@__envp);
            global::net.sf.jni4net.utils.JniHandle @__return = default(global::net.sf.jni4net.utils.JniHandle);
            try {
            global::RMIRWin10BLE.WCLble @__real = global::net.sf.jni4net.utils.Convertor.StrongJp2C<global::RMIRWin10BLE.WCLble>(@__env, @__obj);
            @__return = global::net.sf.jni4net.utils.Convertor.StrongC2JString(@__env, ((global::RMIRWin10BLE.IBleInterface)(@__real)).DisconnectUEI());
            }catch (global::System.Exception __ex){@__env.ThrowExisting(__ex);}
            return @__return;
        }
        
        private static bool DiscoverServices5(global::System.IntPtr @__envp, global::net.sf.jni4net.utils.JniLocalHandle @__obj) {
            // ()Z
            // ()Z
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.Wrap(@__envp);
            bool @__return = default(bool);
            try {
            global::RMIRWin10BLE.WCLble @__real = global::net.sf.jni4net.utils.Convertor.StrongJp2C<global::RMIRWin10BLE.WCLble>(@__env, @__obj);
            @__return = ((bool)(((global::RMIRWin10BLE.IBleInterface)(@__real)).DiscoverServices()));
            }catch (global::System.Exception __ex){@__env.ThrowExisting(__ex);}
            return @__return;
        }
        
        private static bool GetFeatures6(global::System.IntPtr @__envp, global::net.sf.jni4net.utils.JniLocalHandle @__obj) {
            // ()Z
            // ()Z
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.Wrap(@__envp);
            bool @__return = default(bool);
            try {
            global::RMIRWin10BLE.WCLble @__real = global::net.sf.jni4net.utils.Convertor.StrongJp2C<global::RMIRWin10BLE.WCLble>(@__env, @__obj);
            @__return = ((bool)(((global::RMIRWin10BLE.IBleInterface)(@__real)).GetFeatures()));
            }catch (global::System.Exception __ex){@__env.ThrowExisting(__ex);}
            return @__return;
        }
        
        private static global::net.sf.jni4net.utils.JniHandle GetSubscription7(global::System.IntPtr @__envp, global::net.sf.jni4net.utils.JniLocalHandle @__obj) {
            // ()Ljava/lang/String;
            // ()LSystem/String;
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.Wrap(@__envp);
            global::net.sf.jni4net.utils.JniHandle @__return = default(global::net.sf.jni4net.utils.JniHandle);
            try {
            global::RMIRWin10BLE.WCLble @__real = global::net.sf.jni4net.utils.Convertor.StrongJp2C<global::RMIRWin10BLE.WCLble>(@__env, @__obj);
            @__return = global::net.sf.jni4net.utils.Convertor.StrongC2JString(@__env, ((global::RMIRWin10BLE.IBleInterface)(@__real)).GetSubscription());
            }catch (global::System.Exception __ex){@__env.ThrowExisting(__ex);}
            return @__return;
        }
        
        private static int GetStage8(global::System.IntPtr @__envp, global::net.sf.jni4net.utils.JniLocalHandle @__obj) {
            // ()I
            // ()I
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.Wrap(@__envp);
            int @__return = default(int);
            try {
            global::RMIRWin10BLE.WCLble @__real = global::net.sf.jni4net.utils.Convertor.StrongJp2C<global::RMIRWin10BLE.WCLble>(@__env, @__obj);
            @__return = ((int)(((global::RMIRWin10BLE.IBleInterface)(@__real)).GetStage()));
            }catch (global::System.Exception __ex){@__env.ThrowExisting(__ex);}
            return @__return;
        }
        
        private static void SetDisconnecting9(global::System.IntPtr @__envp, global::net.sf.jni4net.utils.JniLocalHandle @__obj, bool disconnecting) {
            // (Z)V
            // (Z)V
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.Wrap(@__envp);
            try {
            global::RMIRWin10BLE.WCLble @__real = global::net.sf.jni4net.utils.Convertor.StrongJp2C<global::RMIRWin10BLE.WCLble>(@__env, @__obj);
            ((global::RMIRWin10BLE.IBleInterface)(@__real)).SetDisconnecting(disconnecting);
            }catch (global::System.Exception __ex){@__env.ThrowExisting(__ex);}
        }
        
        private static bool IsDisconnecting10(global::System.IntPtr @__envp, global::net.sf.jni4net.utils.JniLocalHandle @__obj) {
            // ()Z
            // ()Z
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.Wrap(@__envp);
            bool @__return = default(bool);
            try {
            global::RMIRWin10BLE.WCLble @__real = global::net.sf.jni4net.utils.Convertor.StrongJp2C<global::RMIRWin10BLE.WCLble>(@__env, @__obj);
            @__return = ((bool)(((global::RMIRWin10BLE.IBleInterface)(@__real)).IsDisconnecting()));
            }catch (global::System.Exception __ex){@__env.ThrowExisting(__ex);}
            return @__return;
        }
        
        private static bool IsScanning11(global::System.IntPtr @__envp, global::net.sf.jni4net.utils.JniLocalHandle @__obj) {
            // ()Z
            // ()Z
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.Wrap(@__envp);
            bool @__return = default(bool);
            try {
            global::RMIRWin10BLE.WCLble @__real = global::net.sf.jni4net.utils.Convertor.StrongJp2C<global::RMIRWin10BLE.WCLble>(@__env, @__obj);
            @__return = ((bool)(((global::RMIRWin10BLE.IBleInterface)(@__real)).IsScanning()));
            }catch (global::System.Exception __ex){@__env.ThrowExisting(__ex);}
            return @__return;
        }
        
        private static bool IsConnected12(global::System.IntPtr @__envp, global::net.sf.jni4net.utils.JniLocalHandle @__obj) {
            // ()Z
            // ()Z
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.Wrap(@__envp);
            bool @__return = default(bool);
            try {
            global::RMIRWin10BLE.WCLble @__real = global::net.sf.jni4net.utils.Convertor.StrongJp2C<global::RMIRWin10BLE.WCLble>(@__env, @__obj);
            @__return = ((bool)(((global::RMIRWin10BLE.IBleInterface)(@__real)).IsConnected()));
            }catch (global::System.Exception __ex){@__env.ThrowExisting(__ex);}
            return @__return;
        }
        
        private static bool HasCCCD13(global::System.IntPtr @__envp, global::net.sf.jni4net.utils.JniLocalHandle @__obj) {
            // ()Z
            // ()Z
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.Wrap(@__envp);
            bool @__return = default(bool);
            try {
            global::RMIRWin10BLE.WCLble @__real = global::net.sf.jni4net.utils.Convertor.StrongJp2C<global::RMIRWin10BLE.WCLble>(@__env, @__obj);
            @__return = ((bool)(((global::RMIRWin10BLE.IBleInterface)(@__real)).HasCCCD()));
            }catch (global::System.Exception __ex){@__env.ThrowExisting(__ex);}
            return @__return;
        }
        
        private static bool NeedsCCCD14(global::System.IntPtr @__envp, global::net.sf.jni4net.utils.JniLocalHandle @__obj) {
            // ()Z
            // ()Z
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.Wrap(@__envp);
            bool @__return = default(bool);
            try {
            global::RMIRWin10BLE.WCLble @__real = global::net.sf.jni4net.utils.Convertor.StrongJp2C<global::RMIRWin10BLE.WCLble>(@__env, @__obj);
            @__return = ((bool)(((global::RMIRWin10BLE.IBleInterface)(@__real)).NeedsCCCD()));
            }catch (global::System.Exception __ex){@__env.ThrowExisting(__ex);}
            return @__return;
        }
        
        private static int GetListSize15(global::System.IntPtr @__envp, global::net.sf.jni4net.utils.JniLocalHandle @__obj) {
            // ()I
            // ()I
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.Wrap(@__envp);
            int @__return = default(int);
            try {
            global::RMIRWin10BLE.WCLble @__real = global::net.sf.jni4net.utils.Convertor.StrongJp2C<global::RMIRWin10BLE.WCLble>(@__env, @__obj);
            @__return = ((int)(((global::RMIRWin10BLE.IBleInterface)(@__real)).GetListSize()));
            }catch (global::System.Exception __ex){@__env.ThrowExisting(__ex);}
            return @__return;
        }
        
        private static global::net.sf.jni4net.utils.JniHandle GetListItem16(global::System.IntPtr @__envp, global::net.sf.jni4net.utils.JniLocalHandle @__obj, int ndx) {
            // (I)Ljava/lang/String;
            // (I)LSystem/String;
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.Wrap(@__envp);
            global::net.sf.jni4net.utils.JniHandle @__return = default(global::net.sf.jni4net.utils.JniHandle);
            try {
            global::RMIRWin10BLE.WCLble @__real = global::net.sf.jni4net.utils.Convertor.StrongJp2C<global::RMIRWin10BLE.WCLble>(@__env, @__obj);
            @__return = global::net.sf.jni4net.utils.Convertor.StrongC2JString(@__env, ((global::RMIRWin10BLE.IBleInterface)(@__real)).GetListItem(ndx));
            }catch (global::System.Exception __ex){@__env.ThrowExisting(__ex);}
            return @__return;
        }
        
        private static global::net.sf.jni4net.utils.JniHandle GetItemName17(global::System.IntPtr @__envp, global::net.sf.jni4net.utils.JniLocalHandle @__obj, int ndx) {
            // (I)Ljava/lang/String;
            // (I)LSystem/String;
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.Wrap(@__envp);
            global::net.sf.jni4net.utils.JniHandle @__return = default(global::net.sf.jni4net.utils.JniHandle);
            try {
            global::RMIRWin10BLE.WCLble @__real = global::net.sf.jni4net.utils.Convertor.StrongJp2C<global::RMIRWin10BLE.WCLble>(@__env, @__obj);
            @__return = global::net.sf.jni4net.utils.Convertor.StrongC2JString(@__env, ((global::RMIRWin10BLE.IBleInterface)(@__real)).GetItemName(ndx));
            }catch (global::System.Exception __ex){@__env.ThrowExisting(__ex);}
            return @__return;
        }
        
        private static int GetRssi18(global::System.IntPtr @__envp, global::net.sf.jni4net.utils.JniLocalHandle @__obj, int ndx) {
            // (I)I
            // (I)I
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.Wrap(@__envp);
            int @__return = default(int);
            try {
            global::RMIRWin10BLE.WCLble @__real = global::net.sf.jni4net.utils.Convertor.StrongJp2C<global::RMIRWin10BLE.WCLble>(@__env, @__obj);
            @__return = ((int)(((global::RMIRWin10BLE.IBleInterface)(@__real)).GetRssi(ndx)));
            }catch (global::System.Exception __ex){@__env.ThrowExisting(__ex);}
            return @__return;
        }
        
        private static int GetInCount19(global::System.IntPtr @__envp, global::net.sf.jni4net.utils.JniLocalHandle @__obj) {
            // ()I
            // ()I
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.Wrap(@__envp);
            int @__return = default(int);
            try {
            global::RMIRWin10BLE.WCLble @__real = global::net.sf.jni4net.utils.Convertor.StrongJp2C<global::RMIRWin10BLE.WCLble>(@__env, @__obj);
            @__return = ((int)(((global::RMIRWin10BLE.IBleInterface)(@__real)).GetInCount()));
            }catch (global::System.Exception __ex){@__env.ThrowExisting(__ex);}
            return @__return;
        }
        
        private static int GetInDataSize20(global::System.IntPtr @__envp, global::net.sf.jni4net.utils.JniLocalHandle @__obj) {
            // ()I
            // ()I
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.Wrap(@__envp);
            int @__return = default(int);
            try {
            global::RMIRWin10BLE.WCLble @__real = global::net.sf.jni4net.utils.Convertor.StrongJp2C<global::RMIRWin10BLE.WCLble>(@__env, @__obj);
            @__return = ((int)(((global::RMIRWin10BLE.IBleInterface)(@__real)).GetInDataSize()));
            }catch (global::System.Exception __ex){@__env.ThrowExisting(__ex);}
            return @__return;
        }
        
        private static global::net.sf.jni4net.utils.JniHandle GetInData21(global::System.IntPtr @__envp, global::net.sf.jni4net.utils.JniLocalHandle @__obj, int ndx) {
            // (I)[B
            // (I)[B
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.Wrap(@__envp);
            global::net.sf.jni4net.utils.JniHandle @__return = default(global::net.sf.jni4net.utils.JniHandle);
            try {
            global::RMIRWin10BLE.WCLble @__real = global::net.sf.jni4net.utils.Convertor.StrongJp2C<global::RMIRWin10BLE.WCLble>(@__env, @__obj);
            @__return = global::net.sf.jni4net.utils.Convertor.ArrayPrimC2J(@__env, ((global::RMIRWin10BLE.IBleInterface)(@__real)).GetInData(ndx));
            }catch (global::System.Exception __ex){@__env.ThrowExisting(__ex);}
            return @__return;
        }
        
        private static int ReadSignalStrength22(global::System.IntPtr @__envp, global::net.sf.jni4net.utils.JniLocalHandle @__obj) {
            // ()I
            // ()I
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.Wrap(@__envp);
            int @__return = default(int);
            try {
            global::RMIRWin10BLE.WCLble @__real = global::net.sf.jni4net.utils.Convertor.StrongJp2C<global::RMIRWin10BLE.WCLble>(@__env, @__obj);
            @__return = ((int)(((global::RMIRWin10BLE.IBleInterface)(@__real)).ReadSignalStrength()));
            }catch (global::System.Exception __ex){@__env.ThrowExisting(__ex);}
            return @__return;
        }
        
        private static void WritePacket23(global::System.IntPtr @__envp, global::net.sf.jni4net.utils.JniLocalHandle @__obj, global::net.sf.jni4net.utils.JniLocalHandle pkt) {
            // ([B)V
            // ([B)V
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.Wrap(@__envp);
            try {
            global::RMIRWin10BLE.WCLble @__real = global::net.sf.jni4net.utils.Convertor.StrongJp2C<global::RMIRWin10BLE.WCLble>(@__env, @__obj);
            ((global::RMIRWin10BLE.IBleInterface)(@__real)).WritePacket(global::net.sf.jni4net.utils.Convertor.ArrayPrimJ2Cbyte(@__env, pkt));
            }catch (global::System.Exception __ex){@__env.ThrowExisting(__ex);}
        }
        
        private static int GetSentState24(global::System.IntPtr @__envp, global::net.sf.jni4net.utils.JniLocalHandle @__obj) {
            // ()I
            // ()I
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.Wrap(@__envp);
            int @__return = default(int);
            try {
            global::RMIRWin10BLE.WCLble @__real = global::net.sf.jni4net.utils.Convertor.StrongJp2C<global::RMIRWin10BLE.WCLble>(@__env, @__obj);
            @__return = ((int)(((global::RMIRWin10BLE.IBleInterface)(@__real)).GetSentState()));
            }catch (global::System.Exception __ex){@__env.ThrowExisting(__ex);}
            return @__return;
        }
        
        private static void SetSentState25(global::System.IntPtr @__envp, global::net.sf.jni4net.utils.JniLocalHandle @__obj, int state) {
            // (I)V
            // (I)V
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.Wrap(@__envp);
            try {
            global::RMIRWin10BLE.WCLble @__real = global::net.sf.jni4net.utils.Convertor.StrongJp2C<global::RMIRWin10BLE.WCLble>(@__env, @__obj);
            ((global::RMIRWin10BLE.IBleInterface)(@__real)).SetSentState(state);
            }catch (global::System.Exception __ex){@__env.ThrowExisting(__ex);}
        }
        
        private static void UpdateConnection26(global::System.IntPtr @__envp, global::net.sf.jni4net.utils.JniLocalHandle @__obj, int interval_min, int interval_max, int latency, int timeout) {
            // (IIII)V
            // (IIII)V
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.Wrap(@__envp);
            try {
            global::RMIRWin10BLE.WCLble @__real = global::net.sf.jni4net.utils.Convertor.StrongJp2C<global::RMIRWin10BLE.WCLble>(@__env, @__obj);
            ((global::RMIRWin10BLE.IBleInterface)(@__real)).UpdateConnection(interval_min, interval_max, latency, timeout);
            }catch (global::System.Exception __ex){@__env.ThrowExisting(__ex);}
        }
        
        private static global::net.sf.jni4net.utils.JniHandle GetBLEStack27(global::System.IntPtr @__envp, global::net.sf.jni4net.utils.JniLocalHandle @__obj) {
            // ()Ljava/lang/String;
            // ()LSystem/String;
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.Wrap(@__envp);
            global::net.sf.jni4net.utils.JniHandle @__return = default(global::net.sf.jni4net.utils.JniHandle);
            try {
            global::RMIRWin10BLE.WCLble @__real = global::net.sf.jni4net.utils.Convertor.StrongJp2C<global::RMIRWin10BLE.WCLble>(@__env, @__obj);
            @__return = global::net.sf.jni4net.utils.Convertor.StrongC2JString(@__env, ((global::RMIRWin10BLE.IBleInterface)(@__real)).GetBLEStack());
            }catch (global::System.Exception __ex){@__env.ThrowExisting(__ex);}
            return @__return;
        }
        
        private static void @__ctorWCLble0(global::System.IntPtr @__envp, global::net.sf.jni4net.utils.JniLocalHandle @__class, global::net.sf.jni4net.utils.JniLocalHandle @__obj) {
            // ()V
            // ()V
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.Wrap(@__envp);
            try {
            global::RMIRWin10BLE.WCLble @__real = new global::RMIRWin10BLE.WCLble();
            global::net.sf.jni4net.utils.Convertor.InitProxy(@__env, @__obj, @__real);
            }catch (global::System.Exception __ex){@__env.ThrowExisting(__ex);}
        }
        
        new internal sealed class ContructionHelper : global::net.sf.jni4net.utils.IConstructionHelper {
            
            public global::net.sf.jni4net.jni.IJvmProxy CreateProxy(global::net.sf.jni4net.jni.JNIEnv @__env) {
                return new global::RMIRWin10BLE.@__WCLble(@__env);
            }
        }
    }
    #endregion
}
