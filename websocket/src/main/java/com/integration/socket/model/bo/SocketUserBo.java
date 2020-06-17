package com.integration.socket.model.bo;

import lombok.Getter;

import javax.websocket.Session;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * @author 蒋文龙(Vin)
 * @description
 * @date 2020/5/29
 */

public class SocketUserBo extends UserBo {

    private static final String USER_NAME_PREFIX = "name=";
    private static final String USER_ID_PREFIX = "id=";

    @Getter
    private Session session;

    private SocketUserBo(Session session, String username, String userId) {
        super(username, session.getId());
        this.session = session;
        this.setUserId(userId);
    }

    private static String getQueryParam(String name, String[] queryInfos) throws UnsupportedEncodingException {
        for (String queryInfo : queryInfos) {
            if (queryInfo.startsWith(name)) {
                return URLDecoder.decode(queryInfo.substring(name.length()), "utf-8");
            }
        }
        return null;
    }

    public static SocketUserBo convertSocketUserBo(Session session) throws UnsupportedEncodingException {
        String[] queryInfos = session.getQueryString().split("&");
        String username = getQueryParam(USER_NAME_PREFIX, queryInfos);
        if (username == null) {
            return null;
        }
        String userId = getQueryParam(USER_ID_PREFIX, queryInfos);
        return new SocketUserBo(session, username, userId);
    }

    public static String getUsernameFromSession(Session session) throws UnsupportedEncodingException {
        String[] queryInfos = session.getQueryString().split("&");
        return getQueryParam(USER_NAME_PREFIX, queryInfos);
    }
}