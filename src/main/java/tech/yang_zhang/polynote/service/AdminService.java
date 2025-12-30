package tech.yang_zhang.polynote.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.yang_zhang.polynote.dao.NotesDao;
import tech.yang_zhang.polynote.dao.ReplicationLogDao;
import tech.yang_zhang.polynote.dao.ReplicationSyncStateDao;

@Service
public class AdminService {

    private final NotesDao notesDao;
    private final ReplicationLogDao replicationLogDao;
    private final ReplicationSyncStateDao replicationSyncStateDao;
    private final LamportClockService lamportClockService;

    public AdminService(NotesDao notesDao,
                        ReplicationLogDao replicationLogDao,
                        ReplicationSyncStateDao replicationSyncStateDao,
                        LamportClockService lamportClockService) {
        this.notesDao = notesDao;
        this.replicationLogDao = replicationLogDao;
        this.replicationSyncStateDao = replicationSyncStateDao;
        this.lamportClockService = lamportClockService;
    }

    @Transactional
    public void resetAll() {
        notesDao.reset();
        replicationLogDao.reset();
        replicationSyncStateDao.reset();
        lamportClockService.reset();
    }
}
