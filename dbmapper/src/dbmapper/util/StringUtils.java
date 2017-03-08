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

public class StringUtils {
	public static String toLowerCase(String s) {
		return (s == null)?null:s.toLowerCase();
	}
	
	public static String trimToEmpty(String s) {
		return (s == null)?"":s.trim();
	}
	
	public static String capitalize(String s) {
		s = trimToEmpty(s);
		return s.isEmpty()?s:(s.substring(0, 1).toUpperCase() + s.substring(1));
	}
	
	public static boolean isEmpty(String s) {
		return (s == null) || (s.length() == 0);
	}
	
	public static String join(Iterable<String> items, String separator) {
		StringBuilder b = new StringBuilder();
		for(String item : items) {
			b.append(item).append(separator);
		}
		if (b.length() > 0) {
			b.setLength(b.length() - separator.length());
		}
		return b.toString();
	}
	
	public static String repeat(String s, int times, String separator) {
		StringBuilder b = new StringBuilder();
		times--;
		for(int i = 0; i <= times; i++) {
			b.append(s);
			if ((i < times) && (separator != null)) {
				b.append(separator);
			}
		}
		return b.toString();
	}
	
	public static String fromUnderscoresToCamelCase(String columnName) {
		StringBuilder b = new StringBuilder();
		for(String s : columnName.split("_")) {
			b.append(StringUtils.capitalize(StringUtils.toLowerCase(s)));
		}
		return b.toString();
	}
	
	public static String fromCamelCaseToUnderscores(String s) {
		StringBuilder b = new StringBuilder();
		for(int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (i == 0) {
				c = Character.toLowerCase(c);
			}

			if (Character.isUpperCase(c)) {
				b.append('_');
				c = Character.toLowerCase(c);
			}
			
			b.append(c);
		}
		return b.toString();
	}	
}