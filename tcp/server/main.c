#include <fcntl.h>
#include <poll.h>
#include <asm/errno.h>
#include <errno.h>
#include "../common.h"

ssize_t n;

typedef struct Client {
    int fd;
    char *name;
    int autorized;
    struct Client *next;
} ClientLinkedList;

ClientLinkedList *init_list(int fd) {
    ClientLinkedList *temp = (ClientLinkedList *) malloc(sizeof(ClientLinkedList));
    temp->fd = fd;
    temp->autorized = 0;
    temp->next = NULL;
    return temp;
}

ClientLinkedList *first, *last;

struct pollfd *sockets;
int sockfd;
int poll_size;

void handleMessage(ClientLinkedList *client);

void sendMessagesToAllClients(ClientLinkedList *author, char *buffer, uint32_t bufferLength);

void exit_and_free(ClientLinkedList *client);

void handleName(ClientLinkedList *client);

ClientLinkedList* findClientByFd(int fd);

int main(int argc, char *argv[]) {
    uint16_t portno;
    unsigned int clilen;
    int temp_poll_size;
    int state;
    struct sockaddr_in serv_addr, cli_addr;
    poll_size = 1;
    
    /* First call to socket() function */
    sockfd = socket(AF_INET, SOCK_STREAM, 0);

    if (sockfd < 0) {
        perror("ERROR opening socket");
        exit(1);
    }
    int on = 1;
    if (setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, (char *) &on, sizeof(on)) < 0) {
        perror("ERROR on setsockopt");
        exit(2);
    }

    if (fcntl(sockfd, F_SETFL, O_NONBLOCK) < 0) {
        perror("ERROR making socket nonblock");
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

    if (listen(sockfd, 5) < 0) {
        perror("ERROR on listen");
        exit(1);
    }

    clilen = sizeof(cli_addr);

    first = init_list(sockfd);
    last = first;

    sockets = (struct pollfd *) malloc(sizeof(struct pollfd));

    bzero(sockets, sizeof(sockets));
    sockets[0].fd = sockfd;
    sockets[0].events = POLLIN;

    while (1) {
        state = poll(sockets, poll_size, -1);
        if (state < 0) {
            perror("ERROR on poll");
            break;
        }

        temp_poll_size = poll_size;
        for (int i = 0; i < temp_poll_size; i++) {

            if (sockets[i].revents == 0) {
                continue;
            }

            if (sockets[i].revents != POLLIN) {
                printf("loopppp: %d\n", i);
                perror("ERROR wrong revents\n");
                exit(1);
            }

            if (sockets[i].fd == sockfd) {
                state = accept(sockfd, (struct sockaddr *) &cli_addr, &clilen);

                if (state < 0) {
                    break;
                }

                poll_size++;
                sockets = (struct pollfd *) realloc(sockets, poll_size * sizeof(struct pollfd));
                sockets[poll_size - 1].fd = state;
                sockets[poll_size - 1].events = POLLIN;

                ClientLinkedList *newClient = (ClientLinkedList *) malloc(sizeof(ClientLinkedList));
                newClient->fd = state;
                newClient->autorized = 0;
                newClient->next = NULL;
                last->next = newClient;
                last = newClient;

            } else {
                ClientLinkedList* client = findClientByFd(sockets[i].fd);
                state = poll(&sockets[i], 1, -1);
                if (state < 0) {
                    perror("ERROR on poll");
                    exit(1);
                }
                if (sockets[i].revents != POLLIN) {
                    printf("ERROR wrong revents");
                    exit(1);
                }
                if (client->autorized == 0) {
                    handleName(client);
                } else {
                    handleMessage(client);
                }
            }


        }

    }
}

ClientLinkedList* findClientByFd(int fd) {
    ClientLinkedList *temp = first->next;
    while (temp != NULL) {
        if (temp->fd == fd) {
            return temp;
        }
        temp = temp->next;
    }
    exit(1);
}

void handleName(ClientLinkedList *client) {
    char *name_buffer;
    uint32_t name_size = 0;
    int state;
    printf("new Client connected\n");
    if ((state = read(client->fd, &name_size, sizeof(int))) < 0) {
        if (errno != EWOULDBLOCK) {
            perror("ERROR reading from socket");
            exit(1);
        }
        printf("ERROR reading from socket");
    } else if (state == 0) {
        exit_and_free(client);
    }
    client->name = (char *) malloc(name_size);
    name_buffer = (char *) malloc(name_size + 25);

    if ((state = read_bytes(client->fd, name_buffer, name_size)) < 0) {
        if (errno != EWOULDBLOCK) {
            perror("ERROR reading from socket");
            exit(1);
        }
    } else if (state == 0) {
        exit_and_free(client);
    } else {
        delete_line_break(name_buffer);
        strncpy(client->name, name_buffer, name_size);
        client->autorized = 1;
        printf("%s connected to server\n", client->name);

        sprintf(name_buffer, "%s connected to server\n", client->name);
        delete_line_break(name_buffer);
        sendMessagesToAllClients(client, name_buffer, name_size + 25);
        fflush(stdout);
    }
}

void handleMessage(ClientLinkedList *client) {
    char *message_buffer;
    int r;
    uint32_t message_size = 0;
    r = read(client->fd, &message_size, sizeof(int));
    if (r < 0) {
        if (errno != EWOULDBLOCK) {
            perror("ERROR reading from socket");
            exit(1);
        }
    } else if (r == 0) {
        exit_and_free(client);
    }

    message_buffer = (char *) malloc(message_size);

    if ((r = read_bytes(client->fd, message_buffer, message_size)) < 0) {
        if (errno != EWOULDBLOCK) {
            perror("ERROR reading from socket");
            exit(1);
        }
    } else if (r == 0) {
        exit_and_free(client);
    } else {
        delete_line_break(message_buffer);
        if (strcmp(message_buffer, "/exit") == 0) {
            printf("%s disconnected from server\n", client->name);
            char *name_buffer;
            uint32_t connected_name_size = sizeof(client->name) + 15 * sizeof(char);
            name_buffer = (char *) malloc(connected_name_size);
            sprintf(name_buffer, "%s left the chat", client->name);
            sendMessagesToAllClients(client, name_buffer, connected_name_size);
            exit_and_free(client);
        } else {
            char *message;
            uint32_t full_message_size = message_size + sizeof(client->name) + 2 * sizeof(char);
            message = (char *) malloc(full_message_size);
            sprintf(message, "%s: %s", client->name, message_buffer);
            printf("%s\n", message);
            fflush(stdout);
            sendMessagesToAllClients(client, message, full_message_size);
        }
    }
}

void sendMessagesToAllClients(ClientLinkedList *author, char *buffer, uint32_t bufferLength) {
    int temp_fd = author->fd;
    for (int j = 0; j < poll_size; j++) {
        if (sockets[j].fd != sockfd && sockets[j].fd != temp_fd) {
            printf("sockets: %d, client: %d\n", sockets[j].fd, first->next->fd);
            fflush(stdout);
            n = write(sockets[j].fd, &bufferLength, sizeof(int));
            if (n < 0) {
                if (errno != EWOULDBLOCK) {
                    perror("ERROR reading from socket");
                    exit(1);
                }
            }
            printf("size\n");
            fflush(stdout);
            n = write(sockets[j].fd, buffer, bufferLength);
            if (n < 0) {
                if (errno != EWOULDBLOCK) {
                    perror("ERROR reading from socket");
                    exit(1);
                }
            }
            printf("msg\n");
            fflush(stdout);
        }
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


