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

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class AutoCloseables implements AutoCloseable {
	private List<AutoCloseable> items = new LinkedList<AutoCloseable>();
	
	public void close() {
		ListIterator<AutoCloseable> iterator = items.listIterator();
		while(iterator.hasNext()) {
			AutoCloseable ac = iterator.next();
			try {
				ac.close();
				iterator.remove();
			} catch(Exception e) {}
		}
	}
	
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}
	
	public <T extends AutoCloseable> T add(T t) {
		items.add(t);
		return t;
	}
	
	public void closeQueitly() {
		try {close();} catch(Exception e) {}
	}
}