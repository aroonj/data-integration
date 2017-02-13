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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * listen socket และ process command
 * 
 * @version 1.0
 * @author <a hfre="mailto:aroon.janthong@gmail.com">Aroon Janthong</a>
 *
 */
public class ListenCtl {

	public static interface Action {
		int action(String message);
	}

	public class MultiThreadServer implements Runnable {

		private boolean stop = false;
		/** ถ้าปกติ state=0 อื่นๆไม่ปกติ */
		private int state = 1;
		private final int port;
		private final InetAddress address;
		private final ExecutorService exe;
		private final BlockingQueue<String> buffers;
		private ServerSocket ssocket;

		MultiThreadServer(InetAddress address, int port, ExecutorService exe, BlockingQueue<String> buffers) {
			this.port = port;
			this.address = address;
			this.exe = exe;
			this.buffers = buffers;

		}

		public void run() {
			try {
				start();
			} catch (Exception e) {
				state++;
				stop();
				log.error("Start listener thread fail.", e);
			}
		}

		void stop() {
			stop = true;
			state = 1;
			try {
				if (ssocket != null) {
					ssocket.close();
				}
			} catch (IOException e) {
				log.error("Close server socket fail.", e);
			}
		}

		void start() throws Exception {

			log.debug("Controller listen on {}, port {}", this.address.getHostAddress(), this.port);

			ssocket = new ServerSocket(port, 1000, this.address);

			log.info("Contoller listen on address {} port {}.", ssocket.getInetAddress().getHostAddress(), port);

			int error = 0;
			state = 0;

			while (!stop && !ssocket.isClosed()) {
				try {
					Socket csocket = ssocket.accept();

					csocket.setSoTimeout(60000);
					log.debug("Connected --");
					if (stop) {

						if (!csocket.isClosed()) {
							csocket.close();
						}
						break;
					}
					Processor precess = new Processor(csocket, buffers);
					exe.submit(precess);
					log.debug("Submit process --");
				} catch (SocketException e) {
					log.error("Socket connection error");
					error++;
					if (error > 100) {
						stop = true;
						state++;
					}
				} catch (Exception e) {
					log.error("Accept connection error {}", e);
					error++;
					if (error > 100) {
						stop = true;
						state++;
					}
				}
			}
			if (!ssocket.isClosed()) {
				ssocket.close();
			}

		}
	}

	/**
	 * ประมวลผนคำสั่ง จะประมวลผลเมื่อเจอ new line character
	 * 
	 * @author aroonjanthong
	 *
	 */
	static class Processor implements Runnable {

		private final Socket csocket;
		private final BlockingQueue<String> buffers;

		/**
		 * รับคำสั่งจาก csocket นำไปใส่ไว้ใน buffers
		 * 
		 * @param csocket
		 *            socket ที่รอรับคำสั่ง
		 * @param buffers
		 *            ที่สำหรับเก็บคำสั่ง
		 * 
		 */
		Processor(Socket csocket, BlockingQueue<String> buffers) {
			this.csocket = csocket;
			this.buffers = buffers;
		}

		@Override
		public void run() {

			if (csocket == null) {
				return;
			}
			if (csocket.isClosed()) {
				return;
			}
			if (buffers == null) {
				return;
			}

			InputStream in = null;
			InputStreamReader reader = null;
			BufferedReader lineReader = null;

			try {
				in = csocket.getInputStream();
				reader = new InputStreamReader(in);
				lineReader = new BufferedReader(reader);
				String line = lineReader.readLine();

				log.debug("Recept message {}", line);
				buffers.offer(line, 10, TimeUnit.SECONDS);
				log.debug("Put message {} to the buffer queue.", line);

			} catch (Exception e) {
				log.error("Processing socket error", e);
			} finally {
				try {
					lineReader.close();
					reader.close();
					in.close();
					csocket.close();
				} catch (Exception e) {
					log.error("Clossing socket error", e);
				}
			}

		}

	}

	/**
	 * listener port key
	 */
	public static final String SOCKET_PORT = "socket.port";

	/**
	 * IP address key
	 */
	public static final String SOCKET_ADDRESS = "socket.address";

	private static final Logger log = LoggerFactory.getLogger(ListenCtl.class);

	private final Action[] actions;

	private BlockingQueue<String> queues = new ArrayBlockingQueue<>(2);

	private ExecutorService exe = Executors.newFixedThreadPool(2);

	private MultiThreadServer server;

	public ListenCtl(Properties properties, Action... actions) {

		this.actions = actions;

		int port = ExpectedProperties.getInt(properties, SOCKET_PORT, 0);
		InetAddress address = getInetAddress(properties);

		server = new MultiThreadServer(address, port, exe, queues);

		try {
			exe.submit(server);

		} catch (Exception e) {
			log.error("Start server fail.");
		}

	}

	/**
	 * stop controller server
	 * 
	 */
	public void stop() {
		if (server != null) {
			server.stop();
			exe.shutdown();
			queues.clear();

		}
	}

	/**
	 * 
	 * @return
	 */
	public boolean isStop() {
		return server.stop;
	}

	public int getState() {
		return server.state;
	}

	/**
	 * รอรับ event
	 * 
	 * @return process event message or null if server is not running or error.
	 */
	public int[] listen() {
		if (server.stop) {
			log.error("Controller server thread is not running.");
			return null;
		}

		try {
			String message = null;
			while (!server.stop) {
				message = queues.poll(1, TimeUnit.SECONDS);
				if (message == null) {
					continue;
				}

				log.debug("Get message {}", message);
				int[] rs = new int[this.actions.length];
				for (int i = 0; i < rs.length; i++) {
					rs[i] = actions[i].action(message);
				}
				return rs;
			}
		} catch (InterruptedException e) {
			log.error("Process event error", e);
		}
		return null;
	}

	protected InetAddress getInetAddress(Properties p) {
		String t = ExpectedProperties.getString(p, SOCKET_ADDRESS, "localhost");
		try {
			return InetAddress.getByName(t);
		} catch (UnknownHostException e) {
			log.error("Error on get IP address from host name " + t, e);
		}
		log.info("User localhost instead.");

		try {
			return InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			log.error("Error on get localhost address ", e);
			return null;
		}
	}

}
