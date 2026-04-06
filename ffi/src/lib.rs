use jni::errors::ThrowRuntimeExAndDefault;
use jni::objects::{JByteArray, JClass};
use jni::sys::jlong;
use jni::{EnvUnowned, jni_mangle};
use mcpixel::version::Version;
use mcpixel::{Configuration, PixelArt};
use std::cell::LazyCell;

const VERSION: LazyCell<Version> = LazyCell::new(|| {
    let data = include_bytes!("1.21.11");
    Version::read(&data[..]).expect("should have valid version data")
});

#[jni_mangle("dev.newty.mcpixel.ffi.McPixel")]
pub extern "system" fn new_pixel_art(mut env: EnvUnowned, _: JClass, image: JByteArray) -> jlong {
    let image = env
        .with_env(|env| env.convert_byte_array(image))
        .resolve::<ThrowRuntimeExAndDefault>();

    match PixelArt::new(image, VERSION.clone(), Configuration::default()) {
        Ok(art) => Box::into_raw(Box::new(art)) as jlong,
        Err(_) => 0, // null ptr
    }
}

#[jni_mangle("dev.newty.mcpixel.ffi.McPixel")]
pub extern "system" fn free_pixel_art(_: EnvUnowned, _: JClass, ptr: jlong) {
    unsafe {
        let _ = Box::from_raw(ptr as *mut PixelArt);
    }
}
