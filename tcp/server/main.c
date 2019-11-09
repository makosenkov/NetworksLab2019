#include "../common.h"

ssize_t n;

typedef struct Client {
    int fd;
    char *name;
    struct Client *next;
} ClientLinkedList;

ClientLinkedList *init_list(int fd) {
    ClientLinkedList *temp = (ClientLinkedList *) malloc(sizeof(ClientLinkedList));
    temp->fd = fd;
    temp->next = NULL;
    return temp;
}

ClientLinkedList *first, *last;

pthread_mutex_t mutex;

void handle_connection(void *arg);
void handleMessage(ClientLinkedList *client);
void sendMessagesToAllClients(ClientLinkedList *author, char *buffer, u_int32_t bufferLength);
void exit_and_free(ClientLinkedList *client);
void handleName(ClientLinkedList *client);

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

    first = init_list(sockfd);
    last = first;

    if (pthread_mutex_init(&mutex, NULL) != 0) {
        perror("ERROR creating mutex");
        exit(1);
    }

    pthread_t tid;

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

        if (pthread_create(&tid, NULL, (void *) handle_connection, newClient) != 0) {
            printf("thread has not created");
            close(sockfd);
            exit(1);
        }
    }

}

void handle_connection(void *arg) {
    ClientLinkedList *client = (ClientLinkedList *) arg;
    handleName(client);

    while (1) {
        handleMessage(client);
    }
}

void handleName(ClientLinkedList *client) {
    char *name_buffer;
    u_int32_t name_size = 0;
    int r;
    printf("new Client connected\n");
    if ((r = read(client->fd, &name_size, sizeof(u_int32_t))) < 0) {
        printf("ERROR reading from socket");
    } else if (r == 0) {
        exit_and_free(client);
    }
    client->name = (char *) malloc(name_size);
    name_buffer = (char *) malloc(name_size + 25 * sizeof(char));

    if ((r = read_bytes(client->fd, name_buffer, name_size)) < 0) {
        perror("ERROR reading from socket");
        exit(1);
    } else if(r == 0) {
        exit_and_free(client);
    } else {
        delete_line_break(name_buffer);
        strncpy(client->name, name_buffer, name_size);
        printf("%s connected to server\n", client->name);

        sprintf(name_buffer, "%s connected to server\n", client->name);
        delete_line_break(name_buffer);
        sendMessagesToAllClients(client, name_buffer, name_size + 25 * sizeof(char));
        fflush(stdout);
    }
}

void handleMessage(ClientLinkedList *client) {
    char *message_buffer;
    int r;
    u_int32_t message_size = 0;
    r = read(client->fd, &message_size, sizeof(u_int32_t));
    if (r < 0 ) {
        perror("ERROR reading from socket");
        exit(1);
    } else if (r == 0) {
        exit_and_free(client);
    }

    message_buffer = (char *) malloc(message_size);

    if ((r = read_bytes(client->fd, message_buffer, message_size)) < 0) {
        perror("ERROR reading from socket");
        exit(1);
    } else if(r == 0) {
        exit_and_free(client);
    } else {
        delete_line_break(message_buffer);
        if (strcmp(message_buffer, "/exit") == 0) {
            printf("%s disconnected from server\n", client->name);
            char *name_buffer;
            u_int32_t connected_name_size = sizeof(client->name) + 15 * sizeof(char);
            name_buffer = (char *) malloc(connected_name_size);
            sprintf(name_buffer, "%s left the chat", client->name);
            sendMessagesToAllClients(client, name_buffer, connected_name_size);
            exit_and_free(client);
        } else {
            char *message;
            u_int32_t full_message_size = message_size + sizeof(client->name) + 2 * sizeof(char);
            message = (char *) malloc(full_message_size);
            sprintf(message, "%s: %s", client->name, message_buffer);
            sendMessagesToAllClients(client, message, full_message_size);
        }
    }
}

void sendMessagesToAllClients(ClientLinkedList *author, char *buffer, u_int32_t bufferLength) {
    ClientLinkedList *receiver = first->next;
    pthread_mutex_lock(&mutex);
    while (receiver != NULL) {
        if (receiver != author) {
            n = write(receiver->fd, &bufferLength, sizeof(u_int32_t));
            if (n < 0) {
                perror("ERROR writing to socket");
                exit(1);
            }
            n = write(receiver->fd, buffer, bufferLength);
            if (n < 0) {
                perror("ERROR writing to socket");
                exit(1);
            }
        }
        receiver = receiver->next;
    }
    pthread_mutex_unlock(&mutex);
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

