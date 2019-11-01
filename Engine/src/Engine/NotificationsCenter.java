package Engine;

import Engine.GsonClasses.NotificationData;

import java.util.ArrayList;
import java.util.LinkedList;

public class NotificationsCenter {
    private ArrayList<NotificationData> notifications = new ArrayList<>();
    private int seen = 0;
    private int unseen = 0;

    public void addNotification(String content, String type) {
        String date = Manager.getCurrentDateString();
        notifications.add(0, new NotificationData(type, content, date));
        unseen ++;
    }

    public int getNumberOfNotifications() {
        return notifications.size();
    }

    public int getSeen() {
        return seen;
    }

    public int getUnseen() {
        return unseen;
    }

    public ArrayList<NotificationData> getNotifications() {
        return notifications;
    }

    public void seenAll() {
        seen += notifications.size();
        unseen = 0;
    }

    public void clear() {
        seen = 0;
        unseen = 0;
        notifications.clear();
    }
}
