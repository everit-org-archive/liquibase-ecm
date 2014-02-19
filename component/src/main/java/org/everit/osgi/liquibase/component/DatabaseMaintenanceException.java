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
public class DatabaseMaintenanceException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public DatabaseMaintenanceException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
