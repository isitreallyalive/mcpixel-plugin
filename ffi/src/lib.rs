use jni::errors::{Error, ThrowRuntimeExAndDefault};
use jni::objects::{JByteArray, JClass, JIntArray, JObject, JString};
use jni::sys::{jint, jintArray, jlong, jobjectArray, jsize};
use jni::{EnvUnowned, bind_java_type, jni_mangle, jni_str};
use mcpixel::version::Version;
use mcpixel::{Configuration, PixelArt};
use std::sync::LazyLock;

static VERSION: LazyLock<Version> = LazyLock::new(|| {
    let data = include_bytes!("1.21.11");
    Version::read(&data[..]).expect("should have valid version data")
});

bind_java_type! {
    rust_type = Texture,
    java_type = dev.newty.mcpixel.ffi.Texture,

    constructors {
        fn new(base_id: JString, base_top: bool, overlay_id: JString, overlay_top: bool)
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
pub extern "system" fn art_dimensions(mut env: EnvUnowned, _: JClass, ptr: jlong) -> jintArray {
    let art = unsafe { &mut *(ptr as *mut PixelArt) };
    let (width, height) = art.dimensions();

    env.with_env(|env| {
        let array = JIntArray::new(env, 2)?;
        array.set_region(env, 0, &[width as jint, height as jint])?;

        Ok::<_, Error>(array)
    })
    .resolve::<ThrowRuntimeExAndDefault>()
    .into_raw()
}

#[jni_mangle("dev.newty.mcpixel.ffi.McPixel")]
pub extern "system" fn art_blocks(mut env: EnvUnowned, _: JClass, ptr: jlong) -> jobjectArray {
    let art = unsafe { &*(ptr as *mut PixelArt) };
    let blocks = art.blocks();

    env.with_env(|env| {
        let texture_class = env.find_class(jni_str!("dev/newty/mcpixel/ffi/Texture"))?;
        let flat_blocks: Vec<_> = blocks.iter().flat_map(|row| row.iter()).collect();
        let array =
            env.new_object_array(flat_blocks.len() as jsize, texture_class, JObject::null())?;

        for (i, res) in flat_blocks.iter().enumerate() {
            if let Some(((base_id, base_top), overlay)) = res.as_ref() {
                // resolve
                let base_id = env.new_string(base_id)?;
                let (overlay_id, overlay_top) = if let Some((id, top)) = overlay {
                    let id = env.new_string(id)?;
                    (id, *top)
                } else {
                    (JString::null(), false)
                };

                // create pair and put it in the array
                let pair = Texture::new(env, base_id, *base_top, overlay_id, overlay_top)?;
                array.set_element(env, i, pair)?;
            }
        }

        Ok::<_, Error>(array)
    })
    .resolve::<ThrowRuntimeExAndDefault>()
    .into_raw()
}
