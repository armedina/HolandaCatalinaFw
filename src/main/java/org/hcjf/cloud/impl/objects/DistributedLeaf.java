package org.hcjf.cloud.impl.objects;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * @author javaito
 */
public abstract class DistributedLeaf implements DistributedObject {

    private final Object key;
    private Long lastUpdate;

    public DistributedLeaf(Object key) {
        this.key = key;
    }

    @Override
    public Object getKey() {
        return key;
    }

    public final Long getLastUpdate() {
        return lastUpdate;
    }

    public final void setLastUpdate(Long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}
