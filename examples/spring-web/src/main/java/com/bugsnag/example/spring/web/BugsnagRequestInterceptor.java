package com.bugsnag.example.spring.web;

import com.bugsnag.Report;
import com.bugsnag.callbacks.Callback;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Component
public class BugsnagRequestInterceptor extends HandlerInterceptorAdapter implements Callback {

    private static final String HEADER_X_FORWARDED_FOR = "X-FORWARDED-FOR";

    private static final ThreadLocal<Map<String, Object>> REQUEST_METADATA =
            new ThreadLocal<Map<String, Object>>();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        Map<String, Object> map = new HashMap<>();
        map.put("url", request.getRequestURL().toString());
        map.put("method", request.getMethod());
        map.put("params", request.getParameterMap());
        map.put("clientIp", getClientIp(request));
        map.put("headers", getHeaderMap(request));
        REQUEST_METADATA.set(map);
        return true;
    }

    @Override
    public void beforeNotify(Report report) {
        Map<String, Object> map = REQUEST_METADATA.get();
        report.addToTab("request", "url", map.get("url"));
        report.addToTab("request", "method", map.get("method"));
        report.addToTab("request", "params", map.get("params"));
        report.addToTab("request", "clientIp", map.get("clientIp"));
        report.addToTab("request", "headers", map.get("headers"));
    }

    // FIXME copied from bugsnag code
    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        String forwardedAddr = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (forwardedAddr != null) {
            remoteAddr = forwardedAddr;
            int idx = remoteAddr.indexOf(',');
            if (idx > -1) {
                remoteAddr = remoteAddr.substring(0, idx);
            }
        }
        return remoteAddr;
    }

    private Map<String, String> getHeaderMap(HttpServletRequest request) {
        Map<String, String> map = new HashMap<String, String>();
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            map.put(key, request.getHeader(key));
        }

        return map;
    }

}
