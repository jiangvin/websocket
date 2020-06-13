/*
 * This file is generated by jOOQ.
 */
package com.integration.socket.repository.jooq;


import com.integration.socket.repository.jooq.tables.Map;
import com.integration.socket.repository.jooq.tables.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Generated;

import org.jooq.Catalog;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;


/**
 * This class is generated by jOOQ.
 */
@Generated(
value = {
    "http://www.jooq.org",
    "jOOQ version:3.11.11"
},
comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DefaultSchema extends SchemaImpl {

    private static final long serialVersionUID = -1125201586;

    /**
     * The reference instance of <code></code>
     */
    public static final DefaultSchema DEFAULT_SCHEMA = new DefaultSchema();

    /**
     * The table <code>map</code>.
     */
    public final Map MAP = com.integration.socket.repository.jooq.tables.Map.MAP;

    /**
     * The table <code>user</code>.
     */
    public final User USER = com.integration.socket.repository.jooq.tables.User.USER;

    /**
     * No further instances allowed
     */
    private DefaultSchema() {
        super("", null);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Catalog getCatalog() {
        return DefaultCatalog.DEFAULT_CATALOG;
    }

    @Override
    public final List<Table<?>> getTables() {
        List result = new ArrayList();
        result.addAll(getTables0());
        return result;
    }

    private final List<Table<?>> getTables0() {
        return Arrays.<Table<?>>asList(
                   Map.MAP,
                   User.USER);
    }
}
