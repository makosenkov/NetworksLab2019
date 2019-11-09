#ifndef NETWORKSLAB20191_COMMON_H
#define NETWORKSLAB20191_COMMON_H

#endif //NETWORKSLAB20191_COMMON_H

#include <stdio.h>
#include <stdlib.h>

#include <netinet/in.h>
#include <unistd.h>

#include <netdb.h>

#include <string.h>
#include <pthread.h>

void delete_line_break(char *str) {
    for (int i = 0; i < (int) strlen(str); i++) {
        if (str[i] == '\n') {
            str[i] = '\0';
            break;
        }
    }
}

int read_bytes(int fd, char *buffer, int len) {
    int read_size = 0;
    int res;
    while (read_size < len) {
        res = read(fd, buffer + read_size, len);
        if (res == 0) {
            read_size = res;
            break;
        }
        if (res < 0) {
            perror("ERROR reading from socket");
            exit(1);
        }
        len -= res;
        read_size += res;
    }
    return read_size;
}