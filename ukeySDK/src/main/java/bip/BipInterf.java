package bip;

import com.micronet.api.Result;

import java.util.ArrayList;

/*
 *Created by chenji on 2020/3/11
 *
 */ public interface BipInterf {



    ArrayList<BipResult> onStartBip(String instruct);

    void  onCreate(String instruct);
    boolean  onBind();
    void onStartCommond();
    void onDestroy();
 }
