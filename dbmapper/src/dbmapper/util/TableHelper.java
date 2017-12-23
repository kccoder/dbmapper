package dbmapper.util;

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import dbmapper.DBMapperException;

public class TableHelper {
	private String tableName;
	private Map<String, Column> columns = new HashMap<String, Column>();
	private UpdateHelper updateHelper;
	
	public TableHelper(Connection connection, String tableName) {
		this.tableName = tableName;
		try (AutoCloseables closeables = new AutoCloseables()) {
			DatabaseMetaData dbmd = connection.getMetaData();
			ResultSet columnsrs = closeables.add(dbmd.getColumns(null, null, tableName, null));
			while(columnsrs.next()) {
				Column column = new Column();
					column.setName(columnsrs.getString("COLUMN_NAME"));
					column.setPropertyName(StringUtils.uncapitalize(StringUtils.fromUnderscoresToCamelCase(column.getName())));
//					column.setGeneratedColumn(columnsrs.getBoolean("IS_GENERATEDCOLUMN"));
					column.setAutoIncrement("YES".equalsIgnoreCase(columnsrs.getString("IS_AUTOINCREMENT")));
				columns.put(column.getName(), column);
			}

			// load primary key info
			ResultSet pkrs = closeables.add(dbmd.getPrimaryKeys(null, null, tableName));
			while(pkrs.next()) {
				String columnName = pkrs.getString("COLUMN_NAME");
				Column column = columns.get(columnName);
				if (column == null) {
					// not sure how this would ever happen
					throw new DBMapperException(
						"primary key column ['" + columnName + "'] isn't a column for table ['" + tableName + "']");
				} else {
					column.setPrimaryKey(true);
				}
			}
		} catch(Exception e) {
			throw new DBMapperException(e);
		}
	}
	
	public String getTableName() {
		return tableName;
	}	
	
	public Collection<Column> getColumns() {
		return columns.values();
	}

	public UpdateHelper getUpdateHelper() {
		return updateHelper;
	}

	public void setUpdateHelper(UpdateHelper updateHelper) {
		this.updateHelper = updateHelper;
	}
}