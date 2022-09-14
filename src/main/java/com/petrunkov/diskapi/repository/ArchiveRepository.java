package com.petrunkov.diskapi.repository;

import com.petrunkov.diskapi.model.SystemItemArchived;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;

public interface ArchiveRepository extends JpaRepository<SystemItemArchived, Long> {


    @Query(nativeQuery = true, value =
    "DELETE FROM t_archive WHERE id = ?1 ;")
    @Modifying
    @Transactional(isolation = Isolation.SERIALIZABLE)
    void deleteFileById(String id);

    @Query(nativeQuery = true, value = "WITH RECURSIVE items AS(" +
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
            "DELETE FROM t_archive o WHERE o.id IN (SELECT i.id FROM items i); ")
    @Modifying
    @Transactional(isolation = Isolation.SERIALIZABLE)
    void deleteFolderById(String id);

    @Query(nativeQuery = true, value =
    "SELECT * FROM t_archive WHERE id = ?1 AND date BETWEEN ?2 AND ?3 ;")
    Collection<SystemItemArchived> getHistoryInterval(String id, Instant dateStart, Instant dateEnd);

}
