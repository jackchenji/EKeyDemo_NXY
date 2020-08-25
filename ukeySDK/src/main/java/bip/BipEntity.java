package bip;

/*
 *Created by chenji on 2020/6/3
 */ public class BipEntity {
     String instruct;
    String datas;
    String typeflag;
    String yangzhengma;


    public BipEntity(String datas, String typeflag, String yangzhengma) {
        this.datas = datas;
        this.typeflag = typeflag;
        this.yangzhengma = yangzhengma;
    }


    public BipEntity(String instruct, String datas, String typeflag, String yangzhengma) {
        this.instruct = instruct;
        this.datas = datas;
        this.typeflag = typeflag;
        this.yangzhengma = yangzhengma;
    }




    public String getInstruct() {
        return instruct;
    }

    public void setInstruct(String instruct) {
        this.instruct = instruct;
    }

    public String getDatas() {
        return datas;
    }

    public void setDatas(String datas) {
        this.datas = datas;
    }

    public String getTypeflag() {
        return typeflag;
    }

    public void setTypeflag(String typeflag) {
        this.typeflag = typeflag;
    }

    public String getYangzhengma() {
        return yangzhengma;
    }

    public void setYangzhengma(String yangzhengma) {
        this.yangzhengma = yangzhengma;
    }



}
