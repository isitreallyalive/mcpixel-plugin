use jni::errors::ThrowRuntimeExAndDefault;
use jni::objects::{JByteArray, JClass, JObject};
use jni::sys::{jlong, jobjectArray, jsize};
use jni::{EnvUnowned, bind_java_type, jni_mangle, jni_str};
use mcpixel::version::Version;
use mcpixel::{Configuration, PixelArt};
use std::sync::LazyLock;

static VERSION: LazyLock<Version> = LazyLock::new(|| {
    let data = include_bytes!("1.21.11");
    Version::read(&data[..]).expect("should have valid version data")
});

bind_java_type! {
    rust_type = ResolvedBlock,
    java_type = dev.newty.mcpixel.ffi.ResolvedBlock,

    constructors {
        fn new(id: JString, top: bool)
    }
}

bind_java_type! {
    rust_type = BlockPair,
    java_type = dev.newty.mcpixel.ffi.BlockPair,

    constructors {
        fn new(base: dev.newty.mcpixel.ffi.ResolvedBlock, overlay:dev.newty.mcpixel.ffi.ResolvedBlock)
    }
}

#[jni_mangle("dev.newty.mcpixel.ffi.McPixel")]
pub extern "system" fn new_art(mut env: EnvUnowned, _: JClass, image: JByteArray) -> jlong {
    let image = env
        .with_env(|env| env.convert_byte_array(image))
        .resolve::<ThrowRuntimeExAndDefault>();

    match PixelArt::new(image, VERSION.clone(), Configuration::default()) {
        Ok(art) => Box::into_raw(Box::new(art)) as jlong,
        Err(_) => 0, // null ptr
    }
}

#[jni_mangle("dev.newty.mcpixel.ffi.McPixel")]
pub extern "system" fn free_art(_: EnvUnowned, _: JClass, ptr: jlong) {
    unsafe {
        let _ = Box::from_raw(ptr as *mut PixelArt);
    }
}

#[jni_mangle("dev.newty.mcpixel.ffi.McPixel")]
pub extern "system" fn art_blocks(mut env: EnvUnowned, _: JClass, ptr: jlong) -> jobjectArray {
    let art = unsafe { &*(ptr as *mut PixelArt) };

    env.with_env(|env| {
        let outer = {
            let class = env.find_class(jni_str!("[Ldev/newty/mcpixel/ffi/BlockPair;"))?; // BlockPair[]
            env.new_object_array(art.blocks().len() as jsize, class, JObject::null())?
        };

        let pair_class = env.find_class(jni_str!("dev/newty/mcpixel/ffi/BlockPair"))?; // BlockPair

        for (i, row) in art.blocks().iter().enumerate() {
            let inner = env.new_object_array(row.len() as jsize, &pair_class, JObject::null())?;

            for (j, res) in row.iter().enumerate() {
                let Some(((id, top), overlay)) = res.as_ref() else {
                    continue;
                };

                let base = {
                    let id = env.new_string(id)?;
                    ResolvedBlock::new(env, id, *top)?
                };
                let overlay_block = if let Some((id, top)) = overlay {
                    let id = env.new_string(id)?;
                    ResolvedBlock::new(env, id, *top)?
                } else {
                    ResolvedBlock::null()
                };

                let pair = BlockPair::new(env, base, overlay_block)?;
                inner.set_element(env, j, pair)?;
            }

            outer.set_element(env, i, inner)?;
        }

        Ok::<_, jni::errors::Error>(outer)
    })
    .resolve::<ThrowRuntimeExAndDefault>()
    .into_raw()
}
