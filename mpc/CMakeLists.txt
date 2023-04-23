cmake_minimum_required(VERSION 3.23)
project(ligma C)
set(CMAKE_C_STANDARD 99)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fPIC -O3 -g")

set(JAVA_AWT_LIBRARY NotNeeded)
set(JAVA_AWT_INCLUDE_PATH NotNeeded)

find_package(JNI)
include_directories(${JNI_INCLUDE_DIRS})

add_library(drivempc SHARED src/main/cpp/interface.c src/main/cpp/matrix_functions.c src/main/cpp/model.c src/main/cpp/mecanum_mpc.c src/main/cpp/locs.c)
add_executable(test src/main/cpp/main.c src/main/cpp/interface.c src/main/cpp/matrix_functions.c src/main/cpp/model.c src/main/cpp/mecanum_mpc.c src/main/cpp/locs.c)
set (install_dir ${CMAKE_SOURCE_DIR}/../libs/arm64-v8a)
install(TARGETS drivempc DESTINATION ${install_dir})
target_include_directories(drivempc PRIVATE ${Java_INCLUDE_DIRS})
