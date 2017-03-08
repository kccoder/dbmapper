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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ClassHelper {
	private Class<?> clazz;
	private Map<String, Setter> setters = new HashMap<>();
	private Map<String, Getter> getters = new HashMap<>();
	
	public ClassHelper(Class<?> clazz) {
		this.clazz = clazz;
		
		for(Method method : clazz.getMethods()) {
			if (isSetter(method)) {
				setters.put(method.getName(), new Setter(method));
			} else if (isGetter(method)) {
				getters.put(method.getName(), new Getter(method));
			}
		}
	}
	
	public Object newInstance() throws Exception {
		return clazz.newInstance();
	}
	
	public Class<?> getClazz() {
		return clazz;
	}
	
	public Setter getSetter(String methodName) {
		return setters.get(methodName);
	}
	
	public Getter getGetter(String methodName) {
		return getters.get(methodName);
	}
	
	public Getter getGetterForProperty(String propertyName) {
		return getters.get("get" + StringUtils.capitalize(propertyName));
	}

	public Setter getSetterForProperty(String propertyName) {
		return setters.get("set" + StringUtils.capitalize(propertyName));
	}
	
	
	private boolean isSetter(Method method) {
		if (!method.getName().startsWith("set")) return false;
		return method.getParameterCount() == 1;
	}
	
	private boolean isGetter(Method method) {
		if (!method.getName().startsWith("get")) return false;
		return method.getParameterCount() == 0;
	}
}