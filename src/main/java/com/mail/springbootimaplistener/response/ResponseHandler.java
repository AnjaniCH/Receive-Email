package com.mail.springbootimaplistener.response;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ResponseHandler {
  public static ResponseEntity<Object> generateResponse(Boolean status, HttpStatus code, String message, Object responseObj) {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("status", status);
    map.put("code", code.value());
    if(!status){
      map.put("message", message);
    } else {
      map.put("data", responseObj);
    }

    return new ResponseEntity<Object>(map, code);
  }
}
