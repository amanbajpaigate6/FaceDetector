package com.gate6.facedetectionlibrary.utils;

public class ConfigData {

    public String BASE_URL = "http://g6attendance.dev.gate6.com:5000/";
    public String loginUrl = BASE_URL + "loginchange";
    public String signUpUrl = BASE_URL + "userRegister";//"register";
    public String logoutUrl = BASE_URL + "logoutchange";
    public String verifyUserUrl = BASE_URL + "userVerify";
    public String verifyImageUrl = BASE_URL + "userImgUpload";
    public String frameColor = "#0000FF";
    public boolean isFrameShowing = true;
    public String frameLabel = "DETECTING";
    public boolean isFrameLabelShowing = true;
    public boolean isVoiceNeeded = true;
    public int detectionTimerInterval = 5 * 1000; // in milli seconds


    public String getBASE_URL() {
        return BASE_URL;
    }

    public void setBASE_URL(String BASE_URL) {
        this.BASE_URL = BASE_URL;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public String getSignUpUrl() {
        return signUpUrl;
    }

    public void setSignUpUrl(String signUpUrl) {
        this.signUpUrl = signUpUrl;
    }

    public String getLogoutUrl() {
        return logoutUrl;
    }

    public void setLogoutUrl(String logoutUrl) {
        this.logoutUrl = logoutUrl;
    }

    public String getVerifyUserUrl() {
        return verifyUserUrl;
    }

    public void setVerifyUserUrl(String verifyUserUrl) {
        this.verifyUserUrl = verifyUserUrl;
    }

    public String getVerifyImageUrl() {
        return verifyImageUrl;
    }

    public void setVerifyImageUrl(String verifyImageUrl) {
        this.verifyImageUrl = verifyImageUrl;
    }

    public String getFrameColor() {
        return frameColor;
    }

    public void setFrameColor(String frameColor) {
        this.frameColor = frameColor;
    }

    public boolean isFrameShowing() {
        return isFrameShowing;
    }

    public void setFrameShowing(boolean frameShowing) {
        isFrameShowing = frameShowing;
    }

    public String getFrameLabel() {
        return frameLabel;
    }

    public void setFrameLabel(String frameLabel) {
        this.frameLabel = frameLabel;
    }

    public boolean isFrameLabelShowing() {
        return isFrameLabelShowing;
    }

    public void setFrameLabelShowing(boolean frameLabelShowing) {
        isFrameLabelShowing = frameLabelShowing;
    }

    public boolean isVoiceNeeded() {
        return isVoiceNeeded;
    }

    public void setVoiceNeeded(boolean voiceNeeded) {
        isVoiceNeeded = voiceNeeded;
    }

    public int getDetectionTimerInterval() {
        return detectionTimerInterval;
    }

    public void setDetectionTimerInterval(int detectionTimerInterval) {
        this.detectionTimerInterval = detectionTimerInterval;
    }
}
