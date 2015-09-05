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

#define DEBUG_TAG "MulticastSender"
#define MAXRECVSTRING 255

jint Java_com_weiwei_multicastsender_MainActivity_sendMulticast(JNIEnv *env,
		jobject obj, jstring jMulticastIP, jstring jPort, jstring jMessage) {

	const char *multicastIp = (*env)->GetStringUTFChars(env, jMulticastIP, 0);
	const char *port = (*env)->GetStringUTFChars(env, jPort, 0);
	const char *sendString = (*env)->GetStringUTFChars(env, jMessage, 0);

	unsigned short multicastPort = atoi(port);

	struct in_addr localInterface;
	struct sockaddr_in addr;
	int addrlen, sock, cnt;
	struct ip_mreq mreq;

	/* set up socket */
	sock = socket(AF_INET, SOCK_DGRAM, 0);
	if (sock < 0) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "%s",
				" error: socket()");
		return 1;
	}
	bzero((char *) &addr, sizeof(addr));
	addr.sin_family = AF_INET;
	addr.sin_addr.s_addr = htonl(INADDR_ANY);
	addr.sin_port = htons(multicastPort);
	addrlen = sizeof(addr);

	addr.sin_addr.s_addr = inet_addr(multicastIp);

	int i = 0;
	for (i; i < 5; i++) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "sending:%s", sendString);

		cnt = sendto(sock, sendString, strlen(sendString), 0,
				(struct sockaddr *) &addr, addrlen);

		if (cnt < 0) {
			__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "%s",
					" error: sendto ");
			return 2;
		}
		sleep(3);
	}

	(*env)->ReleaseStringUTFChars(env, jMulticastIP, multicastIp);
	(*env)->ReleaseStringUTFChars(env, jPort, port);
	(*env)->ReleaseStringUTFChars(env, jMessage, sendString);
	return 0;
}
