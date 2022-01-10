package main.data;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "lemmas")
public class LemmaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id")
    private Collection<SearchIndexEntity> lemmas;

    @Column(nullable = false)
    private String lemma;

    @Column(nullable = false)
    private int frequency;

    @ManyToMany(mappedBy = "lemmas")
    private Set<SiteEntity> sites;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LemmaEntity lemma1 = (LemmaEntity) o;
        return lemma.equals(lemma1.lemma);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lemma);
    }
}
