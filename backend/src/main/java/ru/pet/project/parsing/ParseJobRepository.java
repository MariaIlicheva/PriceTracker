package ru.pet.project.parsing;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ParseJobRepository extends JpaRepository<ParseJob, Long> {
    // Выборку задач из очереди (SELECT ... FOR UPDATE SKIP LOCKED) добавим на шаге 8.
}