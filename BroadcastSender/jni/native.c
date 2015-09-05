#include <string.h>
#include <jni.h>
#include <android/log.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <time.h>
#include <stdio.h>

#define DEBUG_TAG "BroadcastSender"
#define MAXRECVSTRING 255

jint Java_com_weiwei_broadcastsender_MainActivity_sendBroadcast(JNIEnv *env,
		jobject obj, jstring jBroadcastIP, jint jPort, jstring jMessage) {
	    const char *broadcastIP = (*env)->GetStringUTFChars(env, jBroadcastIP, 0);
		const char *sendString = (*env)->GetStringUTFChars(env, jMessage, 0);


		int sock; /* Socket */
		struct sockaddr_in broadcastAddr; /* Broadcast address */
		unsigned short broadcastPort = (unsigned short)jPort; /* Server port */
		int broadcastPermission; /* Socket opt to set permission to broadcast */
		unsigned int sendStringLen; /* Length of string to broadcast */

		/* Create socket for sending/receiving datagrams */
		/*IPPROTO_UDP*/
		if ((sock = socket(PF_INET, SOCK_DGRAM, IPPROTO_UDP)) < 0) {
			return 1;
			/*DieWithError("socket() failed");*/
		}

		/* Set socket to allow broadcast */
		broadcastPermission = 1;

		if (setsockopt(sock, SOL_SOCKET, SO_BROADCAST,
				(void *) &broadcastPermission, sizeof(broadcastPermission)) < 0)
			return 2;
		/* DieWithError("setsockopt() failed"); */

		/* Construct local address structure */
		memset(&broadcastAddr, 0, sizeof(broadcastAddr)); /* Zero out structure */
		broadcastAddr.sin_family = AF_INET; /* Internet address family */
		broadcastAddr.sin_addr.s_addr = inet_addr(broadcastIP);/* Broadcast IP address */
		broadcastAddr.sin_port = htons(broadcastPort); /* Broadcast port */

		sendStringLen = strlen(sendString); /* Find length of sendString */

	    int i = 0;
		for(i; i < 5; i++){
		/* Broadcast sendString in datagram to clients every 3 seconds*/
		if (sendto(sock, sendString, sendStringLen, 0,
				(struct sockaddr *) &broadcastAddr, sizeof(broadcastAddr))
				!= sendStringLen)
			return 3;
		/* DieWithError("sendto() sent a different number of bytes than expected"); */
		sleep(3);
		}

		(*env)->ReleaseStringUTFChars(env, jBroadcastIP, broadcastIP);
		(*env)->ReleaseStringUTFChars(env, jMessage, sendString);

		return 0;
		/* NOT REACHED */
}
