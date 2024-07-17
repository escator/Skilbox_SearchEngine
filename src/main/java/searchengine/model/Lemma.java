package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Set;

@Entity
@Table(name="lemma")
@Getter
@Setter
@NoArgsConstructor
public class Lemma {

    public Lemma(String lemma) {
        this.lemma = lemma;
    }
    public Lemma(String lemma, Site site) {
        this.lemma = lemma;
        this.site = site;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name="site_id")
    private Site site;

    @Column(name="lemma")
    private String lemma;

    @Column(name="frequency")
    private Integer frequency;

    public void incrementFrequency() {
        if (this.frequency == null) {
            this.frequency = 1;
        } else {
            this.frequency += 1;
        }
    }

    public void decrementFrequency()  {
        if (this.frequency == null) {
            this.frequency = 0;
        } else {
            this.frequency -= 1;
        }
    }
}
