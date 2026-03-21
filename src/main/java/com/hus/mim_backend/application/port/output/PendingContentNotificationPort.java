package com.hus.mim_backend.application.port.output;

/**
 * Output port for notifying admins when new content is submitted for moderation.
 */
public interface PendingContentNotificationPort {
    /**
     * Notify all admins that new content is pending moderation.
     *
     * @param contentType  e.g. "POST", "PAPER"
     * @param contentId    stable content identifier
     * @param contentTitle title of the submitted content
     * @param authorEmail  email of the author who submitted
     */
    void notifyNewPendingContent(String contentType, String contentId, String contentTitle, String authorEmail);
}
