package main.data;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "sites")
@Getter
@Setter
public class SiteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @OneToMany(mappedBy = "site",fetch = FetchType.EAGER)
    private Collection<PageEntity> pages;

    @ManyToMany()
    @JoinTable(name = "sites_lemmas",
            joinColumns = {@JoinColumn(name = "sites_id")},
            inverseJoinColumns = {@JoinColumn(name = "lemmas_id")})
    private Set<LemmaEntity> lemmas;

    @Column(nullable = false)
    @Enumerated (EnumType.STRING)
    private SiteIndexingStatus status;

    @Column(nullable = false)
    private Date status_time;

    @Column(columnDefinition = "TEXT")
    private String last_error;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SiteEntity site = (SiteEntity) o;
        return url.equals(site.url) && name.equals(site.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, name);
    }
}
