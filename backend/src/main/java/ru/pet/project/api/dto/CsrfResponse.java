package ru.pet.project.api.dto;

public record CsrfResponse(String headerName, String token) {
}