package com.example.docker.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.docker.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
