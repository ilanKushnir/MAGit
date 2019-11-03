package magithub.servlets;

import Engine.Commons.Constants;
import Engine.Manager;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;
import java.io.FileFilter;

public class ShutDownContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        System.out.println("System Starting up!");
        File magitRootFolder = new File(Constants.MAGITHUB_FOLDER_PATH);
        magitRootFolder.mkdir();
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        System.out.println("Shutting down!\nDeleting all content");
        File magitRootFolder = new File(Constants.MAGITHUB_FOLDER_PATH);
        deleteFolder(magitRootFolder);
    }

    private void deleteFolder(File folder) {
        File [] files = folder.listFiles();

        if(files != null) {
            for(File file: files) {
                if(file.isDirectory() || file.isHidden()) {
                    deleteFolder(file);
                } else {
                    file.delete();
                }
            }
        }

        folder.delete();
    }

}
