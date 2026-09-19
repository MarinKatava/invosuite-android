package com.invoisuite.app.model;

public record User(Long id, String email, String fullName, String authProvider) {
}
