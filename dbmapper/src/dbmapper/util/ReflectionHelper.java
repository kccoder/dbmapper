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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ReflectionHelper {
	private Map<Class<?>, ClassHelper> classes = Collections.synchronizedMap(new HashMap<>());
	
	
	public boolean doesSetterExist(Class<?> clazz, String methodName) {
		ClassHelper helper = getClassHelper(clazz);
		return helper.getSetter(methodName) != null;
	}
	
	public Class<?> getSetterTypeIfExists(Class<?> clazz, String methodName) throws Exception {
		Setter setter = getClassHelper(clazz).getSetter(methodName);
		return (setter == null)?null:setter.getMethod().getParameterTypes()[0];
	}
	
	public void invokeSetterIfExists(Object object, String methodName, Object value) throws Exception {
		Setter setter = getClassHelper(object.getClass()).getSetter(methodName);
		if (setter != null) {
			setter.setValue(object, value);
		}
	}

	public Object invokeGetterIfExists(Object object, String methodName) throws Exception {
		Getter getter = getClassHelper(object.getClass()).getGetter(methodName);
		if (getter != null) {
			return getter.getValue(object);
		}
		return null;
	}
	
	public boolean doesGetterExistForProperty(Class<?> clazz, String propertyName) {
		return doesGetterExist(clazz, "get" + StringUtils.capitalize(propertyName));
	}
	
	public boolean doesGetterExist(Class<?> clazz, String methodName) {
		ClassHelper helper = getClassHelper(clazz);
		return helper.getGetter(methodName) != null;
	}
	
	

	public ClassHelper getClassHelper(Class<?> clazz) {
		ClassHelper ch = classes.get(clazz);
		if (ch == null) {
			ch = new ClassHelper(clazz);
			classes.put(clazz, ch);
		}
		return ch;
	}
}