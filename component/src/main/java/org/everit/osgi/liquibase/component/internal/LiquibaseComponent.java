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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.everit.osgi.liquibase.bundle.OSGiResourceAccessor;
import org.everit.osgi.liquibase.component.DatabaseMaintenanceException;
import org.everit.osgi.liquibase.component.LiquibaseService;
import org.osgi.framework.Bundle;
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
    public void activate(final Map<String, Object> componentProperties) {
        modified(componentProperties);
    }

    private void dumpSQL(final Liquibase liquibase, final Bundle bundle, final String changeLogFile)
            throws LiquibaseException {
        if (sqlDumpFolder != null) {
            File folderFile = new File(sqlDumpFolder);
            folderFile.mkdirs();
            String symbolicName = bundle.getSymbolicName();
            String fileName = symbolicName + "_" + new Date().getTime() + ".sql";
            File outputFile = new File(folderFile, fileName);

            try (FileWriter fw = new FileWriter(outputFile)) {
                liquibase.update(null, fw);
            } catch (IOException e) {
                logService.log(LogService.LOG_ERROR, "Cannot dump SQL to " + outputFile.getAbsolutePath()
                        + " during processing '" + changeLogFile
                        + "' from the bundle " + bundle.toString(), e);
            }
        }

    }

    @Modified
    public void modified(final Map<String, Object> componentProperties) {
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
    public void process(final DataSource dataSource, final Bundle bundle, final String changeLogFile) {
        Database database = null;
        try {

            database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(
                    new JdbcConnection(dataSource.getConnection()));
            Liquibase liquibase =
                    new Liquibase(changeLogFile, new OSGiResourceAccessor(bundle), database);

            List<ChangeSet> unrunChangeSets = liquibase.listUnrunChangeSets(null);

            if (unrunChangeSets.size() > 0) {
                dumpSQL(liquibase, bundle, changeLogFile);

                if (update) {
                    liquibase.update(null);
                }
            } else {
                logService.log(LogService.LOG_INFO, "Nothing to change in the database for bundle "
                        + bundle.toString());
            }
        } catch (LiquibaseException e) {
            throw new DatabaseMaintenanceException("Error during processing '" + changeLogFile + "' from the bundle "
                    + bundle.toString(), e);
        } catch (SQLException e) {
            throw new DatabaseMaintenanceException("Error during processing '" + changeLogFile + "' from the bundle "
                    + bundle.toString(), e);
        } finally {
            if (database != null) {
                try {
                    database.close();
                } catch (DatabaseException e) {
                    logService.log(LogService.LOG_ERROR, "Cannot close database during processing '" + changeLogFile
                            + "' from the bundle " + bundle.toString(), e);
                }
            }
        }

    }
}
