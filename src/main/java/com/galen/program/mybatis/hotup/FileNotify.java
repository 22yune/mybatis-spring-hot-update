package com.galen.program.mybatis.hotup;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyException;
import net.contentobjects.jnotify.JNotifyListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p><b>   文件监听  </b></p><br>
 * <p> 两秒一次触发文件事件，两秒内的原始文件事件缓存去重    <br>
 * <br><p>
 *
 * @author baogen.zhang          2021-01-22 11:21
 */
public abstract class FileNotify extends Thread {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	protected List<Resource[]> resources;

	volatile boolean interrupted = false;

	private ConcurrentHashMap<FileEvent,Boolean> eventMap = new ConcurrentHashMap<FileEvent,Boolean>();

	private ClassLoader classLoader;


	protected FileNotify(List<Resource[]> resources, ClassLoader classLoader) {
		super("FileNotify");
		this.resources = resources;
		this.classLoader = classLoader;
		setDaemon(true);
	}

	/**
	 *  文件事件生产逻辑
	 * @param wd
	 * @param rootPath
	 * @param name
	 * @return
	 */
	abstract protected FileEvent product(int wd,String rootPath, String name);

	/**
	 * 文件事件消费逻辑
	 * @param event
	 */
	abstract protected void consume(FileEvent event);

	public void checkAndConfigure() {
		int mask = JNotify.FILE_MODIFIED;// | JNotify.FILE_CREATED | JNotify.FILE_DELETED | JNotify.FILE_RENAMED;
		//是否监控子目录
		boolean watchSubtree = false;

		Listener listener = new Listener();

		Set<String> paths = new HashSet<String>();
		for (Resource[] rs : resources) {
			for (Resource r : rs){
				try {
					File file = r.getFile();
					if(file.isDirectory()){
						paths.add(file.getAbsolutePath());
					}else {
						paths.add(file.getParent()) ;
					}
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
			if(!eventMap.isEmpty()){
				try{
					ClassLoader temp = Thread.currentThread().getContextClassLoader();
					Thread.currentThread().setContextClassLoader(classLoader);
					Iterator<Map.Entry<FileEvent, Boolean>> iterator = eventMap.entrySet().iterator();
					while (iterator.hasNext()){
						FileEvent e = iterator.next().getKey();
						consume(e);
						iterator.remove();
					}
					Thread.currentThread().setContextClassLoader(temp);
				}catch (Exception e) {
					interrupted = true;
					e.printStackTrace();
				}
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
			print("renamed " + rootPath + File.separator + oldName + " -> " + newName);
		}

		@Override
		public void fileModified(int wd, String rootPath, String name) {
			print("modified " + rootPath + File.separator + name );
			FileEvent event = product(wd, rootPath, name);
			if( event != null){
				FileNotify.this.eventMap.put(event,true);
			}
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


}
