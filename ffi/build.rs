use cbindgen::{Config, Language};

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let crate_dir = std::env::var("CARGO_MANIFEST_DIR")?;

    cbindgen::generate_with_config(crate_dir, Config {
        language: Language::C,
        ..Default::default()
    })?.write_to_file("target/generated/mcpixel.h");

    Ok(())
}
