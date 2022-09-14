package com.petrunkov.diskapi.repository;


import com.petrunkov.diskapi.model.SystemItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;


public interface StorageRepository extends JpaRepository<SystemItem, String> {
    @Query(nativeQuery = true, value =
    "WITH RECURSIVE items AS(" +
            "SELECT id, url, date, parent_id, type, size " +
            "FROM t_system_items " +
            "WHERE id = ?1 " +
            "UNION ALL " +
            "SELECT e.id, " +
                "e.url, " +
                "e.date, " +
                "e.parent_id," +
                "e.type," +
                "e.size " +
            "FROM t_system_items e, items " +
            "WHERE items.id = e.parent_id) " +
            "SELECT * FROM items; ")
    Set<SystemItem> getItemInfo(String id);
    @Query(nativeQuery = true, value =
            "WITH RECURSIVE items AS(" +
                "SELECT id, url, date, parent_id, type, size " +
                "FROM t_system_items " +
                "WHERE id = ?1 " +
                "UNION ALL " +
                "SELECT e.id, " +
                    "e.url, " +
                    "e.date, " +
                    "e.parent_id," +
                    "e.type," +
                    "e.size " +
                "FROM t_system_items e, items " +
                "WHERE items.id = e.parent_id) " +
                "DELETE FROM t_system_items " +
                    "WHERE id IN (SELECT id FROM items); ")
    @Modifying
    @Transactional(isolation = Isolation.SERIALIZABLE)
    void deleteFolder(String id);
    @Query(nativeQuery = true, value =
            "SELECT * FROM t_system_items " +
            "WHERE type = 'FILE' AND date BETWEEN ?1 AND ?2 ; ")

    Set<SystemItem> getFileHistory(Instant start, Instant finish);



    @Query(nativeQuery = true, value =
            "WITH RECURSIVE items AS(" +
                    "SELECT id, url, date, parent_id, type, size " +
                    "FROM t_system_items " +
                    "WHERE id = ?1 " +
                    "UNION ALL " +
                    "SELECT e.id, " +
                    "e.url, " +
                    "e.date, " +
                    "e.parent_id," +
                    "e.type," +
                    "e.size " +
                    "FROM t_system_items e, items " +
                    "WHERE items.parent_id = e.id) " +
                    "SELECT * FROM items WHERE id <> ?1 ; ")
    Set<SystemItem> getPredecessors(String id);

}
