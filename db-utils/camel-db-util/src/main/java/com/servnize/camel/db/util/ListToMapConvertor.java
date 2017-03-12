package com.servnize.camel.db.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convert list of object to Map
 * 
 * @author aroonjanthong
 *
 */
public class ListToMapConvertor {

	private static final Logger log = LoggerFactory.getLogger(ListToMapConvertor.class);

	private String[] keys;

	/**
	 * Set the list of map key names, the keys are separated by comma ','.
	 * 
	 * @param names
	 */
	public void setNames(String names) {
		log.debug("names = {}", names);
		if (names == null || names.isEmpty()) {
			log.error("No key or name specify");
			return;
		}
		String[] tmps = names.split(",");
		this.keys = new String[tmps.length];
		for (int i = 0; i < keys.length; i++) {
			this.keys[i] = tmps[i].trim();
		}

	}

	/**
	 * replace the {@link List} of IN of the body {@link Exchange#getIn()} with {@link Map}.
	 * 
	 * @param msg
	 */
	public void convert(Exchange msg) {
		Object o = msg.getIn().getBody();
		if (o == null) {
			log.error("Message body is null.");
			return;
		}

		Map<String, Object> map = new HashMap<>();
		
		if (o instanceof List<?>) {
			List<?> list = (List<?>) o;
			for (int i = 0; i < this.keys.length; i++) {
				if (list.size() > i) {
					if (this.keys[i] == null) {
						continue;
					}
					map.put(this.keys[i], list.get(i));
				} else {
					log.error("Key name greater than list size = {}, key index {}", list.size(), i);
				}
			}
			msg.getIn().setBody(map);	
		}
		
		
	}

}
