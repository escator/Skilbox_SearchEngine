package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "site")
@Setter @Getter
@ToString
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')",  name = "status")
    private IndexingStatus status;

    @Column(columnDefinition = "DATETIME", name = "status_time")
    private LocalDateTime statusTime;

    @Column(columnDefinition = "TEXT", name = "last_error")
    private String lastError;

    @Column(columnDefinition = "VARCHAR(255)", name = "url", unique = true)
    private String url;

    @Column(columnDefinition = "VARCHAR(255)", name = "name")
    private String name;

    @OneToMany(mappedBy  =  "site", cascade =  CascadeType.ALL)
    private List<Page> pages;
}
