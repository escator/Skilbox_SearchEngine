package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Setter @Getter
@Table(name = "page", indexes = @Index(name = "path_index", columnList  =  "path"))
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name  = "site_id", nullable = false)
    private Site site;

    @Column(columnDefinition = "VARCHAR(255)", name = "path", nullable = false)
    private String path;

    @Column(columnDefinition  =  "INTEGER", name = "title", nullable = false)
    private Integer code;

    @Column(columnDefinition = "MEDIUMTEXT", name = "content", nullable  = false)
    private String content;

}
