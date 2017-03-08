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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LambdaExactTypeConverter<T> implements ExactTypeConverter<T> {
	private ValueGetter<T> valueGetter;
	private ValueSetter<T> valueSetter;
	
	public LambdaExactTypeConverter(ValueGetter<T> valueGetter, ValueSetter<T> valueSetter) {
		this.valueGetter = valueGetter;
		this.valueSetter = valueSetter;
	}
	
	public T getValue(ResultSet rs, String columnName) throws Exception {
		return valueGetter.getValue(rs, columnName);
	}
	
	public void setNonNullValue(PreparedStatement ps, int index, T value) throws SQLException {
		valueSetter.setValue(ps, index, value);
	}
	
	public interface ValueGetter<T> {
		public T getValue(ResultSet rs, String columnName) throws SQLException;
	}
	
	public interface ValueSetter<T> {
		public void setValue(PreparedStatement ps, int index, T value) throws SQLException;
	}	
}