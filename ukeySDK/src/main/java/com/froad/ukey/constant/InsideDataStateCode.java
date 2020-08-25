package com.froad.ukey.constant;

/**
 * Created by FW on 2017/4/6.
 */
public class InsideDataStateCode {

    public final static String RES_SUCCESS = "9000";//成功
    public final static String RES_SUCCESS_CONTINUE = "61";//成功,继续接收(发送固定接收指令B1C00000+le)
    public final static String RES_SUCCESS_CONTINUE_6C = "6C";//成功,继续接收(将前一条获取数据指令最后的le替换为返回的le然后再发送)
    public final static String RES_FAIL_VERIFY_PIN_ERROE = "63C";//PIN码校验错误，剩余
    public final static String RES_FAIL_LC_LEN_ERROE = "6700";//Lc长度不正确
    public final static String RES_FAIL_CARD_NOT_STARTUP = "6891";//V盾未启用
    public final static String RES_FAIL_CARD_NOT_INIT = "6980";//白卡，未个人化
    public final static String RES_FAIL_CREATR_KEYPAIR_ERROR_HAS_CER = "6981";//已生成证书，不能再生成密钥对
    public final static String RES_FAIL_CHECK_PERMISSION = "6982";//权限检查失败
    public final static String RES_FAIL_CARD_LOCK = "6983";//卡锁定
    public final static String RES_FAIL_INVALID_RANDOM = "6984";//随机数无效
    public final static String RES_FAIL_MAC_ERROR = "6985";//MAC错误
    public final static String RES_FAIL_KEY_LEN_MISMATCH = "6987";//指令请求与密钥长度不符
    public final static String RES_FAIL_WRITE_CER_NO_KEYPAIR = "6988";//未生成密钥对不能写入证书
    public final static String RES_FAIL_SEND_INSIDEDATA_TOO_LONG = "6989";//待写入数据越界，超出卡片缓存空间
    public final static String RES_FAIL_INSTRUCTIONS_FORMAT_ERROR = "698A";//指令格式错误
    public final static String RES_FAIL_KEYPAIR_FORMAT_ERROR = "698B";//密钥对格式错误
    public final static String RES_FAIL_PUBLICKEY_ABSTRACT_UNFINISHED = "698C";//未完成公钥摘要计算
    public final static String RES_FAIL_ABSTRACT_SIGN_MISMATCH = "698D";//摘要类型与签名类型不匹配
    public final static String RES_FAIL_WRITE_CER_IDENTICAL = "698E";//不能重复写入相同类型的证书
    public final static String RES_FAIL_PIN_VERIFY_NOT_PASSED = "698F";//PIN未校验通过
    public final static String RES_FAIL_NOT_STARTUP_CARD = "6990";//不启动V盾
    public final static String RES_FAIL_NO_WRITE_CER = "6996";//未预制任何证书
    public final static String RES_SUCCESS_CHANGE_INITPIN_YES = "6997";//已修改过PIN码
    public final static String RES_SUCCESS_CHANGE_INITPIN_NO = "6998";//未修改过初始PIN码
    public final static String RES_SUCCESS_ClOSEMODEL_OPEN = "6999";//确认开启V盾自动关闭模式
    public final static String RES_SUCCESS_CLOSEMODEL_CLOSE = "699A";//确认关闭V盾自动关闭模式
    public final static String RES_FAIL_RESET_PWD_VERIFY = "699D";//密码重置验证失败
    public final static String RES_FAIL_ALGORITHM_NONSUPPORT = "6A80";//算法不支持
    public final static String RES_FAIL_ABSTRACT_ALGORITHM_NOFILTER = "6A81";//算法类型不匹配
    public final static String RES_FAIL_ABSTRACT_ALGORITHM_NONSUPPORT = "6A83";//不支持的算法
    public final static String RES_FAIL_P1_P2_ERROR = "6A86";//P1P2错误
    public final static String RES_FAIL_APPOINT_CER_NONEXIST = "6A89";//指定的当前证书不存在
    public final static String RES_FAIL_ORDER_NOT_RECOGNIZED = "6D00";//指令数据不识别
    public final static String RES_FAIL_UNDER_INTERFACE_ERROR = "6F00";//底层接口运算错误
    public final static String RES_FAIL_OTHER_CASE = "6F01";//其他未知错误
    public final static String RES_SUCCESS_STK_RUNNING = "9300";//STK界面正在显示
    public final static String RES_SUCCESS_STK_NORESPONCE = "9301";//卡片忙，需要重新发送数据

    public final static String RES_SUCCESS_STR = "接收数据成功";
    public final static String RES_SUCCESS_CONTINUE_61_STR1 = "接收数据成功，继续接收 ";
    public final static String RES_SUCCESS_CONTINUE_61_STR2 = " 个字节数据";
    public final static String RES_SUCCESS_CONTINUE_6C_STR1 = "接收数据成功，继续接收 ";
    public final static String RES_SUCCESS_CONTINUE_6C_STR2 = " 个字节数据";
    public final static String RES_FAIL_VERIFY_PIN_ERROE_STR1 = "密码错误，还剩 ";
    public final static String RES_FAIL_VERIFY_PIN_ERROE_STR2 = " 次机会";
    public final static String RES_FAIL_LC_LEN_ERROE_STR = "Lc长度不正确";
    public final static String RES_FAIL_CARD_NOT_STARTUP_STR = "V盾未启用";
    public final static String RES_FAIL_CARD_NOT_INIT_STR = "白卡，未个人化";
    public final static String RES_FAIL_CREATR_KEYPAIR_ERROR_HAS_CER_STR = "已生成证书，不能再生成密钥对";
    public final static String RES_FAIL_CHECK_PERMISSION_STR = "权限检查失败";
    public final static String RES_FAIL_CARD_LOCK_STR = "卡锁定";
    public final static String RES_FAIL_INVALID_RANDOM_STR = "随机数无效";
    public final static String RES_FAIL_MAC_ERROR_STR = "MAC错误";
    public final static String RES_FAIL_KEY_LEN_MISMATCH_STR = "指令请求与密钥长度不符";
    public final static String RES_FAIL_WRITE_CER_NO_KEYPAIR_STR = "未生成密钥对不能写入证书";
    public final static String RES_FAIL_SEND_INSIDEDATA_TOO_LONG_STR = "待写入数据越界，超出卡片缓存空间";
    public final static String RES_FAIL_INSTRUCTIONS_FORMAT_ERROR_STR = "指令格式错误";
    public final static String RES_FAIL_KEYPAIR_FORMAT_ERROR_STR = "密钥对格式错误";
    public final static String RES_FAIL_PUBLICKEY_ABSTRACT_UNFINISHED_STR = "未完成公钥摘要计算";
    public final static String RES_FAIL_ABSTRACT_SIGN_MISMATCH_STR = "摘要类型与签名类型不匹配";
    public final static String RES_FAIL_WRITE_CER_IDENTICAL_STR = "不能重复写入相同类型的证书";
    public final static String RES_FAIL_PIN_VERIFY_NOT_PASSED_STR = "PIN未校验通过";
    public final static String RES_FAIL_NOT_STARTUP_CARD_STR = "不启动V盾";
    public final static String RES_FAIL_NO_WRITE_CER_STR = "未预制任何证书";
    public final static String RES_SUCCESS_CHANGE_INITPIN_YES_STR = "已修改过初始密码";
    public final static String RES_SUCCESS_CHANGE_INITPIN_NO_STR = "未修改初始密码";
    public final static String RES_SUCCESS_ClOSEMODEL_OPEN_STR = "确认开启V盾自动关闭功能";
    public final static String RES_SUCCESS_ClOSEMODEL_CLOSE_STR = "确认关闭V盾自动关闭功能";
    public final static String RES_FAIL_RESET_PWD_VERIFY_STR = "密码重置验证失败";
    public final static String RES_FAIL_ALGORITHM_NONSUPPORT_STR = "算法不支持";
    public final static String RES_FAIL_ABSTRACT_ALGORITHM_NOFILTER_STR = "算法类型不匹配";
    public final static String RES_FAIL_ABSTRACT_ALGORITHM_NONSUPPORT_STR = "不支持的算法";
    public final static String RES_FAIL_P1_P2_ERROR_STR = "P1P2错误";
    public final static String RES_FAIL_APPOINT_CER_NONEXIST_STR = "指定的当前证书不存在";
    public final static String RES_FAIL_ORDER_NOT_RECOGNIZED_STR = "指令数据不识别";
    public final static String RES_FAIL_UNDER_INTERFACE_ERROR_STR = "底层接口运算错误";
    public final static String RES_FAIL_OTHER_CASE_STR = "其他未知错误";

    public static String getInsideErrorMsg (String eCode) {
        if (RES_SUCCESS.equalsIgnoreCase(eCode)) {
            return RES_SUCCESS_STR;
        } else if (eCode.contains(RES_SUCCESS_CONTINUE)) {
            int eCodeLen = eCode.length();
            return RES_SUCCESS_CONTINUE_61_STR1 + Integer.parseInt(eCode.substring(eCodeLen - 2), 16) + RES_SUCCESS_CONTINUE_61_STR2;
        } else if (eCode.contains(RES_SUCCESS_CONTINUE_6C)) {
            int eCodeLen = eCode.length();
            return RES_SUCCESS_CONTINUE_6C_STR1 + Integer.parseInt(eCode.substring(eCodeLen - 2), 16) + RES_SUCCESS_CONTINUE_6C_STR2;
        } else if (RES_FAIL_LC_LEN_ERROE.equalsIgnoreCase(eCode)) {
            return RES_FAIL_LC_LEN_ERROE_STR;
        } else if (RES_FAIL_CARD_NOT_STARTUP.equalsIgnoreCase(eCode)) {
            return RES_FAIL_CARD_NOT_STARTUP_STR;
        } else if (RES_FAIL_CARD_NOT_INIT.equalsIgnoreCase(eCode)) {
            return RES_FAIL_CARD_NOT_INIT_STR;
        } else if (RES_FAIL_CREATR_KEYPAIR_ERROR_HAS_CER.equalsIgnoreCase(eCode)) {
            return RES_FAIL_CREATR_KEYPAIR_ERROR_HAS_CER_STR;
        } else if (RES_FAIL_CHECK_PERMISSION.equalsIgnoreCase(eCode)) {
            return RES_FAIL_CHECK_PERMISSION_STR;
        } else if (RES_FAIL_CARD_LOCK.equalsIgnoreCase(eCode)) {
            return RES_FAIL_CARD_LOCK_STR;
        } else if (RES_FAIL_INVALID_RANDOM.equalsIgnoreCase(eCode)) {
            return RES_FAIL_INVALID_RANDOM_STR;
        } else if (RES_FAIL_MAC_ERROR.equalsIgnoreCase(eCode)) {
            return RES_FAIL_MAC_ERROR_STR;
        } else if (RES_FAIL_KEY_LEN_MISMATCH.equalsIgnoreCase(eCode)) {
            return RES_FAIL_KEY_LEN_MISMATCH_STR;
        } else if (RES_FAIL_WRITE_CER_NO_KEYPAIR.equalsIgnoreCase(eCode)) {
            return RES_FAIL_WRITE_CER_NO_KEYPAIR_STR;
        } else if (RES_FAIL_SEND_INSIDEDATA_TOO_LONG.equalsIgnoreCase(eCode)) {
            return RES_FAIL_SEND_INSIDEDATA_TOO_LONG_STR;
        } else if (RES_FAIL_P1_P2_ERROR.equalsIgnoreCase(eCode)) {
            return RES_FAIL_P1_P2_ERROR_STR;
        } else if (RES_FAIL_INSTRUCTIONS_FORMAT_ERROR.equalsIgnoreCase(eCode)) {
            return RES_FAIL_INSTRUCTIONS_FORMAT_ERROR_STR;
        } else if (RES_FAIL_KEYPAIR_FORMAT_ERROR.equalsIgnoreCase(eCode)) {
            return RES_FAIL_KEYPAIR_FORMAT_ERROR_STR;
        } else if (RES_FAIL_PUBLICKEY_ABSTRACT_UNFINISHED.equalsIgnoreCase(eCode)) {
            return RES_FAIL_PUBLICKEY_ABSTRACT_UNFINISHED_STR;
        } else if (RES_FAIL_ABSTRACT_SIGN_MISMATCH.equalsIgnoreCase(eCode)) {
            return RES_FAIL_ABSTRACT_SIGN_MISMATCH_STR;
        } else if (RES_FAIL_WRITE_CER_IDENTICAL.equalsIgnoreCase(eCode)) {
            return RES_FAIL_WRITE_CER_IDENTICAL_STR;
        } else if (RES_FAIL_PIN_VERIFY_NOT_PASSED.equalsIgnoreCase(eCode)) {
            return RES_FAIL_PIN_VERIFY_NOT_PASSED_STR;
        } else if (RES_FAIL_NOT_STARTUP_CARD.equalsIgnoreCase(eCode)) {
            return RES_FAIL_NOT_STARTUP_CARD_STR;
        } else if (RES_FAIL_NO_WRITE_CER.equalsIgnoreCase(eCode)) {
            return RES_FAIL_NO_WRITE_CER_STR;
        } else if (RES_SUCCESS_CHANGE_INITPIN_YES.equalsIgnoreCase(eCode)) {
            return RES_SUCCESS_CHANGE_INITPIN_YES_STR;
        } else if (RES_SUCCESS_CHANGE_INITPIN_NO.equalsIgnoreCase(eCode)) {
            return RES_SUCCESS_CHANGE_INITPIN_NO_STR;
        } else if (RES_SUCCESS_ClOSEMODEL_OPEN.equalsIgnoreCase(eCode)) {
            return RES_SUCCESS_ClOSEMODEL_OPEN_STR;
        } else if (RES_SUCCESS_CLOSEMODEL_CLOSE.equalsIgnoreCase(eCode)) {
            return RES_SUCCESS_ClOSEMODEL_CLOSE_STR;
        } else if (RES_FAIL_RESET_PWD_VERIFY.equalsIgnoreCase(eCode)) {
            return RES_FAIL_RESET_PWD_VERIFY_STR;
        } else if (RES_FAIL_ALGORITHM_NONSUPPORT.equalsIgnoreCase(eCode)) {
            return RES_FAIL_ALGORITHM_NONSUPPORT_STR;
        } else if (RES_FAIL_ABSTRACT_ALGORITHM_NONSUPPORT.equalsIgnoreCase(eCode)) {
            return RES_FAIL_ABSTRACT_ALGORITHM_NONSUPPORT_STR;
        } else if (RES_FAIL_ABSTRACT_ALGORITHM_NOFILTER.equalsIgnoreCase(eCode)) {
            return RES_FAIL_ABSTRACT_ALGORITHM_NOFILTER_STR;
        } else if (RES_FAIL_APPOINT_CER_NONEXIST.equalsIgnoreCase(eCode)) {
            return RES_FAIL_APPOINT_CER_NONEXIST_STR;
        } else if (RES_FAIL_ORDER_NOT_RECOGNIZED.equalsIgnoreCase(eCode)) {
            return RES_FAIL_ORDER_NOT_RECOGNIZED_STR;
        } else if (RES_FAIL_UNDER_INTERFACE_ERROR.equalsIgnoreCase(eCode)) {
            return RES_FAIL_UNDER_INTERFACE_ERROR_STR;
        } else if (eCode.toUpperCase().contains(RES_FAIL_VERIFY_PIN_ERROE)) {
            int eCodeLen = eCode.length();
            return RES_FAIL_VERIFY_PIN_ERROE_STR1 + eCode.substring(eCodeLen - 1) + RES_FAIL_VERIFY_PIN_ERROE_STR2;
        } else {
            return RES_FAIL_OTHER_CASE_STR;
        }
    }
}
