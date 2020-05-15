/*
 * This file is generated by jOOQ.
 */
package com.integration.socket.repository.jooq;


import com.integration.socket.repository.jooq.tables.Map;
import com.integration.socket.repository.jooq.tables.records.MapRecord;

import javax.annotation.Generated;

import org.jooq.UniqueKey;
import org.jooq.impl.Internal;


/**
 * A class modelling foreign key relationships and constraints of tables of
 * the <code></code> schema.
 */
@Generated(
value = {
    "http://www.jooq.org",
    "jOOQ version:3.11.4"
},
comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

    // -------------------------------------------------------------------------
    // IDENTITY definitions
    // -------------------------------------------------------------------------


    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<MapRecord> KEY_MAP_PRIMARY = UniqueKeys0.KEY_MAP_PRIMARY;

    // -------------------------------------------------------------------------
    // FOREIGN KEY definitions
    // -------------------------------------------------------------------------


    // -------------------------------------------------------------------------
    // [#1459] distribute members to avoid static initialisers > 64kb
    // -------------------------------------------------------------------------

    private static class UniqueKeys0 {
        public static final UniqueKey<MapRecord> KEY_MAP_PRIMARY = Internal.createUniqueKey(Map.MAP, "KEY_map_PRIMARY", Map.MAP.ID);
    }
}