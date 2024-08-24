# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.
cmake_minimum_required(VERSION 3.4.1)

# Declares the project name. The project name can be accessed via ${ PROJECT_NAME},
# Since this is the top level CMakeLists.txt, the project name is also accessible
# with ${CMAKE_PROJECT_NAME} (both CMake variables are in-sync within the top level
# build script scope).
project("openjpeg")

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DUSE_JPIP")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library(${CMAKE_PROJECT_NAME} SHARED
             # Provides a relative path to your source file(s).
             openjpeg/src/lib/openjp2/bio.c
             openjpeg/src/lib/openjp2/cidx_manager.c
             openjpeg/src/lib/openjp2/cio.c
             openjpeg/src/lib/openjp2/dwt.c
             openjpeg/src/lib/openjp2/event.c
             openjpeg/src/lib/openjp2/function_list.c
             openjpeg/src/lib/openjp2/image.c
             openjpeg/src/lib/openjp2/invert.c
             openjpeg/src/lib/openjp2/j2k.c
             openjpeg/src/lib/openjp2/jp2.c
             openjpeg/src/lib/openjp2/mct.c
             openjpeg/src/lib/openjp2/mqc.c
             openjpeg/src/lib/openjp2/openjpeg.c
             openjpeg/src/lib/openjp2/opj_clock.c
             openjpeg/src/lib/openjp2/opj_malloc.c
             openjpeg/src/lib/openjp2/phix_manager.c
             openjpeg/src/lib/openjp2/pi.c
             openjpeg/src/lib/openjp2/ppix_manager.c
             openjpeg/src/lib/openjp2/sparse_array.c
             openjpeg/src/lib/openjp2/t1.c
             openjpeg/src/lib/openjp2/t2.c
             openjpeg/src/lib/openjp2/tcd.c
             openjpeg/src/lib/openjp2/tgt.c
             openjpeg/src/lib/openjp2/thix_manager.c
             openjpeg/src/lib/openjp2/thread.c
             openjpeg/src/lib/openjp2/tpix_manager.c
             openjpeg/src/bin/common/color.c
             openjpg.cpp
             )

target_include_directories(${CMAKE_PROJECT_NAME}
        PRIVATE
        ./
        openjpeg/src/lib/openjp2
        openjpeg/src/bin/common
)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries(${CMAKE_PROJECT_NAME}

        # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )
