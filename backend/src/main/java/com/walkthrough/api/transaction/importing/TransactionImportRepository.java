package com.walkthrough.api.transaction.importing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionImportRepository extends JpaRepository<TransactionImport, Long> {

    /** Derived query with a limit: "find the first 20, ordered by startedAt descending". */
    List<TransactionImport> findTop20ByOrderByStartedAtDesc();
}
