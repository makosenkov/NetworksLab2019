#include <stdio.h>
#include <stdlib.h>

#include <netinet/in.h>
#include <unistd.h>

#include <string.h>
#include <pthread.h>

ssize_t n;

typedef struct Client {
    int fd;
    char name[10];
    struct Client *next;
} ClientLinkedList;

ClientLinkedList *init_list(int fd) {
    ClientLinkedList *temp = (ClientLinkedList *) malloc(sizeof(ClientLinkedList));
    temp->fd = fd;
    temp->next = NULL;
    return temp;
}

ClientLinkedList *first, *last;

void handle_connection(void *arg);
void handleMessage(ClientLinkedList *client, char *buffer, int bufferLength);
void sendMessagesToAllClients(ClientLinkedList *author, char *buffer, int bufferLength);
void exit_and_free(ClientLinkedList *client);
void delete_line_break(char *str);

int main(int argc, char *argv[]) {
    int sockfd, newsockfd;
    uint16_t portno;
    unsigned int clilen;

    struct sockaddr_in serv_addr, cli_addr;

    /* First call to socket() function */
    sockfd = socket(AF_INET, SOCK_STREAM, 0);

    if (sockfd < 0) {
        perror("ERROR opening socket");
        exit(1);
    }

    /* Initialize socket structure */
    bzero((char *) &serv_addr, sizeof(serv_addr));
    portno = 5001;

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

    first = init_list(sockfd);
    last = first;

    while (1) {
        ClientLinkedList *newClient = init_list(sockfd);
        newsockfd = accept(sockfd, (struct sockaddr *) &cli_addr, &clilen);

        if (newsockfd < 0) {
            perror("ERROR on accept");
            close(sockfd);
            exit(1);
        }

        newClient->fd = newsockfd;
        last->next = newClient;
        last = newClient;

        pthread_t tid;
        if (pthread_create(&tid, NULL, (void *) handle_connection, newClient) != 0) {
            printf("thread has not created");
            close(sockfd);
            exit(1);
        }
    }
}

void handle_connection(void *arg) {
    ClientLinkedList *client = (ClientLinkedList *) arg;
    char name[10];
    printf("new Client connected\n");
    if ((n = read(client->fd, name, sizeof(name))) < 0) {
        printf("ERROR reading from socket");
    } else if (n == 0) {
        exit_and_free(client);
    }
    delete_line_break(name);
    strncpy(client->name, name, 10);
    printf("%s connected to server\n", client->name);
    char name_buffer[50];
    sprintf(name_buffer, "%s connected to server\n", client->name);
    delete_line_break(name_buffer);
    sendMessagesToAllClients(client, name_buffer, 50);
    fflush(stdout);

    char message[255];
    while (1) {
        handleMessage(client, message, sizeof(message));
    }
}

void handleMessage(ClientLinkedList *client, char *buffer, int bufferLength) {
    bzero(buffer, bufferLength);
    int message;
    message = read(client->fd, buffer, bufferLength);
    if (message < 0 ) {
        perror("ERROR reading from socket");
        exit(1);
    } else if (message == 0) {
        exit_and_free(client);
    }
    delete_line_break(buffer);
    if (strcmp(buffer, "/exit") == 0) {
        printf("%s disconnected from server", client->name);
        char name_buffer[50];
        sprintf(name_buffer, "%s left the chat", client->name);
        sendMessagesToAllClients(client, name_buffer, 50);
        exit_and_free(client);
    } else {
        sendMessagesToAllClients(client, buffer, bufferLength);
    }
}

void sendMessagesToAllClients(ClientLinkedList *author, char *buffer, int bufferLength) {
    ClientLinkedList *receiver = first->next;
    char message[255];
    sprintf(message, "%s: %s", author->name, buffer);
    while (receiver != NULL) {
        if (receiver != author) {
            n = write(receiver->fd, message, bufferLength);
            if (n < 0) {
                perror("ERROR writing to socket");
                exit(1);
            }
        }
        receiver = receiver->next;
    }
}

void exit_and_free(ClientLinkedList *client) {
    close(client->fd);
    ClientLinkedList *temp = first;
    while (temp->next != client) {
        temp = temp->next;
    }
    if (client->next == NULL) {
        last = temp;
        temp->next = NULL;
        free(client);
        pthread_exit(NULL);
    } else {
        temp->next = client->next;
        free(client);
        pthread_exit(NULL);
    }
}

void delete_line_break(char *str) {
    for (int i = 0; i < (int) strlen(str); i++) {
        if (str[i] == '\n') {
            str[i] = '\0';
            break;
        }
    }
}
