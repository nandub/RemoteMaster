//------------------------------------------------------------------------------
// <auto-generated>
//     This code was generated by jni4net. See http://jni4net.sourceforge.net/ 
//     Runtime Version:2.0.50727.4952
//
//     Changes to this file may cause incorrect behavior and will be lost if
//     the code is regenerated.
// </auto-generated>
//------------------------------------------------------------------------------

namespace org.drools.@event.knowledgebase {
    
    
    #region Component Designer generated code 
    [global::net.sf.jni4net.attributes.JavaInterfaceAttribute()]
    public partial interface KnowledgeBaseEvent {
        
        [global::net.sf.jni4net.attributes.JavaMethodAttribute("()Lorg/drools/KnowledgeBase;")]
        global::org.drools.KnowledgeBase getKnowledgeBase();
    }
    #endregion
    
    #region Component Designer generated code 
    public partial class KnowledgeBaseEvent_ {
        
        public static global::java.lang.Class _class {
            get {
                return global::org.drools.@event.knowledgebase.@__KnowledgeBaseEvent.staticClass;
            }
        }
    }
    #endregion
    
    #region Component Designer generated code 
    [global::net.sf.jni4net.attributes.JavaProxyAttribute(typeof(global::org.drools.@event.knowledgebase.KnowledgeBaseEvent), typeof(global::org.drools.@event.knowledgebase.KnowledgeBaseEvent_))]
    [global::net.sf.jni4net.attributes.ClrWrapperAttribute(typeof(global::org.drools.@event.knowledgebase.KnowledgeBaseEvent), typeof(global::org.drools.@event.knowledgebase.KnowledgeBaseEvent_))]
    internal sealed partial class @__KnowledgeBaseEvent : global::java.lang.Object, global::org.drools.@event.knowledgebase.KnowledgeBaseEvent {
        
        internal new static global::java.lang.Class staticClass;
        
        internal static global::net.sf.jni4net.jni.MethodId _getKnowledgeBase0;
        
        private @__KnowledgeBaseEvent(global::net.sf.jni4net.jni.JNIEnv @__env) : 
                base(@__env) {
        }
        
        private static void InitJNI(global::net.sf.jni4net.jni.JNIEnv @__env, java.lang.Class @__class) {
            global::org.drools.@event.knowledgebase.@__KnowledgeBaseEvent.staticClass = @__class;
            global::org.drools.@event.knowledgebase.@__KnowledgeBaseEvent._getKnowledgeBase0 = @__env.GetMethodID(global::org.drools.@event.knowledgebase.@__KnowledgeBaseEvent.staticClass, "getKnowledgeBase", "()Lorg/drools/KnowledgeBase;");
        }
        
        public global::org.drools.KnowledgeBase getKnowledgeBase() {
            global::net.sf.jni4net.jni.JNIEnv @__env = this.Env;
            using(new global::net.sf.jni4net.jni.LocalFrame(@__env, 10)){
            return global::net.sf.jni4net.utils.Convertor.FullJ2C<global::org.drools.KnowledgeBase>(@__env, @__env.CallObjectMethodPtr(this, global::org.drools.@event.knowledgebase.@__KnowledgeBaseEvent._getKnowledgeBase0));
            }
        }
        
        private static global::System.Collections.Generic.List<global::net.sf.jni4net.jni.JNINativeMethod> @__Init(global::net.sf.jni4net.jni.JNIEnv @__env, global::java.lang.Class @__class) {
            global::System.Type @__type = typeof(__KnowledgeBaseEvent);
            global::System.Collections.Generic.List<global::net.sf.jni4net.jni.JNINativeMethod> methods = new global::System.Collections.Generic.List<global::net.sf.jni4net.jni.JNINativeMethod>();
            methods.Add(global::net.sf.jni4net.jni.JNINativeMethod.Create(@__type, "getKnowledgeBase", "getKnowledgeBase0", "()Lorg/drools/KnowledgeBase;"));
            return methods;
        }
        
        private static global::net.sf.jni4net.utils.JniHandle getKnowledgeBase0(global::System.IntPtr @__envp, global::net.sf.jni4net.utils.JniLocalHandle @__obj) {
            // ()Lorg/drools/KnowledgeBase;
            // ()Lorg/drools/KnowledgeBase;
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.Wrap(@__envp);
            global::net.sf.jni4net.utils.JniHandle @__return = default(global::net.sf.jni4net.utils.JniHandle);
            try {
            global::org.drools.@event.knowledgebase.KnowledgeBaseEvent @__real = global::net.sf.jni4net.utils.Convertor.FullJ2C<global::org.drools.@event.knowledgebase.KnowledgeBaseEvent>(@__env, @__obj);
            @__return = global::net.sf.jni4net.utils.Convertor.FullC2J<global::org.drools.KnowledgeBase>(@__env, @__real.getKnowledgeBase());
            }catch (global::System.Exception __ex){@__env.ThrowExisting(__ex);}
            return @__return;
        }
        
        new internal sealed class ContructionHelper : global::net.sf.jni4net.utils.IConstructionHelper {
            
            public global::net.sf.jni4net.jni.IJvmProxy CreateProxy(global::net.sf.jni4net.jni.JNIEnv @__env) {
                return new global::org.drools.@event.knowledgebase.@__KnowledgeBaseEvent(@__env);
            }
        }
    }
    #endregion
}
