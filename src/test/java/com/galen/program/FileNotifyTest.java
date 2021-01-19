package com.galen.program;

import com.galen.program.mybatis.hotup.FileNotify;
import org.junit.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
        String path = "file:D:/workspace/management/*/**/src/main/resources/sqlMapper";
        String path1 = "file:D:/workspace/management/*/**/src/main/resources/sqlMapper/*";
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            resources.add(resolver.getResources(path));
            resources.add(resolver.getResources(path1));
        } catch (IOException e) {
            e.printStackTrace();
        }
        new FileNotify( resources, this.getClass().getClassLoader()) {
            @Override
            protected void doOnChange(String rootPath, String name) {
                System.out.println(rootPath + File.separator + name);
            }
        }.start();
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
