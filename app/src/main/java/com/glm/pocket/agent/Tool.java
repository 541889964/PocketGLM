package com.glm.pocket.agent;

import org.json.JSONObject;

public interface Tool{
 String name();
 String desc();
 String schema();
 String run(JSONObject args) throws Exception;
 default boolean dangerous(){return false;} // true=需approval闸门
}
