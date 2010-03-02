/*
 * Copyright 2009 GBIF.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.gbif.provider.datasource.impl;

import org.gbif.provider.datasource.ImportRecord;
import org.gbif.provider.datasource.ImportSourceException;
import org.gbif.provider.model.DataResource;
import org.gbif.provider.model.ExtensionMapping;
import org.gbif.provider.model.PropertyMapping;
import org.gbif.provider.model.SourceSql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

import javax.sql.DataSource;

/**
 * Import source for relational databases that maps a sql resultset into
 * CoreRecords and allows to iterate over them.
 * 
 */
public class SqlImportSource extends ImportSourceBase {
  private static final Integer FETCH_SIZE = 1000;

  private Connection conn;
  private Statement stmt;
  private ResultSet rs;
  private String viewSql;

  public void close() {
    hasNext = false;
    try {
      rs.close();
      stmt.close();
      conn.close();
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public Long getResourceId() {
    return resourceId;
  }

  public boolean hasNext() {
    return hasNext;
  }

  @Override
  public void init(DataResource resource, ExtensionMapping view)
      throws ImportSourceException {
    super.init(resource, view);

    if (!(view.getSource() instanceof SourceSql)) {
      throw new IllegalArgumentException(
          "View needs to have a source of type SourceSql ");
    }
    SourceSql src = (SourceSql) view.getSource();
    // try to connect to db via JDBC
    try {
      DataSource ds = resource.getDatasource();
      if (ds != null) {
        this.conn = ds.getConnection();
      } else {
        throw new ImportSourceException("Can't connect to database");
      }
    } catch (SQLException e) {
      throw new ImportSourceException("Can't connect to database", e);
    }
    this.viewSql = src.getSql();
    try {
      this.stmt = this.conn.createStatement(ResultSet.TYPE_FORWARD_ONLY,
          ResultSet.CONCUR_READ_ONLY);
      this.stmt.setFetchSize(FETCH_SIZE);
      log.debug("SQLImportSource executing for resourceId[" + resource.getId()
          + "], connection[" + resource.getJdbcUrl() + "], the following SQL: "
          + this.viewSql);
      this.rs = this.stmt.executeQuery(this.viewSql);
      this.hasNext = this.rs.next();
    } catch (SQLException e) {
      annotationManager.annotateResource(resource,
          "Exception while creating RDBMS resultset import source"
              + e.toString());
      this.hasNext = false;
      throw new ImportSourceException("Cant init sql result set", e);
    }
  }

  public Iterator<ImportRecord> iterator() {
    return this;
  }

  public ImportRecord next() {
    ImportRecord row = null;
    if (hasNext) {
      try {
        row = new ImportRecord(resourceId,
            escapeRawValue(rs.getString(coreIdColumn)));
        // TODO: the mapping that takes place here should probably be done with
        // a separate mapping class
        if (guidColumn != null) {
          row.setGuid(escapeRawValue(rs.getString(guidColumn)));
        }
        if (linkColumn != null) {
          if (linkTemplate.contains(ExtensionMapping.TEMPLATE_ID_PLACEHOLDER)) {
            row.setLink(linkTemplate.replace(
                ExtensionMapping.TEMPLATE_ID_PLACEHOLDER,
                rs.getString(linkColumn)));
          } else {
            row.setLink(escapeRawValue(rs.getString(linkColumn)));
          }
        }
        for (PropertyMapping pm : properties) {
          if (pm.getColumn() != null && pm.getColumn() != null
              && !pm.getColumn().startsWith("#")) {
            String column = pm.getColumn();
            String val = escapeRawValue(rs.getString(column));
            // lookup value in term mapping map
            if (vocMap.containsKey(column)) {
              if (vocMap.get(column).containsKey(val)) {
                val = vocMap.get(column).get(val);
              }
            }
            row.setPropertyValue(pm.getProperty(), val);
          } else if (pm.getValue() != null) {
            row.setPropertyValue(pm.getProperty(), pm.getValue());
          }
        }
      } catch (SQLException e) {
        annotationManager.annotateResource(resource,
            "Exception while retrieving RDBMS source record" + e.toString());
        hasNext = false;
        row = null;
      }

      try {
        // forward rs cursor
        hasNext = rs.next();
      } catch (SQLException e2) {
        annotationManager.annotateResource(resource,
            "Exception while iterating RDBMS source" + e2.toString());
        hasNext = false;
      }
    }
    return row;
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }

}
