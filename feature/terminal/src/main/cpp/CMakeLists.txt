# DraftPeek 真 PTY native 桥。
#
# 仅依赖 bionic 的 openpty/fork/TIOCSCTTY/ioctl/waitpid，不引入第三方源码，
# 因此没有 GPL/AGPL 许可问题。
cmake_minimum_required(VERSION 3.22.1)
project(draftpeek_pty C)

add_library(draftpeek_pty SHARED pty_bridge.c)

# log 仅用于 native 侧诊断；JNI 符号由 pty_bridge.c 显式导出。
target_link_libraries(draftpeek_pty log)

target_compile_options(draftpeek_pty PRIVATE
    -Wall
    -Wextra
    -Werror
    -O2
)

# Android 15+ 16 KB page size：lib 的 LOAD 段按 16 KB 对齐。
target_link_options(draftpeek_pty PRIVATE
    -Wl,-z,max-page-size=16384
)

set_target_properties(draftpeek_pty PROPERTIES
    C_VISIBILITY_PRESET hidden
)
