#include <fcntl.h>
#include <poll.h>
#include <errno.h>
#include "../common.h"

void sendMessages(void *arg);

void receiveMessages(void *arg);
void disconnect();
void doPoll(struct pollfd fd, int state);
size_t n;
int sockfd;

int main(int argc, char *argv[]) {
    uint16_t portno;
    struct sockaddr_in serv_addr;
    struct hostent *server;
    char *name;
    uint32_t name_size;
    n = 0;
    struct pollfd read_sock;

    if (argc < 3) {
        fprintf(stderr, "usage %s hostname port\n", argv[0]);
        exit(0);
    }

    portno = (uint16_t) atoi(argv[2]);

    /* Create a socket point */
    sockfd = socket(AF_INET, SOCK_STREAM, 0);

    if (sockfd < 0) {
        perror("ERROR opening socket");
        exit(1);
    }

    if (fcntl(sockfd, F_SETFL, O_NONBLOCK) < 0) {
        perror("ERROR making socket nonblock");
        exit(1);
    }

    server = gethostbyname(argv[1]);

    if (server == NULL) {
        fprintf(stderr, "ERROR, no such host\n");
        exit(0);
    }

    bzero((char *) &serv_addr, sizeof(serv_addr));
    serv_addr.sin_family = AF_INET;
    bcopy(server->h_addr, (char *) &serv_addr.sin_addr.s_addr, (size_t) server->h_length);
    serv_addr.sin_port = htons(portno);

    /* Now connect to the server */
    while (connect(sockfd, (struct sockaddr *) &serv_addr, sizeof(serv_addr)) < 0) {
    }

    printf("Enter your name:");

    name_size = getline(&name, &n, stdin);

    if (write(sockfd, &name_size, sizeof(uint32_t)) < 0) {
        perror("ERROR writing to socket");
        exit(1);
    }

    if (write(sockfd, name, name_size) < 0) {
        perror("ERROR writing to socket");
        exit(1);
    }

    read_sock.fd = sockfd;
    read_sock.events = POLLIN;

    pthread_t tid_send;
    if (pthread_create(&tid_send, NULL, (void *) sendMessages, &sockfd) != 0) {
        printf("thread has not created\n");
        exit(1);
    }

    pthread_t tid_receive;
    if (pthread_create(&tid_receive, NULL, (void *) receiveMessages, &read_sock) != 0) {
        printf("thread has not created\n");
    }


    pthread_join(tid_send, NULL);
    pthread_join(tid_receive, NULL);
    return 0;
}

void sendMessages(void *arg) {
    int fd = *(int *) arg;
    char *buffer;
    uint32_t buffer_size;
    while (1) {
        buffer = NULL;
        n = 0;
        buffer_size = getline(&buffer, &n, stdin);

        if (write(fd, &buffer_size, sizeof(int)) < 0) {
            perror("ERROR writing to socket");
            exit(1);
        }

        if (write(fd, buffer, buffer_size) < 0) {
            perror("ERROR writing to socket");
            exit(1);
        }

        delete_line_break(buffer);
        if (strcmp(buffer, "/exit") == 0) {
            printf("Disconnected\n");
            disconnect();
            exit(EXIT_SUCCESS);
        }
    }
}

void receiveMessages(void *arg) {
    struct pollfd fd = *(struct pollfd *) arg;
    int state;
    char *buffer;
    uint32_t buffer_size;
    while (1) {
        state = poll(&fd, 1, -1);
        if (state < 0) {
            perror("ERROR on poll");
            exit(1);
        }

        if (fd.revents == 0) {
            continue;
        }

        if (fd.revents != POLLIN) {
            printf("ERROR wrong revents");
            exit(1);
        }
        printf("read poll done\n");
        fflush(stdout);
        buffer_size = 0;

        if ((state = read(fd.fd, &buffer_size, sizeof(int)) < 0)) {
            if (errno != EWOULDBLOCK) {
                perror("ERROR reading size from socket");
                disconnect();
            }
        }

        if (state == 0) {
            printf("\rThis socket is closed\n");
            disconnect();
        }
        printf("read size\n");
        fflush(stdout);

        state = poll(&fd, 1, -1);
        printf("sec poll\n");
        fflush(stdout);
        if (state < 0) {
            perror("ERROR on poll");
            exit(1);
        } else if (fd.revents != POLLIN) {
            printf("ERROR wrong revents");
            exit(1);
        }
        printf("sec poll done\n");
        fflush(stdout);
        buffer = (char *) malloc(buffer_size);
        if (read(fd.fd, buffer, buffer_size) < 0) {
            if (errno != EWOULDBLOCK) {
                perror("ERROR reading msg from socket");
                disconnect();
            }
        }
        printf("read msg\n");
        fflush(stdout);
        printf("\r");
        printf("%s\n", buffer);
        free(buffer);
    }
}

void disconnect() {
    close(sockfd);
    printf("\n");
    exit(EXIT_SUCCESS);
}

void doPoll(struct pollfd fd, int state) {
    state = poll(&fd, 1, -1);
    if (state < 0) {
        perror("ERROR on poll");
        exit(1);
    } else if (state != POLLIN) {
        printf("ERROR wrong revents");
        exit(1);
    }
}

