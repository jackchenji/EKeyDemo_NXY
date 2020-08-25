/*
 * froadLog.h
 *
 *  Created on: 2014-3-14
 *      Author: zhanghaihua
 */
#include <android/log.h>

#ifndef _FROADLOG_H_
#define _FROADLOG_H_

void setLogLevel(int level);
int LOGD(char* fm, ...);
int LOGV(char* fm, ...);
int LOGE(char* fm, ...);
int LOGI(char* fm, ...);
int LOGW(char* fm, ...);

#endif /* FROADLOG_H_ */