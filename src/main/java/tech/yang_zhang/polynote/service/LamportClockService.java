package tech.yang_zhang.polynote.service;

import org.springframework.stereotype.Service;

import tech.yang_zhang.polynote.dao.ReplicationLogDao;

@Service
public class LamportClockService {
    private long counter;

    public LamportClockService(ReplicationLogDao replicationLogDao) {
        this.counter = replicationLogDao.findMaxTimestamp();
    }

    public synchronized long getCurrentTime() {
        return counter;
    }

    public synchronized long tick() {
        counter += 1;
        return counter;
    }

    public synchronized long update(long receivedTime) {
        counter = Math.max(counter, receivedTime) + 1;
        return counter;
    }
}
