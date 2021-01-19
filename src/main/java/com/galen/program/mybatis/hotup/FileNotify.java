package com.galen.program.mybatis.hotup;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyException;
import net.contentobjects.jnotify.JNotifyListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Check every now and then that a certain file has not changed. If it has, then
 * call the {@link #doOnChange} method.
 */
public abstract class FileNotify extends Thread {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	protected List<Resource[]> resources;

	volatile boolean interrupted = false;

	private ConcurrentHashMap<Event,Boolean> eventMap = new ConcurrentHashMap<Event,Boolean>();

	private ClassLoader classLoader;


	protected FileNotify(List<Resource[]> resources, ClassLoader classLoader) {
		super("FileNotify");
		this.resources = resources;
		this.classLoader = classLoader;
		setDaemon(true);
	}

	abstract protected void doOnChange(String rootPath, String name);

	public void checkAndConfigure() {
		int mask = JNotify.FILE_MODIFIED;// | JNotify.FILE_CREATED | JNotify.FILE_DELETED | JNotify.FILE_RENAMED;
		//是否监控子目录
		boolean watchSubtree = false;

		Listener listener = new Listener();

		Set<String> paths = new HashSet<String>();
		for (Resource[] rs : resources) {
			for (Resource r : rs){
				try {
					paths.add(r.getFile().getAbsolutePath());
				} catch (IOException e) {
					try {
						String path = r.getURL().getPath();
						if(path.indexOf("file:") == 0 && path.contains(".jar!")){
							path = path.substring(6 ,path.indexOf(".jar!"));
							path = path.substring(0,path.lastIndexOf("/"));
							paths.add(path);
						}
					}catch (IOException e1){
					}
				}
			}
		}
		for (String path : paths) {
			try {
				JNotify.addWatch(path, mask, watchSubtree, listener);
			} catch (JNotifyException e) {
				interrupted = true;
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run() {
		checkAndConfigure();

		while (!interrupted) {
			try{
				ClassLoader temp = Thread.currentThread().getContextClassLoader();
				Thread.currentThread().setContextClassLoader(classLoader);
				Iterator<Map.Entry<Event, Boolean>> iterator = eventMap.entrySet().iterator();
				while (iterator.hasNext()){
					Event e = iterator.next().getKey();
					doOnChange(e.rootPath, e.name);
					iterator.remove();
				}
				Thread.currentThread().setContextClassLoader(temp);
			}catch (Exception e) {
				interrupted = true;
				e.printStackTrace();
			}

			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private class Listener implements JNotifyListener {
		@Override
		public void fileRenamed(int wd, String rootPath, String oldName,
								String newName) {
			print("renamed " + rootPath + " : " + oldName + " -> " + newName);
		}

		@Override
		public void fileModified(int wd, String rootPath, String name) {
			print("modified " + rootPath + " : " + name);
			FileNotify.this.eventMap.put(new Event(wd, rootPath, name),true);
		}
		@Override
		public void fileDeleted(int wd, String rootPath, String name) {
			print("deleted " + rootPath + " : " + name);
		}
		@Override
		public void fileCreated(int wd, String rootPath, String name) {
			print("created " + rootPath + " : " + name);
		}
		private void print(String msg) {
			log.debug(msg);
		}
	}

	private static class Event{
		int wd; String rootPath; String name;

		public Event(int wd, String rootPath, String name) {
			this.wd = wd;
			this.rootPath = rootPath;
			this.name = name;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Event event = (Event) o;
			return wd == event.wd &&
					Objects.equals(rootPath, event.rootPath) &&
					Objects.equals(name, event.name);
		}

		@Override
		public int hashCode() {
			return Objects.hash(wd, rootPath, name);
		}
	}

}
