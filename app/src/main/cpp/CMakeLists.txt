# SECURITY VULN-005: Native C-layer anti-detection
# Builds native_security.so with anti-debugging checks that are harder
# to hook with Frida than Java/Kotlin methods.
cmake_minimum_required(VERSION 3.22.1)
project(native_security C)

add_library(native_security SHARED native_security.c)

# Link with Android log library for debugging (removed in release builds by R8)
target_link_libraries(native_security log)

# Strip symbols in release builds for additional obfuscation
set_target_properties(native_security PROPERTIES
    C_VISIBILITY_PRESET hidden
    VISIBILITY_INLINES_HIDDEN ON
)
