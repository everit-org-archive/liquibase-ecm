package org.everit.osgi.liquibase.component.internal;

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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.everit.osgi.liquibase.component.LiquibaseService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.log.LogService;

@Component(metatype = true, immediate = true)
@Properties({
        @Property(name = LiquibaseService.PROP_UPDATE, boolValue = true),
        @Property(name = LiquibaseService.PROP_SQL_DUMP_FOLDER), @Property(name = "logService.target") })
@Service
public class LiquibaseComponent implements LiquibaseService {

    private boolean update = true;

    private String sqlDumpFolder;

    @Reference
    private LogService logService;

    @Activate
    public void activate(Map<String, Object> componentProperties) {
        System.out.println("Activate called");
        modified(componentProperties);
    }

    @Modified
    public void modified(Map<String, Object> componentProperties) {
        System.out.println("Modify called");
        Object tryUpdateObject = componentProperties.get(LiquibaseService.PROP_UPDATE);
        if (tryUpdateObject != null) {
            if (!(tryUpdateObject instanceof Boolean)) {
                throw new RuntimeException("Expected type for tryUpdate is Boolean but got "
                        + tryUpdateObject.getClass());
            }

        }
        Object sqlDumpFolderObject = componentProperties.get(LiquibaseService.PROP_SQL_DUMP_FOLDER);
        if (sqlDumpFolderObject != null) {
            sqlDumpFolder = String.valueOf(sqlDumpFolderObject);
        }
    }

    @Override
    public void process(DataSource dataSource, BundleContext bundleContext, String changeLogFile) {
        BundleWiring bundleWiring = bundleContext.getBundle().adapt(BundleWiring.class);
        ClassLoader bundleClassLoader = bundleWiring.getClassLoader();

        Database database = null;
        try {

            database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(
                    new JdbcConnection(dataSource.getConnection()));
            Liquibase liquibase =
                    new Liquibase(changeLogFile, new ClassLoaderResourceAccessor(bundleClassLoader), database);

            List<ChangeSet> unrunChangeSets = liquibase.listUnrunChangeSets(null);

            if (unrunChangeSets.size() > 0) {
                dumpSQL(liquibase, bundleContext);

                if (update) {
                    liquibase.update(null);
                }
            } else {
                logService.log(LogService.LOG_INFO, "Nothing to change in the database for bundle "
                        + bundleContext.getBundle().toString());
            }
        } catch (LiquibaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (database != null) {
                try {
                    database.close();
                } catch (DatabaseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

    }

    private void dumpSQL(Liquibase liquibase, BundleContext bundleContext) throws LiquibaseException {
        if (sqlDumpFolder != null) {
            File folderFile = new File(sqlDumpFolder);
            folderFile.mkdirs();
            Bundle bundle = bundleContext.getBundle();
            String symbolicName = bundle.getSymbolicName();
            String fileName = symbolicName + "_" + new Date().getTime() + ".sql";
            File outputFile = new File(folderFile, fileName);

            try (FileWriter fw = new FileWriter(outputFile)) {
                liquibase.update(null, fw);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }
}
