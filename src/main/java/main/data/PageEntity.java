package main.data;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import javax.persistence.Index;
import java.util.Collection;

@Entity
@Table(name = "pages", indexes = {@Index(name = "page_path", columnList = "path")})
@Getter
@Setter
public class PageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id")
    private Collection<SearchIndexEntity> searchIndexes;

    private String path;

    @Column(nullable = false)
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    @ManyToOne()
    private SiteEntity site;
}
