package com.zakgof.semaphore.service;

import java.time.LocalDateTime;
import java.util.List;

import com.zakgof.db.velvet.IVelvetEnvironment;
import com.zakgof.db.velvet.island.DataWrap;
import com.zakgof.semaphore.BusinessException;
import com.zakgof.semaphore.model.Defs;
import com.zakgof.semaphore.model.Lock;
import com.zakgof.semaphore.model.Resource;
import com.zakgof.semaphore.model.User;
import com.zakgof.telegram.IBotService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SemaphoreServiceImpl implements ISemaphoreService {

    private final IVelvetEnvironment velvetEnvironment;
    private final IAuthService authService;
    private final IBotService botService;

    @Override
    public List<DataWrap<String, Resource>> loadAll() {
        return velvetEnvironment.calculate(velvet -> Defs.RESOURCES_WITH_HISTORY.batchGetAll(velvet));
    }

    @Override
    public void requestLock(String jwt, String resourceId, long minutes) {
        velvetEnvironment.execute(velvet -> {
            DataWrap<String, Resource> resourceWrap = Defs.RESOURCES.get(velvet, resourceId);
            if (resourceWrap == null)
                throw new BusinessException("Resource not found");
            else if (resourceWrap.singleLink(Defs.RESOURCE_BUSY) != null)
                throw new BusinessException("Resource busy");

            User user =  authService.parseUser(jwt);

            Lock lock = new Lock(now(), now().plusMinutes(minutes));
            Defs.USER.put(velvet, user);
            Defs.LOCK.put(velvet, lock);
            Defs.USER_LOCK.connect(velvet, user, lock);
            Defs.RESOURCE_BUSY.connect(velvet, resourceWrap.getNode(), lock);

            botService.broadcast(user.getDisplayName() + " occupied " + resourceId + " for " + minutes + " min");
        });
    }

    @Override
    public void releaseLock(String gwt, String resourceId) {
        velvetEnvironment.execute(velvet -> {
            DataWrap<String, Resource> resourceWrap = Defs.RESOURCES.get(velvet, resourceId);
            if (resourceWrap == null)
                throw new BusinessException("Resource " + resourceId + " not found");

            DataWrap<Long, Lock> lockWrap = resourceWrap.singleLink(Defs.RESOURCE_BUSY);
            if (lockWrap == null)
                throw new BusinessException("Resource is not locked");

            Lock lock = lockWrap.getNode();
            User lockuser = lockWrap.singleLink(Defs.USER_LOCK.back()).getNode();
            User user = authService.parseUser(gwt);

            if (lockuser.getTelegramId().longValue() != user.getTelegramId().longValue()) {
                throw new BusinessException("Resource is locked by another user: " + lockuser + " sender : " + user);
            }
            lock = lock.setRelease(now());
            Defs.LOCK.put(velvet, lock);
            Defs.RESOURCE_BUSY.disconnect(velvet, resourceWrap.getNode(), lock);
            Defs.RESOURCE_HISTORY.connect(velvet, resourceWrap.getNode(), lock);

            botService.broadcast(user.getDisplayName() + " released " + resourceId);
        });
    }

    private LocalDateTime now() {
        return LocalDateTime.now(Defs.TIMEZONE);
    }

    @Override
    public void addResource(Resource resource) {
        velvetEnvironment.execute(velvet -> {
            if (Defs.RESOURCE.get(velvet, resource.getId()) != null) {
                throw new BusinessException("Resource with the same id already exists");
            }
            Defs.RESOURCE.put(velvet, resource);
        });
    }

    @Override
    public void deleteResource(String resourceId) {
        velvetEnvironment.execute(velvet -> {
            Defs.DELETE_RESOURCES.deleteKey(velvet, resourceId);
        });
    }

}
