package ru.pet.project.parsing;

// Задача, которую обработчик взял из очереди
// attempt — номер этой попытки, с единицы
public record ClaimedJob(long jobId, long linkId, int attempt, ParseTarget target) {
}