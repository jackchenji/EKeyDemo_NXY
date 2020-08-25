#include "froadLog.h"

//LOG控制开关 1-打开，0-关闭
static int isDebug = 0;

void setLogLevel(int level){
	isDebug = level;
}

int LOGI(char* fm, ...){
	va_list ap;
	va_start(ap, fm);

	if(isDebug == 1)
		return (int)(__android_log_vprint(ANDROID_LOG_INFO, "JNI-INFO",fm, ap));

	va_end(ap);
	return -1;
}

int LOGW(char* fm, ...){
	va_list ap;
	va_start(ap, fm);
	if(isDebug == 1)
		return (int)(__android_log_vprint(ANDROID_LOG_WARN, "JNI-WARN",fm, ap));

	va_end(ap);
	return -1;
}

int LOGE(char* fm, ...){
	va_list ap;
	va_start(ap, fm);
	if(isDebug == 1)
		return (int)(__android_log_vprint(ANDROID_LOG_ERROR, "JNI-ERROR",fm, ap));

	va_end(ap);
	return -1;
}

int LOGV(char* fm, ...){
	va_list ap;
	va_start(ap, fm);
	if(isDebug == 1)
		return (int)(__android_log_vprint(ANDROID_LOG_VERBOSE, "JNI-VERBOSE",fm, ap));

	va_end(ap);
	return -1;
}

int LOGD(char* fm, ...){
	va_list ap;
	va_start(ap, fm);
	if(isDebug == 1)
		return (int)(__android_log_vprint(ANDROID_LOG_DEBUG, "JNI-DEBUG",fm, ap));

	va_end(ap);
	return -1;
}

