package com.galen.program;

import com.galen.program.mybatis.hotup.FileEvent;
import com.galen.program.mybatis.hotup.FileNotify;
import org.junit.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * <p><b>     </b></p><br>
 * <p>     <br>
 * <br><p>
 *
 * @author baogen.zhang          2021-01-19 9:29
 */
public class FileNotifyTest {


    @Test
    public void test(){
        List<Resource[]> resources = new ArrayList<>();
        String path = "file:D:/workspace/xIR_J2EE_xAMS4.0_branch/xquant-asset-management/*/**/src/main/resources/sqlMapper";
        String path1 = "file:D:/workspace/xIR_J2EE_xAMS4.0_branch/xquant-asset-management/*/**/src/main/resources/sqlMapper/**";
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            //     resources.add(resolver.getResources(path));
            resources.add(resolver.getResources(path1));
        } catch (IOException e) {
            e.printStackTrace();
        }
        final Calendar calendar = Calendar.getInstance();
        final SimpleDateFormat sdf =new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS" );
        new FileNotify( resources, this.getClass().getClassLoader()) {
            @Override
            protected FileEvent product(int wd, String rootPath, String name) {
                File file = new File(rootPath + File.separator + name);
                Date date = new Date();
                long a = date.getTime() - file.lastModified();
                System.out.println( name + " : " + file.lastModified() + " : " + date.getTime() + " : " + a);
                if( a < 1000 ){
                    int i = name.indexOf(".xml");
                    if(i > 0){
                        System.out.println(name.substring(0,i + 4));
                        return new FileEvent(rootPath, name.substring(0,i + 4));
                    }
                }
                return null;
            }

            @Override
            protected void consume(FileEvent event) {
                File file = new File(event.getRootPath() + File.separator + event.getName());
                Date date = new Date();
                calendar.setTimeInMillis(file.lastModified());
                System.out.println(file.getName() +  " : " + sdf.format(calendar.getTime()) + " : " + sdf.format(date) + " : " + (date.getTime() - file.lastModified()) );
            }
        }.start();
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
