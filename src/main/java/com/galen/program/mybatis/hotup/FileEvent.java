package com.galen.program.mybatis.hotup;

import java.util.Objects;

/**
 * <p><b>     </b></p><br>
 * <p>     <br>
 * <br><p>
 *
 * @author baogen.zhang          2021-01-20 18:05
 */
public class FileEvent {
    private String rootPath; 
    private String name;

    public FileEvent(String rootPath, String name) {
        this.rootPath = rootPath;
        this.name = name;
    }

    public String getRootPath() {
        return rootPath;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileEvent fileEvent = (FileEvent) o;
        return Objects.equals(rootPath, fileEvent.rootPath) &&
                Objects.equals(name, fileEvent.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rootPath, name);
    }
}
