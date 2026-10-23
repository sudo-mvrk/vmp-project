package com.volunnear.support;

import org.springframework.session.DelegatingIndexResolver;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.IndexResolver;
import org.springframework.session.MapSession;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.PrincipalNameIndexResolver;
import org.springframework.session.Session;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sessions that can be found by username, like the indexed Redis repository of production (Q23).
 * Spring Session has no indexed in-memory repository, so this one scans the map; fine for tests.
 * The real Redis behaviour is checked by {@code BanEndsRedisSessionsIntegrationTest}.
 */
public class IndexedMapSessionRepository implements FindByIndexNameSessionRepository<MapSession> {
    private final Map<String, Session> store = new ConcurrentHashMap<>();
    private final MapSessionRepository delegate = new MapSessionRepository(store);
    private final IndexResolver<Session> indexResolver = new DelegatingIndexResolver<>(new PrincipalNameIndexResolver<>());

    @Override
    public MapSession createSession() {
        return delegate.createSession();
    }

    @Override
    public void save(MapSession session) {
        delegate.save(session);
    }

    @Override
    public MapSession findById(String id) {
        return delegate.findById(id);
    }

    @Override
    public void deleteById(String id) {
        delegate.deleteById(id);
    }

    @Override
    public Map<String, MapSession> findByIndexNameAndIndexValue(String indexName, String indexValue) {
        Map<String, MapSession> found = new HashMap<>();
        for (String id : store.keySet()) {
            MapSession session = delegate.findById(id);
            if (session != null && Objects.equals(indexResolver.resolveIndexesFor(session).get(indexName), indexValue)) {
                found.put(id, session);
            }
        }
        return found;
    }
}
