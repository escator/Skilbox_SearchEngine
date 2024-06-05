package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "site")
@Setter @Getter
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')", nullable = false, name = "status")
    private IndexingStatus status;

    @Column(columnDefinition = "DATETIME", nullable = false, name = "status_time")
    private LocalDateTime statusTime;

    @Column(columnDefinition = "TEXT", name = "last_error")
    private String lastError;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false, name = "url")
    private String url;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false, name = "name")
    private String name;

    @OneToMany(mappedBy  =  "site", cascade =  CascadeType.ALL)
    private List<Page> pages;
}
