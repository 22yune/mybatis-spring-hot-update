package com.galen.program.mybatis.hotup;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
   Check every now and then that a certain file has not changed. If it
   has, then call the {@link #doOnChange} method.

 */
public abstract class FileWatchdog<T> extends Thread {
	
	private Logger log = LoggerFactory.getLogger(this.getClass());

  /**
     The default delay between every file modification check, set to 60
     seconds.  */
  static final public long DEFAULT_DELAY = 60000; 
  /**
     The name of the file to observe  for changes.
   */
  protected List<T> resources;
  
  /**
     The delay to observe between every check. By default set {@link
     #DEFAULT_DELAY}. */
  protected long delay = DEFAULT_DELAY; 
  
  List<File> fileList;
  List<Long> lastModifList = new ArrayList<Long>(); 
  boolean warnedAlready = false;
  boolean interrupted = false;

  protected  FileWatchdog(List<T> resources) {
	    super("FileWatchdog");
	    this.resources = resources;
	    this.fileList = new ArrayList<File>();
	    for(T resource : resources){
		    File file = getFile(resource);
		    this.fileList.add(file);
		    this.lastModifList.add(new Long(0));
	    }
	    setDaemon(true);
	    checkAndConfigure();
	}

  /**
     Set the delay to observe between each check of the file changes.
   */
  public  void setDelay(long delay) {
    this.delay = delay;
  }

  abstract protected File getFile(T resource);
  abstract protected String getName(T resource);
  abstract  protected  void doOnChange(T resource);

  protected void checkAndConfigure() {
	  for(int i = 0; i < fileList.size(); i++){
		  File file  = fileList.get(i);
		  if(file == null) continue;
		  boolean fileExists;
		    try {
		      fileExists = file.exists();
		    } catch(SecurityException  e) {
		    	log.warn("Was not allowed to read check file existance, file:["+ getName(resources.get(i)) +"].");
		      interrupted = true; // there is no point in continuing
		      return;
		    }

			if (fileExists) {
				long l = file.lastModified(); // this can also throw a SecurityException
				Long lastModif = lastModifList.get(i);
				if (lastModif.equals(new Long(0)) ){
					lastModifList.set(i,l); 
				}
				if (!lastModif.equals(new Long(0)) && lastModif.compareTo(l) < 0 ) { // however, if we reached this point this
					lastModifList.set(i,l); // is very unlikely.
					log.debug("[" + getName(resources.get(i)) + "] haves modified.");
					doOnChange(resources.get(i));
				}
			} else {
				log.debug("[" + getName(resources.get(i)) + "] does not exist.");
			}
	  }
    
  }

  public
  void run() {    
    while(!interrupted) {
      try {
	    Thread.sleep(delay);
      } catch(InterruptedException e) {
	// no interruption expected
      }
      checkAndConfigure();
    }
  }
}
