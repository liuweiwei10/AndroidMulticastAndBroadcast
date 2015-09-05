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

#define DEBUG_TAG "MulticastReceiver"
#define MAXRECVSTRING 255

jint Java_com_example_multicastreceiver_MainActivity_receiveMulticast(
		JNIEnv *env, jobject obj, jstring jMulticastIP, jstring jPort) {

	const char *multicastIp = (*env)->GetStringUTFChars(env, jMulticastIP, 0);
	const char *port = (*env)->GetStringUTFChars(env, jPort, 0);

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

	/* receive */
	if (bind(sock, (struct sockaddr *) &addr, sizeof(addr)) < 0) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "%s", " error: bind");
		return 1;
	}
	mreq.imr_multiaddr.s_addr = inet_addr(multicastIp);
	mreq.imr_interface.s_addr = htonl(INADDR_ANY);
	if (setsockopt(sock, IPPROTO_IP, IP_ADD_MEMBERSHIP, &mreq, sizeof(mreq))
			< 0) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "%s",
				" error: setsockopt mreq");
		return 3;
	}

	int reuse = 1;
	if (setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, (char *) &reuse,
			sizeof(reuse)) < 0) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "%s",
				" Setting SO_REUSEADDR error");

		close(sock);
		return 1;
	} else {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "%s",
				" Setting SO_REUSEADDR...OK");
	}

	while (1) {
		char message[MAXRECVSTRING];
		cnt = recvfrom(sock, message, MAXRECVSTRING, 0,
				(struct sockaddr *) &addr, &addrlen);

		/*	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "cnt = %d", cnt);*/

		if (cnt < 0) {
			__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG,
					"recvfrom error:%d", errno);

			/*return 4*/
		} else if (cnt == 0) {
			break;
		}
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, " %s packet from %s, message = %s",
				multicastIp, inet_ntoa(addr.sin_addr), message);

	}

	(*env)->ReleaseStringUTFChars(env, jMulticastIP, multicastIp);
	(*env)->ReleaseStringUTFChars(env, jPort, port);
	return 0;
}
