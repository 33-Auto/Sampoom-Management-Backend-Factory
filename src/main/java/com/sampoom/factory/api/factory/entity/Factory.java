package com.sampoom.factory.api.factory.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "factory")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Factory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "factory_id")
    private Long id;

    @Column(name = "factory_name")
    private String name;

    private String location;

}
