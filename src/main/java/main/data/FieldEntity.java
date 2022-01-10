package main.data;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "fields")
public class FieldEntity {
    @Id
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String selector;

    @Column(nullable = false)
    private float weight;
}
