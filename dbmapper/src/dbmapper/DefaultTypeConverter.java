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

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import dbmapper.util.StringUtils;

public class DefaultTypeConverter implements TypeConverter {
	private Map<Class<?>, ExactTypeConverter<?>> map = new HashMap<>();

	public DefaultTypeConverter() {
		put(Boolean.class, boolean.class, (rs, cname)->{return rs.getBoolean(cname);}, (ps, i, value)->{ps.setBoolean(i, value);});
		put(Byte.class, byte.class, (rs, cname)->{return rs.getByte(cname);}, (ps, i, value)->{ps.setByte(i, value);});
		put(Short.class, short.class, (rs, cname)->{return rs.getShort(cname);}, (ps, i, value)->{ps.setShort(i, value);});
		put(Integer.class, int.class, (rs, cname)->{return rs.getInt(cname);}, (ps, i, value)->{ps.setInt(i, value);});
		put(Long.class, long.class, (rs, cname)->{return rs.getLong(cname);}, (ps, i, value)->{ps.setLong(i, value);});

		put(Float.class, float.class, (rs, cname)->{return rs.getFloat(cname);}, (ps, i, value)->{ps.setFloat(i, value);});
		put(Double.class, double.class, (rs, cname)->{return rs.getDouble(cname);}, (ps, i, value)->{ps.setDouble(i, value);});
		put(BigDecimal.class, (rs, cname)->{return rs.getBigDecimal(cname);}, (ps, i, value)->{ps.setBigDecimal(i, value);});
			
		put(String.class, (rs, cname)->{return rs.getString(cname);}, (ps, i, value)->{ps.setString(i, value);});

		put(byte[].class, (rs, cname)->{return rs.getBytes(cname);}, (ps, i, value)->{ps.setBytes(i, value);});

		
		put(LocalDateTime.class, (rs, cname)->{
				Timestamp t = rs.getTimestamp(cname);
				return (t == null)?null:t.toLocalDateTime();	
			}, 
			(ps, i, value)->{ps.setTimestamp(i, Timestamp.valueOf(value));}
		);

		put(LocalDate.class, (rs, cname)->{
				Date d = rs.getDate(cname);
				return (d == null)?null:d.toLocalDate();
			}, 
			(ps, i, value)->{ps.setDate(i, Date.valueOf(value));}
		);
		
		put(LocalTime.class, (rs, cname)->{
				Time t = rs.getTime(cname);
				return (t == null)?null:t.toLocalTime();	
			}, 
			(ps, i, value)->{ps.setTime(i, Time.valueOf(value));}
		);
		
		put(OffsetDateTime.class, (rs, cname) -> {
				Timestamp t = rs.getTimestamp(cname);
				if (t == null) return null;
				LocalDateTime ldt = t.toLocalDateTime();
				return OffsetDateTime.of(ldt, ZoneOffset.ofHours(-t.getTimezoneOffset()/60));
			}, 
			(ps, i, value) -> {
				Timestamp t = Timestamp.valueOf(value.toLocalDateTime());
				Calendar c = Calendar.getInstance(TimeZone.getTimeZone(value.toZonedDateTime().getZone()));
				ps.setTimestamp(i, t, c);
			}
		);
		
		put(ZonedDateTime.class, (rs, cname) -> {
				Timestamp t = rs.getTimestamp(cname);
				if (t == null) return null;
				LocalDateTime ldt = t.toLocalDateTime();
				return OffsetDateTime.of(ldt, ZoneOffset.ofHours(-t.getTimezoneOffset()/60)).toZonedDateTime();
			}, 
			(ps, i, value) -> {
				Timestamp t = Timestamp.valueOf(value.toLocalDateTime());
				Calendar c = Calendar.getInstance(TimeZone.getTimeZone(value.getZone()));
				ps.setTimestamp(i, t, c);
			}
		);
	}
	
	public <T> void put(Class<?> clazz, LambdaExactTypeConverter.ValueGetter<T> valueGetter, 
	LambdaExactTypeConverter.ValueSetter<T> valueSetter) {
		put(clazz, new LambdaExactTypeConverter<T>(valueGetter, valueSetter));
	}

	public <T> void put(Class<?> clazz1, Class<?> clazz2, LambdaExactTypeConverter.ValueGetter<T> valueGetter, 
	LambdaExactTypeConverter.ValueSetter<T> valueSetter) {
		put(clazz1, new LambdaExactTypeConverter<T>(valueGetter, valueSetter));
		put(clazz2, new LambdaExactTypeConverter<T>(valueGetter, valueSetter));
	}
	
	public <T> void put(Class<?> clazz, ExactTypeConverter<T> converter) {
		map.put(clazz, converter);
	}
	
	public Object getValue(ResultSet rs, String columnName, Class<?> targetType) throws Exception {
		Object value = rs.getObject(columnName);
		if (value == null) return null;
		if (targetType.isEnum()) {
			String svalue = rs.getString(columnName);
			if (StringUtils.isEmpty(svalue)) {
				return null;
			} else {
				return Enum.valueOf((Class<? extends Enum>)targetType, svalue);
			}
		} else {
			ExactTypeConverter<?> converter = map.get(targetType);
			if (converter == null) {
				throw new IllegalArgumentException("Don't know how to convert type '" + targetType.getName() + "'.");
			} else {
				return converter.getValue(rs, columnName);
			}
		}
	}
	
	public <T> void setValue(PreparedStatement ps, int index, T value) throws SQLException {
		if (value == null) {
			ps.setNull(index, Types.NULL);
			return;
		}
		
		Class<T> clazz = (Class<T>)value.getClass();
		if (clazz.isEnum()) {
			ps.setString(index, value.toString());
		} else {
			ExactTypeConverter<T> converter = (ExactTypeConverter<T>)map.get(clazz);
			if (converter == null) {
				throw new IllegalArgumentException("Don't know how to convert type '" + clazz.getName() + "'.");
			} else {
				converter.setValue(ps, index, value);
			}
		}
	}
}