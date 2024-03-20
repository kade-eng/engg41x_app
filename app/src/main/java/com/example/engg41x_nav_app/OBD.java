package com.example.engg41x_nav_app;

public class OBD {
    private String code = "";
    private String level = "";
    private String desc = "";
    public OBD(String theCode, String theLevel) {
        code = theCode;
        level = theLevel;
        desc = "";
    }
    public OBD(String theCode, String theLevel, String theDesc) {
        code = theCode;
        level = theLevel;
        desc = theDesc;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
