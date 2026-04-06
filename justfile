build:
    cd ffi && cargo build --release
    jextract ffi/target/generated/mcpixel.h --output src/main/java --t dev.newty.mcpixel.ffi --source