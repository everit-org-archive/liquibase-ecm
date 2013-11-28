package org.everit.osgi.liquibase.tests;

/*
 * Copyright (c) 2011, Everit Kft.
 *
 * All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.everit.osgi.dev.testrunner.TestDuringDevelopment;
import org.everit.osgi.liquibase.component.LiquibaseService;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.BundleContext;

@Component(name = "LiquibaseTest", immediate = true)
@Service(value = LiquibaseTestComponent.class)
@Properties({ @Property(name = "eosgi.testEngine", value = "junit4"),
        @Property(name = "eosgi.testId", value = "liquibaseTest") })
public class LiquibaseTestComponent {

    @Reference
    private ConfigurationInitComponent configInit;

    @Reference
    private LiquibaseService liquibaseService;

    @Reference
    private DataSource dataSource;

    private BundleContext bundleContext;

    @Activate
    public void activate(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void bindConfigInit(final ConfigurationInitComponent configInit) {
        this.configInit = configInit;
    }

    public void bindDataSource(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void bindLiquibaseService(final LiquibaseService liquibaseService) {
        this.liquibaseService = liquibaseService;
    }

    private void dropAll() {
        Database database = null;
        try {
            Connection connection = dataSource.getConnection();
            database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(
                    new JdbcConnection(connection));
            database.setDefaultCatalogName("TEST");
            database.setDefaultSchemaName("public");
            Liquibase liquibase =
                    new Liquibase(null, null, database);
            liquibase.dropAll();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (database != null) {
                try {
                    database.close();
                } catch (DatabaseException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Test
    @TestDuringDevelopment
    public void testDatabaseCreation() {
        liquibaseService.process(dataSource, bundleContext, "META-INF/liquibase/changelog.xml");

        dropAll();
    }

    @Test
    public void testProcessTwiceCreation() {
        liquibaseService.process(dataSource, bundleContext, "META-INF/liquibase/changelog.xml");

        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            Statement insertStatement = connection.createStatement();
            insertStatement.executeUpdate("insert into person (firstName, lastName) values ('John', 'Doe')");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        liquibaseService.process(dataSource, bundleContext, "META-INF/liquibase/changelog.xml");

        try {
            connection = dataSource.getConnection();
            Statement queryStatement = connection.createStatement();
            ResultSet resultSet = queryStatement.executeQuery("select firstName from person where lastName = 'Doe'");
            Assert.assertEquals(true, resultSet.first());
            String firstName = resultSet.getString(1);
            Assert.assertEquals("John", firstName);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        dropAll();
    }
}
