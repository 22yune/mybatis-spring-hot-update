package com.galen.program.mybatis.hotup;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyException;
import net.contentobjects.jnotify.JNotifyListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Check every now and then that a certain file has not changed. If it has, then
 * call the {@link #doOnChange} method.
 */
public abstract class FileNotify extends Thread {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	 protected Resource[] resources;
	  
	volatile boolean interrupted = false;

	private ClassLoader classLoader;
	protected FileNotify(Resource[] resources, ClassLoader classLoader) {
		super("FileNotify");
		this.resources = resources;
		this.classLoader = classLoader;
		setDaemon(true);
	}

	abstract protected void doOnChange(String rootPath, String name);

	public void checkAndConfigure() {
		int mask = JNotify.FILE_MODIFIED;// | JNotify.FILE_CREATED | JNotify.FILE_DELETED | JNotify.FILE_RENAMED;
		//是否监控子目录
		boolean watchSubtree = true;
		
		Listener listener = new Listener();

        Set<String> paths = new HashSet<String>();
        for (Resource r : resources) {
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
                 //   e1.printStackTrace();
                }
            //    e.printStackTrace();
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

	public void run() {
		checkAndConfigure();

  		while (!interrupted) {
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class Listener implements JNotifyListener {
		public void fileRenamed(int wd, String rootPath, String oldName,
                                String newName) {
			print("renamed " + rootPath + " : " + oldName + " -> " + newName);
		}
		public void fileModified(int wd, String rootPath, String name) {
			print("modified " + rootPath + " : " + name);
			try{
				ClassLoader temp = Thread.currentThread().getContextClassLoader();
				Thread.currentThread().setContextClassLoader(classLoader);
				doOnChange(rootPath, name);
				Thread.currentThread().setContextClassLoader(temp);
			}catch (Exception e) {
				interrupted = true;
				e.printStackTrace();
			}
		}
		public void fileDeleted(int wd, String rootPath, String name) {
			print("deleted " + rootPath + " : " + name);
		}
		public void fileCreated(int wd, String rootPath, String name) {
			print("created " + rootPath + " : " + name);
		}
		private void print(String msg) {
			log.debug(msg);
		}
	}


}
