package bip;

import java.util.ArrayList;

/*
 *Created by chenji on 2020/5/7
 */ public class BipResult {
    public   Boolean isSuccess;
    public   String message;
    public ArrayList<String>  arrayList;


    public ArrayList<String> getArrayList() {
        return arrayList;
    }

    public void setArrayList(ArrayList<String> arrayList) {
        this.arrayList = arrayList;
    }


    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getSuccess() {
        return isSuccess;
    }

    public void setSuccess(Boolean success) {
        isSuccess = success;
    }
}
