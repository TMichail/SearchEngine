package main.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;
import java.io.Serializable;

@EqualsAndHashCode
@ToString
@Embeddable
@Getter
@Setter
public class KeySearchIndexEntity implements Serializable {

    @ManyToOne()
    private LemmaEntity lemma;

    @ManyToOne()
    private PageEntity page;
}
