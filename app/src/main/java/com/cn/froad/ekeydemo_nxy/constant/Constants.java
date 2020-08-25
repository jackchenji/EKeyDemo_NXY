package com.cn.froad.ekeydemo_nxy.constant;

/**
 * Created by FW on 2017/6/30.
 */

public class Constants {

    public final static int PERMISSION_RATION = 0;
    public final static int PERMISSION_GRANTED = 1;
    public final static int PERMISSION_DENIED = 2;

    /**
     * 联系人相关权限,包含
     * permission:android.permission.WRITE_CONTACTS
     * permission:android.permission.GET_ACCOUNTS
     * permission:android.permission.READ_CONTACTS
     */
    public final static int PERMISSION_GROUP_CONTACTS = 0;
    /**
     * 拨打电话，手机信息相关权限,包含
     * permission:android.permission.READ_CALL_LOG
     * permission:android.permission.READ_PHONE_STATE
     * permission:android.permission.CALL_PHONE
     * permission:android.permission.WRITE_CALL_LOG
     * permission:android.permission.USE_SIP
     * permission:android.permission.PROCESS_OUTGOING_CALLS
     * permission:com.android.voicemail.permission.ADD_VOICEMAIL
     */
    public final static int PERMISSION_GROUP_PHONE = 1;
    /**
     * 读写日历相关权限,包含
     * permission:android.permission.READ_CALENDAR
     * permission:android.permission.WRITE_CALENDAR
     */
    public final static int PERMISSION_GROUP_CALENDAR = 2;
    /**
     * 照相机相关权限,包含
     * permission:android.permission.CAMERA
     */
    public final static int PERMISSION_GROUP_CAMERA = 3;
    /**
     * 传感器相关权限,包含
     * permission:android.permission.BODY_SENSORS
     */
    public final static int PERMISSION_GROUP_SENSORS = 4;
    /**
     * 定位相关权限,包含
     * permission:android.permission.ACCESS_FINE_LOCATION
     * permission:android.permission.ACCESS_COARSE_LOCATION
     */
    public final static int PERMISSION_GROUP_LOCATION = 5;
    /**
     * SD储存相关权限,包含
     * permission:android.permission.READ_EXTERNAL_STORAGE
     * permission:android.permission.WRITE_EXTERNAL_STORAGE
     */
    public final static int PERMISSION_GROUP_STORAGE = 6;
    /**
     * 麦克风相关权限,包含
     * permission:android.permission.RECORD_AUDIO
     */
    public final static int PERMISSION_GROUP_MICROPHONE = 7;
    /**
     * 短信相关权限,包含
     * permission:android.permission.READ_SMS
     * permission:android.permission.RECEIVE_WAP_PUSH
     * permission:android.permission.RECEIVE_MMS
     * permission:android.permission.RECEIVE_SMS
     * permission:android.permission.SEND_SMS
     * permission:android.permission.READ_CELL_BROADCASTS
     */
    public final static int PERMISSION_GROUP_SMS = 8;
}
