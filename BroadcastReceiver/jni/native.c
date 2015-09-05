#include <string.h>
#include <jni.h>
#include <android/log.h>
#include <sys/socket.h> /* for socket() and bind() */
#include <arpa/inet.h>  /* for sockaddr_in */
#include <stdlib.h>     /* for atoi() and exit() */
#include <string.h>     /* for memset() */
#include <unistd.h>     /* for close() */
#include <sys/types.h>
#include <netinet/in.h>
#include <time.h>
#include <stdio.h>
#include <errno.h>

#define DEBUG_TAG "BroadcastReceiver"
#define MAXRECVSTRING 255

jint Java_com_weiwei_broadcastreceiver_MainActivity_receiveBroadcast(
		JNIEnv *env, jobject obj, jstring jPort) {

	const char *port = (*env)->GetStringUTFChars(env, jPort, 0);

	int sock; /* Socket */
	struct sockaddr_in broadcastAddr; /* Broadcast Address */
	unsigned short broadcastPort; /* Port */
	char recvString[MAXRECVSTRING + 1]; /* Buffer for received string */
	int recvStringLen; /* Length of received string */

	broadcastPort = atoi(port); /* First arg: broadcast port */

	/* Create a best-effort datagram socket using UDP */
	if ((sock = socket(PF_INET, SOCK_DGRAM, IPPROTO_UDP)) < 0) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG,
				"socket()fails:%d", errno);
		return 1;
	}

	/* Construct bind structure */
	memset(&broadcastAddr, 0, sizeof(broadcastAddr)); /* Zero out structure */
	broadcastAddr.sin_family = AF_INET; /* Internet address family */
	broadcastAddr.sin_addr.s_addr = htonl(INADDR_ANY); /* Any incoming interface */
	broadcastAddr.sin_port = htons(broadcastPort); /* Broadcast port */

	/* Bind to the broadcast port */
	if (bind(sock, (struct sockaddr *) &broadcastAddr, sizeof(broadcastAddr))
			< 0) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG,
						"bind() fails:%d", errno);
				return 2;
	}

	while(1) {

	/* Receive a single datagram from the server */
	if ((recvStringLen = recvfrom(sock, recvString, MAXRECVSTRING, 0, NULL, 0))
			< 0) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG,
						"recvfrom() fails:%d", errno);
				return 3;
	}

	recvString[recvStringLen] = '\0';

	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "got broadcast packet, message = %s",
			  recvString);


	}
	(*env)->ReleaseStringUTFChars(env, jPort, port);
	return 0;
}
