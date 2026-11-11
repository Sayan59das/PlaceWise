package com.placewise.storage;

import java.util.List;
import java.util.Optional;

/**
 * Generic CRUD contract. {@code T} is the entity and {@code ID} the type of its key, so one interface
 * serves students (key {@code String}), companies (key {@code Long}) and applications (key
 * {@code ApplicationKey}) with full compile-time type safety and no casts.
 *
 * @param <T>  entity type
 * @param <ID> key type
 */
public interface Repository<T, ID> {

    /**
     * Inserts a new entity.
     *
     * @param entity the entity to store
     * @return the stored entity (for generated keys this is a copy carrying the new id)
     */
    T save(T entity);

    /**
     * Looks an entity up by key.
     *
     * @param id the key
     * @return the entity, or empty when there is none (never {@code null})
     */
    Optional<T> findById(ID id);

    /**
     * Returns every entity in a stable order.
     *
     * @return all entities; empty list when none
     */
    List<T> findAll();

    /**
     * Overwrites an existing entity.
     *
     * @param entity the entity with its (unchanged) key and new values
     * @return {@code true} if it existed and was updated, {@code false} if there was nothing to update
     */
    boolean update(T entity);

    /**
     * Removes an entity.
     *
     * @param id the key
     * @return {@code true} if something was deleted
     */
    boolean delete(ID id);

    /**
     * Tells whether an entity exists.
     *
     * @param id the key
     * @return {@code true} if found
     */
    default boolean existsById(ID id) {
        return findById(id).isPresent();
    }

    /**
     * Counts the entities.
     *
     * @return the number stored
     */
    default long count() {
        return findAll().size();
    }
}
