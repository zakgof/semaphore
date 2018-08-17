package com.zakgof.semaphore.service;

import java.util.List;

import com.zakgof.db.velvet.island.DataWrap;
import com.zakgof.semaphore.model.Resource;

public interface ISemaphoreService {

    List<DataWrap<String, Resource>> loadAll();

    void requestLock(String resourceId, String user, long minutes);

    void releaseLock(String resourceId, String user);

    void addResource(Resource resource);

    void deleteResource(String resourceId);

}