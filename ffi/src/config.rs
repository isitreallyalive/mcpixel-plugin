use jni::Env;
use jni::errors::Error;
use jni::objects::JObject;
use mcpixel::Configuration;

macro_rules! read {
    ($java_type:ident => $rust_type: ty [$method:ident]) => {
        #[allow(non_snake_case)]
        fn $java_type(env: &mut Env, obj: JObject) -> Option<$rust_type> {
            if !obj.is_null() {
                env.call_method(obj, jni_str!("intValue"), jni_sig!("()I"), &[])
                    .ok()
                    .and_then(|v| v.$method().map(|v| v as $rust_type).ok())
            } else {
                None
            }
        }
    };
}

read!(Integer => u32 [i]);
read!(Float => f32 [f]);
read!(Boolean => bool [z]);

macro_rules! config {
    (
        $($field:ident: $type:ident),+
    ) => {
        bind_java_type! {
            rust_type = pub FfiConfiguration,
            java_type = dev.newty.mcpixel.ffi.Configuration,

            fields = {
                $(
                    $field: java.lang.$type
                ),+
            }
        }

        impl FfiConfiguration<'_> {
            pub(crate) fn convert(self, env: &mut Env) -> Result<Configuration, Error> {
                let mut config = Configuration::default();

                $({
                    let obj = self.$field(env)?;
                    $type(env, obj).map(|v| config.$field = v);
                })+

                Ok(config)
            }
        }
    };
}

config! {
    size: Integer,
    stretch: Boolean,
    colours: Integer,
    brightness: Float,
    saturation: Float,
    smoothing: Float,
    overlay: Boolean
}
