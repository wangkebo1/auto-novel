package com.rag.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 角色表
 */
@Data
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name; // ROLE_ADMIN, ROLE_USER

    @Column(length = 200)
    private String description;
}
