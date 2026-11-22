package com.glm.pocket.agent;

import com.glm.pocket.*;
import com.glm.pocket.agent.loader.*;
import com.glm.pocket.agent.tools.*;
import com.glm.pocket.net.*;
import com.glm.pocket.self.*;
import java.io.*;
import java.util.*;
import java.text.*;
import android.app.*;
import android.content.*;
import android.database.*;
import android.net.*;
import android.os.*;
import android.provider.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import android.media.*;
import android.accessibilityservice.*;

import org.json.JSONObject;

public interface Tool{
 String name();
 String desc();
 String schema();
 String run(JSONObject args) throws Exception;
 default boolean dangerous(){return false;} // true=需approval闸门
}
