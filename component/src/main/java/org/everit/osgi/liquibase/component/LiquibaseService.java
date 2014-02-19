/**
 * This file is part of Everit - Liquibase OSGi Component.
 *
 * Everit - Liquibase OSGi Component is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Everit - Liquibase OSGi Component is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Everit - Liquibase OSGi Component.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.everit.osgi.liquibase.component;
import javax.sql.DataSource;

import org.osgi.framework.Bundle;

/**
 * A very simple OSGi service that can be used to do Liquibase database migration.
 */
public interface LiquibaseService {

    /**
     * Configuration property of the component that indicates if the database itself should be updated or not.
     */
    String PROP_UPDATE = "update";

    /**
     * In case this configuration property is defined, the SQL update scripts will be dumped to the specified folder.
     */
    String PROP_SQL_DUMP_FOLDER = "sqlDumpFolder";

    /**
     * Processes a Liquibase changelog.
     * 
     * @param dataSource
     *            The dataSource that the changeLog will be processed on.
     * @param bundle
     *            The bundle that contains the changeLog file.
     * @param changeLogFile
     *            The location of the changeLog file withing the bundle.
     */
    void process(DataSource dataSource, Bundle bundle, String changeLogFile);
}
