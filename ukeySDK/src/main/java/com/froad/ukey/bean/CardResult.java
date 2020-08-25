package com.froad.ukey.bean;

/**
 * 发送指令后的结果状态
 *
 */
public class CardResult {

      private int apduId = 0x00;// 指令码

      private int resCode = -1;// 指令交互状态码

      private int insideCode = 0;// 数据层交互状态码

      private String apduDecData = "";// 解密后的LV格式数据

      private String data = "";// 卡片返回的数据信息

      private String sw = "";//状态值

      private String errMsg = "";//当前流程错误原因

      public int getResCode() {
            return resCode;
      }

      public void setResCode(int resCode) {
            this.resCode = resCode;
      }

      public int getApduId() {
            return apduId;
      }

      public void setApduId(int apduId) {
            this.apduId = apduId;
      }

      public String getApduDecData() {
            return apduDecData;
      }

      public void setApduDecData(String apduDecData) {
            this.apduDecData = apduDecData;
      }

      public String getData() {
            return data;
      }

      public void setData(String data) {
            this.data = data;
      }

      public String getErrMsg() {
            return errMsg;
      }

      public void setErrMsg(String errMsg) {
            this.errMsg = errMsg;
      }

      public int getInsideCode() {
            return insideCode;
      }

      public void setInsideCode(int insideCode) {
            this.insideCode = insideCode;
      }

      public String getSw() {
            return sw;
      }

      public void setSw(String sw) {
            this.sw = sw;
      }
}
