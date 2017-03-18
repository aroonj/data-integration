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

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.camel.spring.Main;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.jolokia.client.J4pClient;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pReadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.servnize.camel.db.util.ListenCtl.Action;

/**
 * This is command line interface used for start and stop Camel Application.
 * 
 * @version 1.0
 * @author <a hfre="mailto:aroon.janthong@gmail.com">Aroon Janthong</a>
 *
 *
 */
public class CamelApplicationCtl {

	public enum AdminCommand {
		RUN, SHUTDOWN
	}

	public static final String CONF_DIR = "conf.dir";
	public static final String EXT_DIR = "ext.dir";

	private Properties properties;

	private static final Logger log = LoggerFactory.getLogger(CamelApplicationCtl.class);

	private static final CamelApplicationCtl app = new CamelApplicationCtl();

	private Main main;

	private String configuration;

	private ListenCtl ctl;

	private AdminCommand adminCommand;

	protected CamelApplicationCtl() {

	}

	public static void main(String[] args) {

		try {
			log.debug("Register shutdown signal hook.");
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					log.info("Shutdown signal hooked.");
					app.shutdown();
					log.info("Gracefully completed shutdown");
				}
			});

			app.parse(args);

			if (AdminCommand.SHUTDOWN.equals(app.adminCommand)) {
				// TODO shutdown
				log.info("Shutdown application ...");
				app.adminShutdown();
				return;

			} else if (AdminCommand.RUN.equals(app.adminCommand)) {
				app.init();
			} else {
				log.info("No admin command apply, application not run.");
			}

		} catch (Exception e) {
			log.error("Start application fail", e);
			//app.shutdown();
			return;
		} finally {
			log.info("Shutdown Application ...");
			app.shutdown();
		}

	}

	/**
	 * TODO initial
	 */
	protected void init() throws Exception {

		// load configuration file
		Properties p = loadProperties(configuration);
		if (p == null || p.isEmpty()) {
			throw new Exception("Load configuration file " + configuration);
		}
		this.properties = p;

		// loockup configuration
		loockUpConfig(this.properties);

		// ตรวจสอบความถูกต้องของ configuration file
		int error = validateConfiguration(properties);
		if (error > 0) {
			throw new Exception("Configuration file not valid" + configuration);
		}

		// TODO start controller

		Action[] actions = SimpleCommands.getActions(properties.getProperty("secret.key"),
				properties.getProperty("command.stop"));
		this.ctl = new ListenCtl(properties, actions);

		int c = 0;
		while (this.ctl.getState() != 0) {
			if (c > 10) {
				break;
			}
			Thread.sleep(1000);
			c++;
		}

		if (this.ctl.getState() != 0) {
			this.ctl.stop();
			throw new Exception("Controller server can not start.");
		}

		// TODO load extension lib
		loadExtension(properties.getProperty("extension.dir"));

		// TODO initial camel
		main = new Main();
		String prefix = "file://";
		if (System.getProperty("os.name").toLowerCase().contains("window")) {
			prefix = "file:///";
		}

		String camelURL = prefix + properties.getProperty("camel.conf");

		log.info("Use camel configuration file {}", camelURL);
		main.setFileApplicationContextUri(camelURL);
		main.start();

		while (!ctl.isStop()) {
			int[] cmd = ctl.listen();
			log.info("command {} {}", cmd[0], cmd[1]);
			if (SimpleCommands.isStop(cmd)) {
				/*
				 * ctl.stop(); main.getApplicationContext().stop(); main.stop();
				 */
				shutdown();
			}
		}
	}

	protected void parse(String[] args) throws Exception {

		Options options = createOptions();
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);

		if (cmd.hasOption("h")) {
			printHelp(options);
			return;
		}

		if (cmd.hasOption("f")) {
			log.info("Use configuration {}", cmd.getOptionValue("f"));
			this.configuration = getPosibleConfigFile(cmd.getOptionValue("f"));
			if (this.configuration == null) {
				log.error("No configuration file");
				printHelp(options);
				return;
			}

		}

		if (cmd.hasOption("s")) {
			this.adminCommand = AdminCommand.SHUTDOWN;
			return;

		} else if (cmd.hasOption("r")) {
			this.adminCommand = AdminCommand.RUN;
			return;

		} else {
			printHelp(options);
			throw new Exception("Error no specific command run or shutdown");
		}

	}

	/**
	 * create command line options
	 * 
	 * @return
	 */
	protected Options createOptions() {
		Options options = new Options();

		options.addOption("h", "Print help");
		options.addOption("f", true, "configuration file");
		options.addOption("r", false, "Run application");
		options.addOption("s", false, "Shutdown application");

		return options;
	}

	protected void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("CamelApplicationCtl", options);

	}

	/**
	 * load configuration from file.
	 * 
	 * @param path
	 * @return
	 */
	protected Properties loadProperties(String path) {

		String myPath = getPosibleConfigFile(path);

		File file = new File(myPath);
		FileInputStream fin = null;
		Properties prop = null;
		if (!file.exists()) {

			log.error("Configuration file not found {}", myPath);
			return null;
		}

		if (!file.isFile()) {

			log.error("{} is not reguration file.", myPath);
			return null;
		}

		try {
			fin = new FileInputStream(file);
			prop = new Properties();
			prop.load(fin);
		} catch (IOException e) {
			log.error("Load configuration file fial", e);
		} finally {
			if (fin != null) {
				try {
					fin.close();
				} catch (IOException e) {
					log.error("Close input file stream error", e);
				}
			}
		}

		return prop;
	}

	/**
	 * # Camel Application Configuration # Author : aroon.janthong@gmail.com
	 * 
	 * ## Camel Spring Context Configuration <br />
	 * camel.conf=
	 * 
	 * <br />
	 * ## Controller Listener used for stop service ### hostname or IP
	 * address<br />
	 * socket.address=localhost
	 * 
	 * <br />
	 * ### Listen port socket.port=19999
	 * 
	 * <br />
	 * ### Secret Key is used for stop application
	 * secret.key=abcdefghijkamnopqrstuvwandxyz <br />
	 * command.stop=stop
	 * 
	 * 
	 * ## Extension Directory <br />
	 * extension.dir=
	 * 
	 * <br />
	 * 
	 * @param properties
	 * @return count of error or 0 if no error
	 */
	protected int validateConfiguration(Properties properties) {

		if (properties == null || properties.isEmpty()) {
			log.error("Configure properties is null or empty.");
			return 1;
		}

		String[] keys = { "camel.conf", "socket.address", "socket.port", "secret.key", "command.stop",
				"extension.dir" };

		int error = 0;

		for (String key : keys) {

			String val = properties.getProperty(key);

			if (val == null || val.isEmpty()) {

				if (!key.equals("extension.dir")) {
					log.error("Configuration hava no value of {} key", key);
					error++;
				}
			} else {
				/*
				 * if (key.equals("secret.key")) { log.info("Configure {} = {}",
				 * key, "***"); } else { log.info("Configure {} = {}", key,
				 * val); }
				 */

				switch (key) {
				case "camel.conf":
					if (!validateCamelConfig(val)) {
						log.error("Error {} = {}", key, val);
						error++;
					}
					break;
				case "csocket.address":
					break;
				case "socket.port":
					String pattern = "^([1-9][0-9][0-9][0-9][0-9]?)$";
					if (!val.matches(pattern)) {
						log.error("Error -- {} = {}", key, val);
						error++;
					}
					break;
				case "secret.key":
					if (val.length() < 6) {
						log.error("Error {} = {}", key, val);
						error++;
					}
					break;
				case "command.stop":

					break;
				case "extension.dir":

					break;
				}
			}
		}
		return error;
	}

	protected boolean validateCamelConfig(String value) {

		if (value == null || value.isEmpty()) {
			return false;
		}

		File file = new File(value);
		if (!file.exists()) {
			log.error("File not found {}", value);
			return false;
		}
		if (file.isDirectory()) {
			log.error("{} is directory", value);
			return false;
		}
		if (!file.canRead()) {
			log.error("Can not read file {}", value);
			return false;
		}

		return true;
	}

	/**
	 * หา path ที่เป็นไปได้ ระหว่าง full path กับ relative
	 * 
	 * @param path
	 * @return
	 */
	protected String getPosibleConfigFile(String path) {
		if (path == null || path.isEmpty()) {
			return null;
		}

		String os = System.getProperty("os.name");
		String separator = System.getProperty("file.separator");
		String curdir = System.getProperty("user.dir");
		os = os.toLowerCase();

		if (os.contains("linux") || os.contains("mac") || os.contains("bsd")) {
			if (path.startsWith(separator)) {
				return path;
			}

		} else {
			if (path.contains(":\\")) {
				return path;
			}
		}

		String cfg = curdir + separator + path;
		return cfg;
	}

	protected void loadExtension(String path) {
		if (path == null || path.isEmpty()) {
			log.info("No extension class load.");
			return;
		}
		File dir = new File(path);
		if (!dir.exists()) {
			log.error("No extension directory found {}", path);
			return;
		}
		if (!dir.isDirectory()) {
			log.error("{} xtension is nit directory", path);
			return;
		}
		String[] files = dir.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if (name.endsWith(".jar")) {

					return true;
				}
				return false;
			}
		});

		if (files.length == 0) {
			log.info("No jar file in {} directory", path);
			return;
		}

		List<URL> urls = new ArrayList<URL>();

		for (String file : files) {
			String uri = "file://" + path + File.separator + file;
			log.debug("Create URL from jar file {}", uri);
			try {
				URL url = new URL(uri);
				urls.add(url);
			} catch (MalformedURLException e) {
				log.error("Can not create new URI from {}", uri);
			}
		}

		if (urls.isEmpty()) {
			log.info("No URL created from jar file in {} directory", path);
			return;
		}

		URL[] urla = new URL[urls.size()];
		urla = urls.toArray(urla);
		ClassLoader currentThreadClassLoader = Thread.currentThread().getContextClassLoader();
		URLClassLoader urlClassLoader = new URLClassLoader(urla, currentThreadClassLoader);
		// Replace the thread classloader - assumes
		// you have permissions to do so
		Thread.currentThread().setContextClassLoader(urlClassLoader);

	}

	/**
	 * Send Command to listener port for shutdown.
	 * 
	 * @throws Exception
	 */
	protected void adminShutdown() throws Exception {

		Properties p = loadProperties(configuration);
		if (p == null || p.isEmpty()) {
			throw new Exception("Load configuration file " + configuration);
		}

		String host = p.getProperty("socket.address");
		int port = Integer.parseInt(p.getProperty("socket.port"));

		String message = SimpleCommands.buildMessage(p.getProperty("secret.key"), p.getProperty("command.stop"));
		SimpleCommands.sendCommand(host, port, message);
	}

	protected void loockUpConfig(Properties pro) {

		if (pro == null || pro.isEmpty()) {
			log.info("Properties is null.");
			return;
		}

		String confdir = System.getProperty(CONF_DIR);
		String extdir = System.getProperty(EXT_DIR);

		if (confdir == null && extdir == null) {
			return;
		}

		// log.debug("System properties {} = {}",CONF_DIR,confdir);

		Set<String> keys = pro.stringPropertyNames();
		String tmpk = null;

		for (String key : keys) {

			String v = pro.getProperty(key);
			if (v == null || v.isEmpty()) {
				log.debug("Configuration value is null, key={} ", key);
				continue;
			}

			if (confdir != null) {
				log.debug("System properties {} = {}", CONF_DIR, confdir);
				tmpk = "{" + CONF_DIR + "}";
				if (v.contains(tmpk)) {
					v = v.replace(tmpk, confdir);
					pro.setProperty(key, v);
					// log.debug("camel conf.dir={}, camel configure file ={}",
					// confdir, v);
					log.debug("Set properties value of {} = {}", key, v);
				}
			}

			if (extdir != null) {
				log.debug("System properties {} = {}", EXT_DIR, extdir);
				tmpk = "{" + EXT_DIR + "}";
				if (v.contains(tmpk)) {
					v = v.replace(tmpk, extdir);
					pro.setProperty(key, v);
					// log.debug("ext.dir={}, extension directory ={}", extdir,
					// v);
					log.debug("Set properties value of {} = {}", key, v);
				}
			}
		}
	}

	/**
	 * Graceful shutdown
	 */
	void shutdown() {
		if (ctl != null) {
			if (!ctl.isStop()) {
				ctl.stop();
				try {
					if (main != null) {
						main.getApplicationContext().stop();
						main.stop();
					}
				} catch (Exception e) {

				}
			}
		}
	}

}
