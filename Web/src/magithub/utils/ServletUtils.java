package magithub.utils;

import Engine.MAGitHubManager;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import java.io.File;
import java.nio.file.Path;

import static constants.Constants.INT_PARAMETER_ERROR;

public class ServletUtils {

    private static final String USER_MANAGER_ATTRIBUTE_NAME = "userManager";

    /*
    Note how the synchronization is done only on the question and\or creation of the relevant managers and once they exists -
    the actual fetch of them is remained un-synchronized for performance POV
     */
    private static final Object userManagerLock = new Object();

    public static MAGitHubManager getMagitHubManager(ServletContext servletContext) {

        synchronized (userManagerLock) {
            if (servletContext.getAttribute(USER_MANAGER_ATTRIBUTE_NAME) == null) {
                servletContext.setAttribute(USER_MANAGER_ATTRIBUTE_NAME, new MAGitHubManager());
            }
        }
        return (MAGitHubManager) servletContext.getAttribute(USER_MANAGER_ATTRIBUTE_NAME);
    }

//    public static ChatManager getChatManager(ServletContext servletContext) {
//        synchronized (chatManagerLock) {
//            if (servletContext.getAttribute(CHAT_MANAGER_ATTRIBUTE_NAME) == null) {
//                servletContext.setAttribute(CHAT_MANAGER_ATTRIBUTE_NAME, new ChatManager());
//            }
//        }
//        return (ChatManager) servletContext.getAttribute(CHAT_MANAGER_ATTRIBUTE_NAME);
//    }

    public static String getUsernameFromRepositoryPath(Path path) {
        File remotePath = new File(path.toString());
        File remoteUserPath = remotePath.getParentFile();
        return remoteUserPath.getName();
    }

    public static int getIntParameter(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException numberFormatException) {
            }
        }
        return INT_PARAMETER_ERROR;
    }

    public static String getJsonResponseString(String message, boolean success) {
        return "{" +
                  "\"message\":\"" + message + "\"," +
                  "\"success\":" + success +
               "}";
    }
}
