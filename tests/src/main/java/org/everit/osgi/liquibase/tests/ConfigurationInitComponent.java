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

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.everit.osgi.liquibase.component.LiquibaseService;
import org.junit.Assert;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.jdbc.DataSourceFactory;

@Component(immediate = true)
@Service(value = ConfigurationInitComponent.class)
public class ConfigurationInitComponent {

    @Reference(bind = "bindConfigAdmin")
    private ConfigurationAdmin configAdmin;

    @Activate
    public void activate(final BundleContext bundleContext) {
        try {
            Dictionary<String, Object> xaDataSourceProps = new Hashtable<String, Object>();
            xaDataSourceProps.put(DataSourceFactory.JDBC_URL, "jdbc:h2:mem:test");
            String xaDataSourcePid = getOrCreateConfiguration("org.everit.osgi.jdbc.dsf.XADataSourceComponent",
                    xaDataSourceProps);

            Dictionary<String, Object> pooledDataSourceProps = new Hashtable<String, Object>();
            pooledDataSourceProps.put("xaDataSource.target", "(service.pid=" + xaDataSourcePid + ")");
            getOrCreateConfiguration("org.everit.osgi.jdbc.commons.dbcp.ManagedDataSourceComponent",
                    pooledDataSourceProps);

            String tmpDirProperty = "java.io.tmpdir";
            String tmpDir = System.getProperty(tmpDirProperty);
            if (tmpDir == null) {
                Assert.fail("User temp directory could not be retrieved");
            }
            Dictionary<String, Object> liquibaseComponentProps = new Hashtable<String, Object>();
            liquibaseComponentProps.put(LiquibaseService.PROP_SQL_DUMP_FOLDER, tmpDir);
            getOrCreateConfiguration("org.everit.osgi.liquibase.component.internal.LiquibaseComponent",
                    liquibaseComponentProps);

            Dictionary<String, Object> liquibaseComponentProps4 = new Hashtable<String, Object>();
            liquibaseComponentProps4
                    .put(LiquibaseService.PROP_SQL_DUMP_FOLDER, "null/?");
            Configuration configuration4 = configAdmin.createFactoryConfiguration(
                    "org.everit.osgi.liquibase.component.internal.LiquibaseComponent", null);
            configuration4.update(liquibaseComponentProps4);

            Dictionary<String, Object> liquibaseComponentProps2 = new Hashtable<String, Object>();
            liquibaseComponentProps2.put(LiquibaseService.PROP_UPDATE, false);
            Configuration configuration = configAdmin.createFactoryConfiguration(
                    "org.everit.osgi.liquibase.component.internal.LiquibaseComponent", null);
            configuration.update(liquibaseComponentProps2);

            Dictionary<String, Object> liquibaseComponentProps3 = new Hashtable<String, Object>();
            liquibaseComponentProps3.put(LiquibaseService.PROP_UPDATE, "notBoolean");
            Configuration configuration3 = configAdmin.createFactoryConfiguration(
                    "org.everit.osgi.liquibase.component.internal.LiquibaseComponent", null);
            configuration3.update(liquibaseComponentProps3);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void bindConfigAdmin(final ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    private String getOrCreateConfiguration(final String factoryPid, final Dictionary<String, Object> props)
            throws IOException,
            InvalidSyntaxException {
        Configuration[] configurations = configAdmin.listConfigurations("(service.factoryPid=" + factoryPid + ")");
        if ((configurations != null) && (configurations.length > 0)) {
            return configurations[0].getPid();
        }
        Configuration configuration = configAdmin.createFactoryConfiguration(factoryPid, null);
        configuration.update(props);
        return configuration.getPid();
    }
}
