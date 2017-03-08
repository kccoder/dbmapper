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
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import dbmapper.TypeConverter;

public class DefaultUpdateHelper implements UpdateHelper {
	private String sql;
	private List<Getter> setGetters = new ArrayList<>();
	private List<Getter> keyGetters = new ArrayList<>();
	
	public DefaultUpdateHelper(String sql, List<Getter> setGetters, List<Getter> keyGetters) {
		this.sql = sql;
		this.setGetters = setGetters;
		this.keyGetters = keyGetters;
	}
	
	public int update(Connection connection, TypeConverter typeConverter, Object object) throws Exception {
		try (AutoCloseables closeables = new AutoCloseables()) {
			PreparedStatement ps = closeables.add(connection.prepareStatement(sql));
			
			int setGettersSize = setGetters.size();
			for(int i = 0; i < setGettersSize; i++) {
				typeConverter.setValue(ps, i + 1, setGetters.get(i).getValue(object));
			}

			for(int i = 0; i < keyGetters.size(); i++) {
				typeConverter.setValue(ps, setGettersSize + i + 1, keyGetters.get(i).getValue(object));
			}
			
			return ps.executeUpdate();
		}
	}
}