package org.everit.osgi.liquibase.component;

import javax.sql.DataSource;

import org.osgi.framework.BundleContext;

public interface LiquibaseService {

    String PROP_MODE = "mode";
    
    String MODE_VALIDATE = "update";
    
    String MODE_UPDATE_IF_NECESSARY = "updateIfNecessary";
    
    String PROP_SQL_DUMP_FOLDER = "sqlDumpFolder";
    
    void process(DataSource dataSource, BundleContext bundleContext, String changeLogFile);
}
