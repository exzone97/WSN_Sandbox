import com.virtenio.commander.io.*;
import com.virtenio.commander.toolsets.preon32.Preon32Helper;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Scanner;

import org.apache.tools.ant.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.InputMismatchException;

public class Handler {

	private Scanner scanner;
	private volatile static boolean exit = false;
	private BufferedWriter writer;
	private static boolean isSensing;

	private static DefaultLogger getConsoleLogger() {
		DefaultLogger consoleLogger = new DefaultLogger();
		consoleLogger.setErrorPrintStream(System.err);
		consoleLogger.setOutputPrintStream(System.out);
		consoleLogger.setMessageOutputLevel(Project.MSG_INFO);

		return consoleLogger;
	}

	private void time_synchronize() throws Exception {
		DefaultLogger consoleLogger = getConsoleLogger();
		File buildFile = new File("E:\\Sandbox\\build.xml");
		Project antProject = new Project();
		antProject.setUserProperty("ant.file", buildFile.getAbsolutePath());
		antProject.addBuildListener(consoleLogger);

		try {
			antProject.fireBuildStarted();
			antProject.init();
			ProjectHelper helper = ProjectHelper.getProjectHelper();
			antProject.addReference("ant.ProjectHelper", helper);
			helper.parse(antProject, buildFile);
			String target = "cmd.time.synchronize";
			antProject.executeTarget(target);
			antProject.fireBuildFinished(null);
		} catch (BuildException e) {
			e.printStackTrace();
		}
	}

	public void init() throws Exception {
		try {
			Preon32Helper nodeHelper = new Preon32Helper("COM4", 115200);
			DataConnection conn = nodeHelper.runModule("basestation");
			BufferedInputStream in = new BufferedInputStream(conn.getInputStream());

			int choiceentry = -1;
			String s;
			scanner = new Scanner(System.in);
			conn.flush();
			System.out.println("MENU");
			System.out.println("1. Check Online");
			System.out.println("2. Synchronize Time");
			System.out.println("3. Get Time");
			System.out.println("4. Start Sensing !");
			System.out.println("0. Exit");
			System.out.println("Choice: ");
			do {
				try {
					choiceentry = scanner.nextInt();
					conn.write(choiceentry);
					Thread.sleep(200);
					switch (choiceentry) {
					case 0: {
						System.out.println("Exit Program ...");
						exit = true;
						break;
					}
					case 1: {
						if (isSensing == false) {
							byte[] buffer = new byte[1024];
							while (in.available() > 0) {
								in.read(buffer);
								conn.flush();
								s = new String(buffer);
								String[] ss = s.split("#");
								for (String res : ss) {
									if (res.startsWith("Node")) {
										System.out.println(res);
									}
								}
								Thread.sleep(1000);
							}
							System.out.println("MENU");
							System.out.println("1. Check Online");
							System.out.println("2. Synchronize Time");
							System.out.println("3. Get Time");
							System.out.println("4. Start Sensing !");
							System.out.println("0. Exit");
							System.out.println("Choice: ");
						} else {
							System.out.println("MENU");
							System.out.println("0. Exit");
							System.out.println("Choice: ");
						}
						break;
					}
					case 2: {
						Thread.sleep(500);
						if (isSensing == false) {
							System.out.println("Done Synchronize");
							System.out.println("MENU");
							System.out.println("1. Check Online");
							System.out.println("2. Synchronize Time");
							System.out.println("3. Get Time");
							System.out.println("4. Start Sensing !");
							System.out.println("0. Exit");
							System.out.println("Choice: ");
						} else {
							System.out.println("MENU");
							System.out.println("0. Exit");
							System.out.println("Choice: ");
						}
						break;
					}
					case 3: {
						if (isSensing == false) {
							byte[] buffer = new byte[1024];
							while (in.available() > 0) {
								in.read(buffer);
								conn.flush();
								s = new String(buffer);
								String[] ss = s.split("#");
								for (String res : ss) {
									if (res.startsWith("Time")) {
										String[] fin = res.split(" ");
//										System.out.println(res);
										long time = Long.parseLong(fin[2]);
										System.out.println(fin[0] + " " + fin[1] + " " + stringFormat(time));
									}
								}

								Thread.sleep(1000);
							}
							System.out.println("MENU");
							System.out.println("1. Check Online");
							System.out.println("2. Synchronize Time");
							System.out.println("3. Get Time");
							System.out.println("4. Start Sensing !");
							System.out.println("0. Exit");
							System.out.println("Choice: ");
						} else {
							System.out.println("MENU");
							System.out.println("0. Exit");
							System.out.println("Choice: ");
						}
						break;
					}
					case 4: {
						if (isSensing == false) {
							System.out.println("Sensing...");
							String fName = System.currentTimeMillis() + "";
							fName = "MH4 OUT NR" + fName + ".txt";
							writeToFile(fName, "Tester", in);
							isSensing = true;
							System.out.println("MENU");
							System.out.println("0. Exit");
							System.out.println("Choice: ");
						} else {
							System.out.println("Already Sensing....");
							System.out.println("MENU");
							System.out.println("0. Exit");
							System.out.println("Choice: ");
						}
						break;
					}
					}
				} catch (InputMismatchException e) {
					String input = scanner.next();
					System.out.println("Input salah..");
					if (isSensing == false) {
						System.out.println("MENU");
						System.out.println("1. Check Online");
						System.out.println("2. Synchronize Time");
						System.out.println("3. Get Time");
						System.out.println("4. Start Sensing !");
						System.out.println("0. Exit");
						System.out.println("Choice: ");
					} else {
						System.out.println("MENU");
						System.out.println("0. Exit");
						System.out.println("Choice: ");
					}
					continue;
				}
			} while (choiceentry != 0);
		} catch (Exception e) {
		}
	}

	public String stringFormat(long val) {
		Date date = new Date(val);
		SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS");
		String dateText = df.format(date);
		return dateText;
	}

	public void writeToFile(String fName, String folName, BufferedInputStream in) throws Exception {
		Thread t = new Thread() {
			byte[] buffer = new byte[2048];
			String s;
			long count = 0;
			File newFolder = new File(folName);

			public void run() {
				if (!newFolder.exists())
					newFolder.mkdir();
				String path = folName + "/" + fName;
				try {
					FileWriter fw = new FileWriter(path);
					writer = new BufferedWriter(fw);
				} catch (Exception e) {
					e.printStackTrace();
				}
				while (!exit) {
					try {
						if (in.available() > 0) {
							in.read(buffer);
							s = new String(buffer);
							String[] subStr = s.split("#");
							for (String w : subStr) {
								if (w.startsWith("SENSE")) {
									String temp = w.replace('<', ' ');
									String temp2 = temp.replace('>', ' ');
									String temp3 = temp2.replace('?', ' ');
									String[] ss = temp3.split(" ");
									long val = Long.parseLong(ss[3]);
									String newString = ss[0] + " " + Integer.toHexString(Integer.parseInt(ss[1])) + " "
											+ ss[2] + " " + stringFormat(val) + " " + ss[4] + " " + ss[5] + " " + ss[6]
											+ " " + ss[7] + " " + ss[8] + " " + ss[9] + " " + ss[10] + " " + ss[11];
									writer.write(newString, 0, newString.length());
									writer.newLine();
//									Thread.sleep(200);
									count++;
									if (count == 10) {
										writer.close();
										FileWriter fw = new FileWriter(path, true);
										writer = new BufferedWriter(fw);
										count = 0;
									}
								}
							}
//							count++;
//							if (count == 10) {
//								writer.close();
//								FileWriter fw = new FileWriter(path, true);
//								writer = new BufferedWriter(fw);
//								count = 0;
//							}
						}
					} catch (Exception e) {
					}
					Arrays.fill(buffer, (byte) 0);
				}
				try {
					writer.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		t.start();
	}

	private void context_set(String target) throws Exception {
		DefaultLogger consoleLogger = getConsoleLogger();
		File buildFile = new File("E:\\Sandbox\\buildUser.xml");
		Project antProject = new Project();
		antProject.setUserProperty("ant.file", buildFile.getAbsolutePath());
		antProject.addBuildListener(consoleLogger);

		try {
			antProject.fireBuildStarted();
			antProject.init();
			ProjectHelper helper = ProjectHelper.getProjectHelper();
			antProject.addReference("ant.ProjectHelper", helper);
			helper.parse(antProject, buildFile);

			antProject.executeTarget(target);
			antProject.fireBuildFinished(null);
		} catch (BuildException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		Handler handler = new Handler();
		isSensing = false;
		handler.context_set("context.set.1");
		handler.time_synchronize();
		handler.init();
	}
}