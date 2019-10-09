package magithub.utils;

//import engine.chat.ChatManager;
//import engine.users.UserManager;

import Engine.MAGitHubManager;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import Engine.Commons.Constants;

public class ServletUtils {

    private static final String MAGITHUB_MANAGER_ATTRIBUTE_NAME = "magitHubManager";
    private static final String CHAT_MANAGER_ATTRIBUTE_NAME = "chatManager";

    /*
    Note how the synchronization is done only on the question and\or creation of the relevant managers and once they exists -
    the actual fetch of them is remained un-synchronized for performance POV
     */
    private static final Object userManagerLock = new Object();
    private static final Object chatManagerLock = new Object();

    public static MAGitHubManager getMAGitHubManager(ServletContext servletContext) {

        synchronized (userManagerLock) {
            if (servletContext.getAttribute(MAGITHUB_MANAGER_ATTRIBUTE_NAME) == null) {
                servletContext.setAttribute(MAGITHUB_MANAGER_ATTRIBUTE_NAME, new MAGitHubManager());
            }
        }
        return (MAGitHubManager) servletContext.getAttribute(MAGITHUB_MANAGER_ATTRIBUTE_NAME);
    }

//    public static ChatManager getChatManager(ServletContext servletContext) {
//        synchronized (chatManagerLock) {
//            if (servletContext.getAttribute(CHAT_MANAGER_ATTRIBUTE_NAME) == null) {
//                servletContext.setAttribute(CHAT_MANAGER_ATTRIBUTE_NAME, new ChatManager());
//            }
//        }
//        return (ChatManager) servletContext.getAttribute(CHAT_MANAGER_ATTRIBUTE_NAME);
//    }

    public static int getIntParameter(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException numberFormatException) {
            }
        }
        return Constants.INT_PARAMETER_ERROR;
    }
}
