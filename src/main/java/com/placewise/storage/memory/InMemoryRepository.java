package com.placewise.storage.memory;

import com.placewise.exception.PlaceWiseException;
import com.placewise.storage.Repository;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Map-backed repository used when the program runs without a database ({@code --no-db}) and in tests.
 * It implements the same {@link Repository} interface as the JDBC classes, so the rest of the program
 * cannot tell the difference (polymorphism). All methods are {@code synchronized}: the backing maps are
 * not thread-safe on their own.
 *
 * @param <T>  entity type
 * @param <ID> key type
 */
public abstract class InMemoryRepository<T, ID> implements Repository<T, ID> {

    private final Map<ID, T> store;

    /**
     * Creates the repository.
     *
     * @param backingMap an empty map; its type decides the iteration order ({@code TreeMap} = sorted,
     *                   {@code LinkedHashMap} = insertion order)
     */
    protected InMemoryRepository(Map<ID, T> backingMap) {
        this.store = backingMap;
    }

    /**
     * Extracts the key from an entity.
     *
     * @param entity the entity
     * @return its key
     */
    protected abstract ID idOf(T entity);

    /**
     * Builds the exception for a key that is already taken.
     *
     * @param entity the entity that could not be saved
     * @return the exception to throw
     */
    protected abstract PlaceWiseException duplicate(T entity);

    /**
     * Gives subclasses access to the backing map; callers must hold the repository lock.
     *
     * @return the map
     */
    protected final Map<ID, T> store() {
        return store;
    }

    @Override
    public synchronized T save(T entity) {
        ID id = idOf(entity);
        if (store.containsKey(id)) {
            throw duplicate(entity);
        }
        store.put(id, entity);
        return entity;
    }

    @Override
    public synchronized Optional<T> findById(ID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public synchronized List<T> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public synchronized boolean update(T entity) {
        ID id = idOf(entity);
        if (!store.containsKey(id)) {
            return false;
        }
        store.put(id, entity);
        return true;
    }

    @Override
    public synchronized boolean delete(ID id) {
        return store.remove(id) != null;
    }
}
