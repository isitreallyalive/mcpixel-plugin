use hex::FromHex;
use std::env::var;
use std::error::Error;
use std::fs;
use std::path::PathBuf;

const DATA_URL: &str = "https://github.com/isitreallyalive/mcpixel/raw/refs/heads/main/data";

fn download(url: &str) -> Result<Vec<u8>, ureq::Error> {
    ureq::get(url)
        .call()
        .and_then(|mut r| r.body_mut().read_to_vec())
}

fn main() -> Result<(), Box<dyn Error>> {
    println!("cargo:rerun-if-env-changed=MINECRAFT");

    let version = var("MINECRAFT")?;

    let out_dir = PathBuf::from(var("OUT_DIR")?);
    let data_path = out_dir.join("data");

    // see if the version data needs downloading/updating
    if let Ok(current) = fs::read(&data_path).map(md5::compute) {
        let latest = {
            let hex = download(&format!("{DATA_URL}/{version}.md5"))?;
            <[u8; 16]>::from_hex(hex.as_slice()).map(md5::Digest)?
        };

        if latest == current {
            return Ok(());
        }
    }

    // download version
    let data = download(&format!("{DATA_URL}/{version}"))?;
    fs::write(&data_path, data)?;

    Ok(())
}
