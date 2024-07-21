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

    @ManyToOne
    @JoinColumn(name="page_id")
    private Page page;

    @ManyToOne
    @JoinColumn(name="lemma_id")
    private Lemma lemma;

    @Column(name="rating", nullable=false)
    private Double rank;

}
