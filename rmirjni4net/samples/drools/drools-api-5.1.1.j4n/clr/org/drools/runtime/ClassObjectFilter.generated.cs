//------------------------------------------------------------------------------
// <auto-generated>
//     This code was generated by jni4net. See http://jni4net.sourceforge.net/ 
//     Runtime Version:2.0.50727.4952
//
//     Changes to this file may cause incorrect behavior and will be lost if
//     the code is regenerated.
// </auto-generated>
//------------------------------------------------------------------------------

namespace org.drools.runtime {
    
    
    #region Component Designer generated code 
    [global::net.sf.jni4net.attributes.JavaClassAttribute()]
    public partial class ClassObjectFilter : global::java.lang.Object, global::org.drools.runtime.ObjectFilter {
        
        internal new static global::java.lang.Class staticClass;
        
        internal static global::net.sf.jni4net.jni.MethodId _accept0;
        
        internal static global::net.sf.jni4net.jni.MethodId @__ctorClassObjectFilter1;
        
        [global::net.sf.jni4net.attributes.JavaMethodAttribute("(Ljava/lang/Class;)V")]
        public ClassObjectFilter(global::java.lang.Class par0) : 
                base(((global::net.sf.jni4net.jni.JNIEnv)(null))) {
            global::net.sf.jni4net.jni.JNIEnv @__env = global::net.sf.jni4net.jni.JNIEnv.ThreadEnv;
            using(new global::net.sf.jni4net.jni.LocalFrame(@__env, 12)){
            @__env.NewObject(global::org.drools.runtime.ClassObjectFilter.staticClass, global::org.drools.runtime.ClassObjectFilter.@__ctorClassObjectFilter1, this, global::net.sf.jni4net.utils.Convertor.ParStrongCp2J(par0));
            }
        }
        
        protected ClassObjectFilter(global::net.sf.jni4net.jni.JNIEnv @__env) : 
                base(@__env) {
        }
        
        public static global::java.lang.Class _class {
            get {
                return global::org.drools.runtime.ClassObjectFilter.staticClass;
            }
        }
        
        private static void InitJNI(global::net.sf.jni4net.jni.JNIEnv @__env, java.lang.Class @__class) {
            global::org.drools.runtime.ClassObjectFilter.staticClass = @__class;
            global::org.drools.runtime.ClassObjectFilter._accept0 = @__env.GetMethodID(global::org.drools.runtime.ClassObjectFilter.staticClass, "accept", "(Ljava/lang/Object;)Z");
            global::org.drools.runtime.ClassObjectFilter.@__ctorClassObjectFilter1 = @__env.GetMethodID(global::org.drools.runtime.ClassObjectFilter.staticClass, "<init>", "(Ljava/lang/Class;)V");
        }
        
        [global::net.sf.jni4net.attributes.JavaMethodAttribute("(Ljava/lang/Object;)Z")]
        public virtual bool accept(global::java.lang.Object par0) {
            global::net.sf.jni4net.jni.JNIEnv @__env = this.Env;
            using(new global::net.sf.jni4net.jni.LocalFrame(@__env, 12)){
            return ((bool)(@__env.CallBooleanMethod(this, global::org.drools.runtime.ClassObjectFilter._accept0, global::net.sf.jni4net.utils.Convertor.ParFullC2J<global::java.lang.Object>(@__env, par0))));
            }
        }
        
        new internal sealed class ContructionHelper : global::net.sf.jni4net.utils.IConstructionHelper {
            
            public global::net.sf.jni4net.jni.IJvmProxy CreateProxy(global::net.sf.jni4net.jni.JNIEnv @__env) {
                return new global::org.drools.runtime.ClassObjectFilter(@__env);
            }
        }
    }
    #endregion
}
