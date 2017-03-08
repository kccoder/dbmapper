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

import java.sql.ResultSet;
import java.util.List;

public interface DBMapper {
	
	// returns any auto-generated ID and expects it to be a long
	// future versions should return List<Map<String, Object>> and
	// let the user decide how to deal any returned values
	/** Insert object into its table (based on class name) and return any auto-generated Long field. */
	public Long insert(Object object);

	// currently determines which column(s) are used to identify this record
	// based on the table definition of primary key(s).
	// it might make sense to provide more flexible update functionality in a future version
	/** Update the record which corresponds to this object based on the primary key(s) of the table. */
	public void update(Object object);
	

	/** Build a single instance of the passed in class from the ResultSet. This method does not call rs.next(). */
	public <T> T buildSingle(Class<T> clazz, ResultSet rs);
	
	/** Build a list of the full contents of the passed in class from the ResultSet */	
	public <T> List<T> buildList(Class<T> clazz, ResultSet rs);
}