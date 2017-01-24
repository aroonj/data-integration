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

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.Objects;

import com.servnize.camel.db.util.ListenCtl.Action;

/**
 * 
 * @version 1.0
 * @author <a hfre="mailto:aroon.janthong@gmail.com">Aroon Janthong</a>
 * 
 *
 */
public class SimpleCommands {

	class SecurityAction implements Action {

		private final String secret;

		SecurityAction(String secret) {
			this.secret = secret;
		}

		@Override
		public int action(String message) {

			String msecret = getSecret(message);
			if (msecret == null || this.secret == null) {
				return 1;
			}

			if (Objects.equals(msecret, this.secret)) {
				return 0;
			}
			return 2;
		}

	}

	class CommandAction implements Action {
		private final String command;

		CommandAction(String command) {
			this.command = command;
		}

		@Override
		public int action(String message) {
			String mcommand = getCommand(message);
			if (mcommand == null || this.command == null) {
				return 1;
			}

			if (Objects.equals(mcommand, this.command)) {
				return 0;
			}
			return 2;
		}

	}

	public static final String STOP_COMMAND = "stop";

	private SimpleCommands() {

	}

	public static final Action[] getActions(String secret, String command) {

		SimpleCommands controller = new SimpleCommands();
		CommandAction cmdAction = controller.new CommandAction(command);
		SecurityAction secreatAction = controller.new SecurityAction(secret);
		Action[] actions = new Action[2];
		actions[0] = secreatAction;
		actions[1] = cmdAction;
		return actions;
	}

	public static final Action[] getStopActions(String secret) {
		return getActions(secret, STOP_COMMAND);
	}

	public static final boolean isStop(int[] cmd) {

		if (cmd == null) {
			return false;
		}
		if (cmd.length != 2) {
			return false;
		}
		int[] expected = { 0, 0 };
		return Arrays.equals(expected, cmd);

	}

	public static final String buildMessage(String secret, String message) {

		return secret + " " + message;
	}

	public static final String buildStopMessage(String secret) {

		return secret + " " + STOP_COMMAND;
	}

	/**
	 * send message command to server
	 * 
	 * @param host
	 * @param port
	 * @param message
	 * @return
	 * @throws Exception
	 */
	public static final int sendCommand(String host, int port, String message) throws Exception {

		Socket csocket = null;
		try {
			csocket = new Socket(host, port);

			OutputStream out = csocket.getOutputStream();
			OutputStreamWriter writer = new OutputStreamWriter(out);
			writer.write(message + "\n");
			writer.flush();
			writer.close();
			out.close();

		} finally {
			csocket.close();
		}
		return 0;
	}

	static String getSecret(String message) {
		String[] ts = message.split(" ");
		if (ts.length != 2) {
			return null;
		}
		return ts[0];
	}

	static String getCommand(String message) {
		String[] ts = message.split(" ");
		if (ts.length != 2) {
			return null;
		}
		return ts[1];
	}
}
