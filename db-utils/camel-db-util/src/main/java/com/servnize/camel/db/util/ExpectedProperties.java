/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.servnize.camel.db.util;

import java.util.Properties;

/**
 * @version 1.0
 * @author <a hfre="mailto:aroon.janthong@gmail.com">Aroon Janthong</a>
 *
 */
public class ExpectedProperties {

	public static final int getInt(Properties properties, String key, int defauleValue) {

		String t = properties.getProperty(key);
		if (t == null || t.isEmpty()) {
			return defauleValue;
		}
		try {
			int v = Integer.parseInt(t);
			return v;
		} catch (Exception e) {
			return defauleValue;
		}

	}

	public static final long getLong(Properties properties, String key, long defauleValue) {

		String t = properties.getProperty(key);
		if (t == null || t.isEmpty()) {
			return defauleValue;
		}
		try {
			Long v = Long.parseLong(t);
			return v;
		} catch (Exception e) {
			return defauleValue;
		}

	}

	public static final double getLong(Properties properties, String key, double defauleValue) {

		String t = properties.getProperty(key);
		if (t == null || t.isEmpty()) {
			return defauleValue;
		}
		try {
			Double v = Double.parseDouble(t);
			return v;
		} catch (Exception e) {
			return defauleValue;
		}

	}

	public static final String getString(Properties properties, String key, String defauleValue) {

		String t = properties.getProperty(key);
		if (t == null || t.isEmpty()) {
			return defauleValue;
		}

		return t;

	}
}
