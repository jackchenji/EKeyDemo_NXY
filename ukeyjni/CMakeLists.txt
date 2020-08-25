cmake_minimum_required(VERSION 3.4.1)

include_directories(src/main/cpp/)

add_library(tmjni SHARED
            src/main/cpp/com_froad_ukey_jni_tmjni.cpp
            src/main/cpp/DesAlgo.cpp
            src/main/cpp/Utils.cpp
            src/main/cpp/froadLog.cpp
            src/main/cpp/tmPrivMethod.cpp
            src/main/cpp/GenP10.cpp
            src/main/cpp/Base64Utils.cpp
            src/main/cpp/GmSSL.cpp
        )

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

find_library( # Sets the name of the path variable.
        lz-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        z)

find_library( # Sets the name of the path variable.
        lm-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        m)

# 添加两个预编译库
add_library(gmssl-crypto
        STATIC
        IMPORTED)

set_target_properties(gmssl-crypto
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/libs/armeabi/libcrypto.a
        )

add_library(gmssl-ssl
        STATIC
        IMPORTED)

set_target_properties(gmssl-ssl
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/libs/armeabi/libssl.a
        )

# Include libraries needed for tmjni lib
target_link_libraries(tmjni
                      gmssl-crypto
                      gmssl-ssl
                      ${log-lib}
                      ${lz-lib}
                      ${lm-lib}
                      )
