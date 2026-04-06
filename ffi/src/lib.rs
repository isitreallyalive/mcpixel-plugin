use jni::jni_mangle;

#[jni_mangle("dev.newty.mcpixel.ffi.McPixel")]
pub extern "system" fn hello_world() {
    println!("hello world!");
}
