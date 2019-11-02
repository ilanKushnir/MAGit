package magithub.servlets;

import Engine.Commons.Constants;
import Engine.Manager;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;

@WebListener
public class ShutDownContextListener implements ServletContextListener {    //TODO check shutdown functions     -context listener
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        System.out.println("System Starting up!");
        File magitRootFolder = new File(Constants.MAGITHUB_FOLDER_PATH);
        magitRootFolder.mkdir();
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        System.out.println("Shutting down!\n Deleting all content");
        File magitRootFolder = new File(Constants.MAGITHUB_FOLDER_PATH);
        deleteFolder(magitRootFolder);
    }
}

    private void deleteFolder(File folder) {
        File[] files = folder.listFiles();

        if(files != null) {
            for(File file: files) {
                if(file.isDirectory()) {
                    deleteFolder(file);
                } else {
                    file.delete();
                }
            }
        }

        folder.delete();
    }

}
