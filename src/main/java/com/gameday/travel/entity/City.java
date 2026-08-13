package com.gameday.travel.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "cities")
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_ja", nullable = false, length = 50)
    private String nameJa;

    @Column(name = "is_unstable", nullable = false)
    private boolean unstable;

    protected City() {
    }

    public Long getId() {
        return id;
    }

    public String getNameJa() {
        return nameJa;
    }

    public boolean isUnstable() {
        return unstable;
    }
}
