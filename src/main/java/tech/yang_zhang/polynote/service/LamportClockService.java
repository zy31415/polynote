package tech.yang_zhang.polynote.service;

import org.springframework.stereotype.Service;
import tech.yang_zhang.polynote.dao.ReplicationLogDao;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class LamportClockService {
    private final AtomicLong logicalTime = new AtomicLong(0);

    public LamportClockService(ReplicationLogDao replicationLogDao) {
        this.logicalTime.set(replicationLogDao.findMaxTimestamp());
    }

    public long getTime() {
        // Using updateAndGet to ensure atomic read-modify-write
        return logicalTime.updateAndGet(value -> value + 1);
    }

    public void setTime(long externalTime) {
        logicalTime.updateAndGet(value -> Math.max(value, externalTime));
    }
}
