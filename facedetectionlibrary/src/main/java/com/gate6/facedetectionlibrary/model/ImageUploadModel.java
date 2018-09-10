package com.gate6.facedetectionlibrary.model;

public class ImageUploadModel implements AppBeanData {

    public String message;
    public int status;
    public ImageDataModel data;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public ImageDataModel getData() {
        return data;
    }

    public void setData(ImageDataModel data) {
        this.data = data;
    }
}
