#include "../common.h"

void sendMessages(void *arg);

void receiveMessages(void *arg);

size_t n;

int main(int argc, char *argv[]) {
    int sockfd;
    uint16_t portno;
    struct sockaddr_in serv_addr;
    struct hostent *server;
    char *name;
    u_int32_t name_size;
    n = 0;

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
    if (connect(sockfd, (struct sockaddr *) &serv_addr, sizeof(serv_addr)) < 0) {
        perror("ERROR connecting");
        exit(1);
    }

    printf("Enter your name:");

    name_size = getline(&name, &n, stdin);

    if (write(sockfd, &name_size, sizeof(u_int32_t)) < 0) {
        perror("ERROR writing to socket");
        exit(1);
    }

    if (write(sockfd, name, name_size) < 0) {
        perror("ERROR writing to socket");
        exit(1);
    }

    pthread_t tid_send;
    if (pthread_create(&tid_send, NULL, (void *) sendMessages, &sockfd) != 0) {
        printf("thread has not created\n");
        exit(1);
    }

    pthread_t tid_receive;
    if (pthread_create(&tid_receive, NULL, (void *) receiveMessages, &sockfd) != 0) {
        printf("thread has not created\n");
    }


    pthread_join(tid_send, NULL);
    pthread_join(tid_receive, NULL);
    return 0;
}

void sendMessages(void *arg) {
    int fd = *(int *) arg;
    char *buffer;
    u_int32_t buffer_size;
    while (1) {
        while (1) {
            buffer = NULL;
            n = 0;
            printf("Enter message >");
            buffer_size = getline(&buffer, &n, stdin);
            if (buffer_size > 1) {
                break;
            }
        }


        if (write(fd, &buffer_size, sizeof(u_int32_t)) < 0) {
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
            close(fd);
            exit(EXIT_SUCCESS);
        }
    }
}

void receiveMessages(void *arg) {
    int fd = *(int *) arg;
    char *buffer;
    u_int32_t buffer_size;
    while (1) {
        buffer_size = 0;

        if (read(fd, &buffer_size, sizeof(u_int32_t)) < 0) {
            perror("ERROR reading from socket");
            exit(1);
        }

        if (buffer_size == 0) {
            printf("\nServer is not available, disconnect\n");
            close(fd);
            exit(EXIT_SUCCESS);
        }

        buffer = (char *) malloc(buffer_size);
        if (read(fd, buffer, buffer_size) < 0) {
            perror("ERROR reading from socket");
            exit(1);
        }

        printf("\n%s\n", buffer);
        printf("Enter message >");
        fflush(stdout);
        free(buffer);
    }
}

