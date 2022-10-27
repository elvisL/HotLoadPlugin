package com.elvis.java.plugin;

import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

public class Notifier {

    public static NotificationGroup NOTIFICATION_GROUP = new NotificationGroup("Custom Notification Group",NotificationDisplayType.BALLOON, true);
    private static Project project;

    public static void setProject(Project project) {
        Notifier.project = project;
    }

    public static void notifyError(String content) {
        NOTIFICATION_GROUP.createNotification(content, NotificationType.ERROR).notify(project);
    }

    public static void notify(String content) {
        NOTIFICATION_GROUP.createNotification(content, NotificationType.INFORMATION).notify(project);
    }
}
