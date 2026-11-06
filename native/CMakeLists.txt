cmake_minimum_required(VERSION 3.24)
set(CMAKE_ASM_NASM_OBJECT_FORMAT win64)
project(glasscalc_native LANGUAGES C ASM_NASM)

if(NOT WIN32 OR NOT CMAKE_SIZEOF_VOID_P EQUAL 8)
    message(FATAL_ERROR "GlassCalc's current NASM backend targets 64-bit Windows only.")
endif()

find_package(JNI REQUIRED)

add_library(glasscalc SHARED
    src/jni_bridge.c
    src/asm_math.asm
)

target_include_directories(glasscalc PRIVATE ${JNI_INCLUDE_DIRS})
target_compile_definitions(glasscalc PRIVATE WIN32_LEAN_AND_MEAN)
set_target_properties(glasscalc PROPERTIES
    OUTPUT_NAME "glasscalc"
    WINDOWS_EXPORT_ALL_SYMBOLS OFF
    RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/bin"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/bin"
)
