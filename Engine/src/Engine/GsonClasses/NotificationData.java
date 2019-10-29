package Engine.GsonClasses;

public class NotificationData {
    private String type;        // pr, fork, alert
    private String content;
    private String date;

    public NotificationData(String type, String content, String date) {
        this.type = type;
        this.content = content;
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public String getDate() {
        return date;
    }
}


