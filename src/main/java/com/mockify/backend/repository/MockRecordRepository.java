package com.mockify.backend.repository;

import com.mockify.backend.model.MockRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MockRecordRepository extends JpaRepository<MockRecord, Long> {
}