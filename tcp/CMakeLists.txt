cmake_minimum_required(VERSION 2.8)

project(NetworksLab20191)

find_library(PTHREAD_LIBRARY pthread)

add_executable(client client/main.c common.h)
add_executable(server server/main.c)


target_link_libraries(server ${PTHREAD_LIBRARY})
target_link_libraries(client ${PTHREAD_LIBRARY})