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

import java.io.Serializable;
import java.util.Date;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @version 1.0
 * @author <a hfre="mailto:aroon.janthong@gmail.com">Aroon Janthong</a>
 *
 */
public class LockerBean implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5474432643812983466L;

	public static final String LOCKER_ID = "LOCKER_ID";
	private static final Logger log = LoggerFactory.getLogger(LockerBean.class);
	private String name = "Locker";
	private LockRecord lock;
	private long id = 0;

	public LockerBean() {

	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public synchronized void lock(Exchange msg) {

		if (lock == null) {
			this.id++;
			this.lock = new LockRecord(name, id, new Date());
			msg.getIn().setHeader(LOCKER_ID, lock);
		}

	}

	public synchronized void release(Exchange msg) {

		log.debug("Release request name {} id {} ", name, id);
		if (lock == null) {
			log.error("No lock object");
			return;
		}
		LockRecord handle = msg.getIn().getHeader(LOCKER_ID, LockRecord.class);

		synchronized (lock) {
			if (lock.equals(handle)) {
				LockRecord old = this.lock;
				this.lock = null;
				log.debug("Release lock name {}, id {}, start {}", name, old.getId(), old.getStart());
			} else {
				log.error("Can not release lock {}", lock);
			}
		}
	}
}
