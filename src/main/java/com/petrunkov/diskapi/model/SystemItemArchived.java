package com.petrunkov.diskapi.model;

import lombok.*;
import org.hibernate.Hibernate;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Objects;


@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table(name = "t_archive")
@Builder
public class SystemItemArchived {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    Long bdId;
    @NotNull
    private String id;
    private String url;
    @NotNull
    private Instant date;
    private String parentId;

    @Enumerated(EnumType.STRING)
    private SystemItemType type;
    private Long size;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        SystemItemArchived that = (SystemItemArchived) o;
        return bdId != null && Objects.equals(bdId, that.bdId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
