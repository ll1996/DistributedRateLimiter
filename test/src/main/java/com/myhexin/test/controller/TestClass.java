package com.myhexin.test.controller;

import java.util.HashMap;
import java.util.Map;

public class TestClass {

    private Object aaa(){
        Map<String,Object> result = new HashMap<>();
        result.put("code",-1);
        result.put("message","被限制了");
        result.put("data",null);
        return result;
    }
}
