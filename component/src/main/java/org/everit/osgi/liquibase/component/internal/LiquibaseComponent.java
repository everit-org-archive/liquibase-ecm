package org.everit.osgi.liquibase.component.internal;

import java.util.Map;

import javax.sql.DataSource;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.everit.osgi.liquibase.component.LiquibaseService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.log.LogService;

@Component(metatype = true, immediate = true)
@Properties({
        @Property(name = LiquibaseService.PROP_MODE, options = {
                @PropertyOption(name = LiquibaseService.MODE_VALIDATE, value = "Validate only"),
                @PropertyOption(name = LiquibaseService.MODE_UPDATE_IF_NECESSARY, value = "Update if necessary") },
                value = LiquibaseService.MODE_UPDATE_IF_NECESSARY),
        @Property(name = LiquibaseService.PROP_SQL_DUMP_FOLDER), @Property(name = "logService.target") })
@Service
public class LiquibaseComponent implements LiquibaseService {

    private boolean updateIfNecessary = true;

    private String sqlDumpFolder;

    @Reference
    private LogService logService;

    @Activate
    public void activate(Map<String, Object> componentProperties) {
        Object modeObject = componentProperties.get(LiquibaseService.PROP_MODE);
        if (modeObject != null) {
            if (LiquibaseService.MODE_VALIDATE.equals(modeObject)) {
                updateIfNecessary = false;
            } else if (!LiquibaseService.MODE_UPDATE_IF_NECESSARY.equals(modeObject)) {
                throw new RuntimeException("Invalid mode value: " + modeObject);
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

        Database database =
                DatabaseFactory.getInstance().findCorrectDatabaseImplementation(
                        new JdbcConnection(dataSource.getConnection()));

        Liquibase liquibase =
                new Liquibase(changeLogFile, new ClassLoaderResourceAccessor(bundleClassLoader), database);
        try {
            liquibase.validate();
        } catch (LiquibaseException e) {
            if (updateIfNecessary) {
                try {
                    logService.log(LogService.LOG_INFO, "Validating database for the changelog file '" + changeLogFile
                            + "' of the bundle " + bundleContext.getBundle().toString()
                            + " failed. Trying to update database schema.");
                    liquibase.update(null);
                } catch (LiquibaseException e1) {
                    if (sqlDumpFolder != null) {
                        dumpSQL(liquibase);
                    }
                    throw new RuntimeException("Updating database for bundle " + bundleContext.getBundle().toString()
                            + " on changeLogFile '" + changeLogFile + "' failed.", e);
                }
            } else {
                if (sqlDumpFolder != null) {
                    dumpSQL(liquibase);
                }
                throw new RuntimeException("Validating database for bundle " + bundleContext.getBundle().toString()
                        + " on changeLogFile '" + changeLogFile + "' failed.", e);
            }

        }
    }

    private void dumpSQL(Liquibase liquibase) {

    }
}
