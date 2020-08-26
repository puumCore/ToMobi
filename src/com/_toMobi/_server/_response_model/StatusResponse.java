package com._toMobi._server._response_model;

/**
 * @author Mandela
 */
public enum StatusResponse {

    SUCCESS ("Success"), ERROR ("Error"), WARNING("Warning");

    public String status;

    StatusResponse(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

}
