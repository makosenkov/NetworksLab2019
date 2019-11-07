#include <stdio.h>
#include <stdlib.h>

#include <netdb.h>
#include <netinet/in.h>
#include <unistd.h>

#include <string.h>
#include <pthread.h>

ssize_t n;

int clients_length;

struct Client {
    int fd;
    char name[10];
};

struct Client *clients;

void handle_connection(void *arg);
int receiveMessage(int fd, char *buffer, int bufferLength);
int sendMessagesToAllClients(int authorFd, char *buffer, int bufferLength);

int main(int argc, char *argv[]) {
    int sockfd, newsockfd;
    uint16_t portno;
    unsigned int clilen;
    clients_length = 0;

    struct sockaddr_in serv_addr, cli_addr;

    /* First call to socket() function */
    sockfd = socket(AF_INET, SOCK_STREAM, 0);

    if (sockfd < 0) {
        perror("ERROR opening socket");
        exit(1);
    }

    /* Initialize socket structure */
    bzero((char *) &serv_addr, sizeof(serv_addr));
    portno = 5002;

    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = INADDR_ANY;
    serv_addr.sin_port = htons(portno);

    /* Now bind the host address using bind() call.*/
    if (bind(sockfd, (struct sockaddr *) &serv_addr, sizeof(serv_addr)) < 0) {
        perror("ERROR on binding");
        exit(1);
    }

    listen(sockfd, 5);
    clilen = sizeof(cli_addr);

    clients = (struct Client *) malloc(sizeof(struct Client));

    while (1) {
        newsockfd = accept(sockfd, (struct sockaddr *) &cli_addr, &clilen);


        if (newsockfd < 0) {
            perror("ERROR on accept");
            close(sockfd);
            exit(1);
        }
        printf("%d\n", newsockfd);
        clients[clients_length].fd = newsockfd;
        printf("%d\n" ,clients[clients_length].fd);
        fflush(stdout);
        pthread_t tid;
        if (pthread_create(&tid, NULL, (void *) handle_connection, &clients_length) != 0) {
            printf("thread has not created");
            close(sockfd);
            exit(1);
        }
        sleep(1);
        clients_length++;
        clients = realloc(clients, clients_length * sizeof(struct Client));
        
    }


}

void handle_connection(void *arg) {
    int index = *(int *) arg;
    printf("%d\n", index);
    struct Client *temp = &clients[index];
    printf("%d\n", temp->fd);
    fflush(stdout);
    char buffer[10];
    printf("new Client connected\n");
    if (read(temp->fd, buffer, sizeof(buffer)) < 0) {
        printf("dfvbkljbdfv");
    }
    strncpy(temp->name, buffer, 10);
    printf("%s\n", temp->name);
    fflush(stdout);
    // n = receiveMessage(temp->fd, buffer, sizeof(buffer));

    char message[255];

    while (1) {
        n = receiveMessage(temp->fd, message, sizeof(message));
        sendMessagesToAllClients(temp->fd, message, sizeof(message));
    }

}

int receiveMessage(int fd, char *buffer, int bufferLength) {
    bzero(buffer, bufferLength);
    int message;
    message = read(fd, buffer, bufferLength);

    if (message < 0 ) {
        close(fd);
        perror("ERROR reading from socket");
        exit(1);
    }

    return message;
}

int sendMessagesToAllClients(int authorFd, char *buffer, int bufferLength) {
    for (int i = 0; i < clients_length; i++) {
        if (clients[i].fd != authorFd) {
            n = write(clients[i].fd, buffer, bufferLength);
            if (n < 0) {
                perror("ERROR writing to socket");
                exit(1);
            }
        }
    }


    return n;
}
