package com.gate6.facedetectionlibrary.utilsPkg;

public class Constant {

    public static final String SUCCESS = "success";

    /*
    "http://10.1.0.22:5000/image_stream"
//                "http://g6attendance.dev.gate6.com:5000/image_stream"
            "http://10.1.0.158:5000/image_stream"*/
    // local machine server
//    public static String BASE_URL = "http://10.1.0.158:5000/";
    // public server
    public static String BASE_URL = "http://g6attendance.dev.gate6.com:5000/";
    public static  String LOGIN_URL = BASE_URL + "loginchange";
    public static  String SIGNUP_URL =  BASE_URL + "userRegister";//"register";
    public static  String LOGOUT_URL =  BASE_URL + "logoutchange";
    public static  String VERIFY_URL =  BASE_URL + "userVerify";
    public static  String VERIFY__IMAGE_URL =  BASE_URL + "userImgUpload";
    public static  String LOGOUT__QUES_URL =  BASE_URL + "ansQuestion";


}
