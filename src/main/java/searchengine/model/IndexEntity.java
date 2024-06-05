package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name="index_t")
@Setter
@Getter
public class IndexEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="page_id", referencedColumnName = "id")
    private Page page;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="lemma_id", referencedColumnName =  "id")
    private Lemma lemma;

    @Column(name="rating", nullable=false)
    private Float rating;

}
