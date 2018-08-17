package com.zakgof.semaphore.model;

import java.time.ZoneId;
import java.time.ZoneOffset;

import com.zakgof.db.velvet.entity.Entities;
import com.zakgof.db.velvet.entity.IEntityDef;
import com.zakgof.db.velvet.entity.IKeylessEntityDef;
import com.zakgof.db.velvet.impl.link.BiSecIndexMultiLinkDef;
import com.zakgof.db.velvet.island.IslandModel;
import com.zakgof.db.velvet.link.ISingleLinkDef;
import com.zakgof.db.velvet.link.Links;
import com.zakgof.db.velvet.query.SecQueries;

public class Defs {

    public static ZoneId TIMEZONE = ZoneId.of("Europe/Kiev");

    public static IEntityDef<String, Resource> RESOURCE = Entities.create(Resource.class);
    public static IEntityDef<Long, User> USER = Entities.create(User.class);
    public static IKeylessEntityDef<Lock> LOCK = Entities.keyless(Lock.class);

    public static ISingleLinkDef<String, Resource, Long, Lock> RESOURCE_BUSY = Links.single(RESOURCE, LOCK);
    public static BiSecIndexMultiLinkDef<String, Resource, Long, Lock, Long> RESOURCE_HISTORY = Links.biSec(RESOURCE, LOCK, Long.class, lock -> lock.getStart().toInstant(ZoneOffset.UTC).toEpochMilli());
    public static BiSecIndexMultiLinkDef<Long, User, Long, Lock, Long> USER_LOCK = Links.biSec(USER, LOCK, Long.class, lock -> lock.getStart().toInstant(ZoneOffset.UTC).toEpochMilli());


    public static IslandModel<String, Resource> RESOURCES = IslandModel.mainEntity(RESOURCE).include(RESOURCE_BUSY).done()
        .entity(LOCK).include(USER_LOCK.back()).done()
        .entity(USER).done()
        .build();

    public static IslandModel<String, Resource> RESOURCES_WITH_HISTORY = IslandModel.mainEntity(RESOURCE)
            .include(RESOURCE_BUSY)
            .include("history", RESOURCE_HISTORY.indexed(SecQueries.last(10)))
        .done()
        .entity(LOCK).include(USER_LOCK.back()).done()
        .entity(USER).done()
        .build();

    public static IslandModel<String, Resource> DELETE_RESOURCES = IslandModel.mainEntity(RESOURCE)
            .include(RESOURCE_BUSY)
            .include(RESOURCE_HISTORY)
        .done()
        .entity(LOCK).detach(USER_LOCK.back()).done()
        .build();

}
