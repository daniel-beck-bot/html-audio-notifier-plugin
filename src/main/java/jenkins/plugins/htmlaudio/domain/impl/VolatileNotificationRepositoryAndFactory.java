package jenkins.plugins.htmlaudio.domain.impl;

import static java.lang.Math.abs;
import static jenkins.plugins.htmlaudio.domain.NotificationId.asNotificationId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import jenkins.plugins.htmlaudio.domain.Notification;
import jenkins.plugins.htmlaudio.domain.NotificationFactory;
import jenkins.plugins.htmlaudio.domain.NotificationId;
import jenkins.plugins.htmlaudio.domain.NotificationRepository;


/**
 * A weird little in-memory based implementation of the repository & factory. Implemented this way to ensure
 * that notifications always are created/inserted in the correct order based on the autogenerated id. 
 * 
 * @author Lars Hvile
 */
public final class VolatileNotificationRepositoryAndFactory implements NotificationRepository,
        NotificationFactory {
    
    private final List<NotificationId> index = new ArrayList<NotificationId>();
    private final List<Notification> notifications = new ArrayList<Notification>();
    
    private long idSequence = 1;
    
    
    /*
     * To ensure that clients don't loose any notifications, it's important that we generate the id's &
     * persist with proper synchronization. 
     */
    public Notification createAndPersist(String soundUrl, String buildDetails) {
        
        if (soundUrl == null) {
            throw new IllegalArgumentException("soundUrl is required");
        }
        
        synchronized (this) {
            final Notification n = new SimpleNotification(
                    asNotificationId(idSequence++),
                    soundUrl,
                    buildDetails);
            
            index.add(n.getId());
            notifications.add(n);
            
            return n;
        }
    }
    
    
    public NotificationId getLastNotificationId() {
        synchronized (this) {
            return index.isEmpty()
                ? null
                : index.get(index.size() - 1);
        }
    }
    
    
    public Collection<Notification> findNewerThan(NotificationId id) {
        synchronized (this) {
            return safeCopy(notifications.subList(
                from(id),
                to()));
        }
    }
    
    
    private static <E> List<E> safeCopy(List<E> source) {
        return new ArrayList<E>(source);
    }
    
    
    private int from(NotificationId id) {
        return id == null
            ? 0
            : abs(binarySearchIndex(id) + 1);
    }
    
    
    private int binarySearchIndex(NotificationId id) {
        return Collections.binarySearch(index, id);
    }
    
    
    private int to() {
        return index.size();
    }
    
    
    public void remove(NotificationRemover remover) {
        synchronized(this) {
            remover.remove(new RemovalIterator());
        }
    }
    
    
    private class RemovalIterator implements Iterator<Notification> {
        
        int pos = 0;
        Notification curr;

        public boolean hasNext() {
            return pos < notifications.size();
        }

        public Notification next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            curr = notifications.get(pos++);
            return curr;
        }

        public void remove() {
            if (curr == null) {
                throw new IllegalStateException();
            }
            
            pos--;
            index.remove(pos);
            notifications.remove(pos);
            curr = null;
        }
    }
}
