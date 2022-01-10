package main.data;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "search_indexes")
public class SearchIndexEntity{

    @EmbeddedId
    private KeySearchIndexEntity keySearchIndexEntity;

    @Column(nullable = false)
    private float ranks;
}
