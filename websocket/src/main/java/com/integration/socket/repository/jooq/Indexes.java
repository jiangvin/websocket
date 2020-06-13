/*
 * This file is generated by jOOQ.
 */
package com.integration.socket.repository.jooq;


import com.integration.socket.repository.jooq.tables.Map;
import com.integration.socket.repository.jooq.tables.User;

import javax.annotation.Generated;

import org.jooq.Index;
import org.jooq.OrderField;
import org.jooq.impl.Internal;


/**
 * A class modelling indexes of tables of the <code></code> schema.
 */
@Generated(
value = {
    "http://www.jooq.org",
    "jOOQ version:3.11.11"
},
comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Indexes {

    // -------------------------------------------------------------------------
    // INDEX definitions
    // -------------------------------------------------------------------------

    public static final Index MAP_NAME_UNIQUE = Indexes0.MAP_NAME_UNIQUE;
    public static final Index MAP_PRIMARY = Indexes0.MAP_PRIMARY;
    public static final Index USER_PRIMARY = Indexes0.USER_PRIMARY;

    // -------------------------------------------------------------------------
    // [#1459] distribute members to avoid static initialisers > 64kb
    // -------------------------------------------------------------------------

    private static class Indexes0 {
        public static Index MAP_NAME_UNIQUE = Internal.createIndex("name_UNIQUE", Map.MAP, new OrderField[] { Map.MAP.NAME }, true);
        public static Index MAP_PRIMARY = Internal.createIndex("PRIMARY", Map.MAP, new OrderField[] { Map.MAP.NAME }, true);
        public static Index USER_PRIMARY = Internal.createIndex("PRIMARY", User.USER, new OrderField[] { User.USER.USER_ID }, true);
    }
}
