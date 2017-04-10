package dbmapper;

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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import dbmapper.util.AutoCloseables;
import dbmapper.util.ClassHelper;
import dbmapper.util.Column;
import dbmapper.util.DatabaseHelper;
import dbmapper.util.DefaultUpdateHelper;
import dbmapper.util.Getter;
import dbmapper.util.ReflectionHelper;
import dbmapper.util.StringUtils;
import dbmapper.util.TableHelper;
import dbmapper.util.UpdateHelper;

public class DefaultDBMapper implements DBMapper {
	private static ReflectionHelper reflectionHelper = new ReflectionHelper();
	private static DatabaseHelper databaseHelper = new DatabaseHelper();
	
	private TypeConverter typeConverter;
	private Connection connection;
	
	public DefaultDBMapper(Connection connection, TypeConverter typeConverter) {
		this.connection = connection;
		this.typeConverter = typeConverter;
	}
	
	public String getTableName(Class<?> clazz) {
		TableName dbname = clazz.getAnnotation(TableName.class);
		if (dbname == null) {
			return StringUtils.fromCamelCaseToUnderscores(clazz.getSimpleName());			
		} else {
			return dbname.value();
		}
	}
	
	// consider creating and caching a single insert statement like updates
	// for possible performance gains.  a downside is that it may attempt to
	// to insert a bunch of unnecessary columns, if it is trying to insert all
	// columns, including null values.  this might also affect how null/default
	// value columns behave.  need to investigate that more thoroughly.
	public Long insert(Object object) {
		return insert(getTableName(object.getClass()), object);
	}
	
	public Long insert(String tableName, Object object) {
		try (AutoCloseables closeables = new AutoCloseables()) {
			TableHelper tableHelper = databaseHelper.getTableHelper(connection, tableName);
			List<String> columns = new ArrayList<String>();
			List<Object> values = new ArrayList<Object>();
			for(Column column : tableHelper.getColumns()) {
				if (!column.isAutoIncrement()) {
					Object value = getValue(object, column.getPropertyName());
					if (value != null) {
						columns.add(column.getName());
						values.add(value);
					} // else { logic could be added to complain if the value is null, but the column is not nullable}
				}
			}
			
			if (columns.isEmpty()) {
				throw new DBMapperException("All values were null, so no insert could be performed");
			}

			StringBuilder b = new StringBuilder("INSERT INTO ").append(tableName);
			b.append(" (").append(StringUtils.join(columns, ", ")).append(")");
			b.append(" VALUES (").append(StringUtils.repeat("?", columns.size(), ", ")).append(")");

			String sql = b.toString();
			PreparedStatement ps = closeables.add(connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS));
			for(int i = 0; i < columns.size(); i++) {
				typeConverter.setValue(ps, i+1, values.get(i));
			}
			
			ps.execute();
			ResultSet rs = closeables.add(ps.getGeneratedKeys());
			if (rs.next()) {
				return rs.getLong(1);
			}
			return null;
		} catch(DBMapperException e) {
			throw e;
		} catch(Exception e) {
			throw new DBMapperException(e);
		}
	}
	
	public void update(Object object) {
		update(getTableName(object.getClass()), object);
	}

	public void update(String tableName, Object object) {
		try {
			Class<?> clazz = object.getClass();
			TableHelper tableHelper = databaseHelper.getTableHelper(connection, tableName);
			ClassHelper classHelper = reflectionHelper.getClassHelper(clazz);
			UpdateHelper updateHelper = tableHelper.getUpdateHelper();
			if (updateHelper == null) {
				updateHelper = createUpdateHelper(tableHelper, classHelper);
				tableHelper.setUpdateHelper(updateHelper);
			}
			updateHelper.update(connection, typeConverter, object);			
		} catch(Exception e) {
			throw new DBMapperException(e);
		}
	}
	
	protected UpdateHelper createUpdateHelper(TableHelper tableHelper, ClassHelper classHelper) throws Exception {			
		try (AutoCloseables closeables = new AutoCloseables()) {			
			List<String> setColumns = new ArrayList<>(), keyColumns = new ArrayList<>();
			List<Getter> setGetters = new ArrayList<>(), keyGetters = new ArrayList<>();
			
			for(Column column : tableHelper.getColumns()) {
				Getter getter = classHelper.getGetterForProperty(column.getPropertyName());
				if (column.isPrimaryKey()) {
					if (getter == null) {
						throw new DBMapperException("Couldn't locate getter for key column ['" + column.getName() + "']");
					}
					keyColumns.add(column.getName());
					keyGetters.add(getter);
				} else {
					if (getter != null) {
						setColumns.add(column.getName());
						setGetters.add(getter);
					}
				}
			}
			
			if (setColumns.isEmpty()) {
				throw new DBMapperException("There doesn't appear to be any columns to update");
			}

			if (keyColumns.isEmpty()) {
				throw new DBMapperException("There doesn't appear to be any primary key columns to identify records to update");
			}
			
			
			StringBuilder b = new StringBuilder("UPDATE ").append(tableHelper.getTableName());
			b.append(" SET ").append(StringUtils.join(setColumns, "=?, ")).append("=?");
			b.append(" WHERE ").append(StringUtils.join(keyColumns, "=? AND ")).append("=?");			
			String sql = b.toString();
			System.out.println(sql);
			return new DefaultUpdateHelper(sql, setGetters, keyGetters);			
		}
	}
	
	public <T> T buildSingle(Class<T> clazz, ResultSet rs) {
		try {
			T object = clazz.newInstance();
			
			ResultSetMetaData rsmd = rs.getMetaData();
			int cc = rsmd.getColumnCount();
			for(int i = 1; i <= cc; i++) {
				String columnName = rsmd.getColumnName(i);
				String setterName = toMethodName("set", columnName);
				Class<?> targetType = reflectionHelper.getSetterTypeIfExists(clazz, setterName);
				if (targetType != null) {
					Object value = typeConverter.getValue(rs, columnName, targetType);
					reflectionHelper.invokeSetterIfExists(object, setterName, value);
				}
			}
			return object;
		} catch(Exception e) {
			throw new DBMapperException(e);
		}
	}

	public <T> List<T> buildList(Class<T> clazz, ResultSet rs) {
		try {
			List<T> list = new ArrayList<>();
			while(rs.next()) {
				T t = buildSingle(clazz, rs);
				if (t != null) {
					list.add(t);
				}
			}
			return list;
		} catch(DBMapperException e) {
			throw e;
		} catch(Exception e) {
			throw new DBMapperException(e);
		}
	}
	
	private Object getValue(Object object, String propertyName) throws Exception {
		String getter = "get" + StringUtils.capitalize(propertyName);
		return reflectionHelper.invokeGetterIfExists(object, getter);
	}
			
	private static String toMethodName(String prefix, String columnName) {
		return prefix + StringUtils.capitalize(StringUtils.fromUnderscoresToCamelCase(columnName));
	}
}